package git.commands;

import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Blob;
import git.objects.Commit;
import git.objects.Tree;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MergeCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: mgit merge <branch>");
            return;
        }

        String targetBranch = args[1];
        Path targetBranchPath = repo.getGitDir().resolve("refs/heads/" + targetBranch);
        if (!Files.exists(targetBranchPath)) {
            System.out.println("Branch not found: " + targetBranch);
            return;
        }

        String currentSha = repo.resolveRef("HEAD");
        String targetSha = Files.readString(targetBranchPath).strip();

        if (currentSha.equals(targetSha)) {
            System.out.println("Already up to date.");
            return;
        }

        String baseSha = repo.findMergeBase(currentSha, targetSha);

        // fast-forward case: current branch hasn't diverged, just move the pointer
        if (baseSha.equals(currentSha)) {
            performFastForward(repo, targetBranch, targetSha);
            return;
        }
        if (baseSha.equals(targetSha)) {
            System.out.println("Already up to date.");
            return;
        }

        // real 3-way merge needed
        Map<String, String> baseFiles = treeFiles(repo, baseSha);
        Map<String, String> currentFiles = treeFiles(repo, currentSha);
        Map<String, String> targetFiles = treeFiles(repo, targetSha);

        Set<String> allFiles = new TreeSet<>();
        allFiles.addAll(baseFiles.keySet());
        allFiles.addAll(currentFiles.keySet());
        allFiles.addAll(targetFiles.keySet());

        List<Tree.Entry> mergedEntries = new ArrayList<>();
        boolean hasConflict = false;

        for (String file : allFiles) {
            String baseSha_ = baseFiles.get(file);
            String currentSha_ = currentFiles.get(file);
            String targetSha_ = targetFiles.get(file);

            String resultSha;

            if (Objects.equals(currentSha_, targetSha_)) {
                // both sides identical (including both null/deleted) - no conflict
                resultSha = currentSha_;
            } else if (Objects.equals(baseSha_, currentSha_)) {
                // only target changed it - take target's version
                resultSha = targetSha_;
            } else if (Objects.equals(baseSha_, targetSha_)) {
                // only current changed it - keep current's version
                resultSha = currentSha_;
            } else {
                // both changed it differently - CONFLICT
                hasConflict = true;
                resultSha = writeConflictBlob(repo, file, currentSha_, targetSha_);
                System.out.println("CONFLICT (content): Merge conflict in " + file);
            }

            if (resultSha != null) {
                mergedEntries.add(new Tree.Entry("100644", file, resultSha));
            }
        }

        // write merged files to working directory
        for (Tree.Entry entry : mergedEntries) {
            byte[] blobData = repo.readObject(entry.sha);
            byte[] content = stripBlobHeader(blobData);
            Path filePath = repo.getRoot().resolve(entry.name);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content);
        }

        // update index to match merged state
        Index index = Index.load(repo.getGitDir());
        index.getEntries().clear();
        for (Tree.Entry entry : mergedEntries) {
            Path filePath = repo.getRoot().resolve(entry.name);
            index.put(entry.name, new Index.Entry(
                entry.sha, Files.size(filePath), Files.getLastModifiedTime(filePath).toMillis()
            ));
        }
        index.save();

        if (hasConflict) {
            System.out.println("\nAutomatic merge failed; fix conflicts and then commit the result.");
            return;
        }

        // no conflicts - create the merge commit automatically with two parents...
        // NOTE: our Commit object only supports ONE parent. For a real merge commit
        // we'd need a second parent field. For now, we commit with current HEAD as parent
        // and document this as a known simplification.
        Tree mergedTree = new Tree(mergedEntries);
        repo.writeObject(mergedTree);

        Commit mergeCommit = new Commit(mergedTree.getSha(), currentSha, "Nakul",
                "Merge branch '" + targetBranch + "'");
        repo.writeObject(mergeCommit);

        String headRefPath = repo.readHeadRefPath();
        repo.updateRef(headRefPath, mergeCommit.getSha());

        System.out.println("Merge made by the 'recursive' strategy.");
    }

    private void performFastForward(Repository repo, String targetBranch, String targetSha) throws Exception {
        Commit commit = Commit.deserialize(repo.readObject(targetSha));
        Tree tree = Tree.deserialize(repo.readObject(commit.getTreeSha()));
        repo.checkoutTree(tree, repo.getRoot());

        Index index = Index.load(repo.getGitDir());
        index.getEntries().clear();
        for (Tree.Entry e : tree.getEntries()) {
            Path filePath = repo.getRoot().resolve(e.name);
            index.put(e.name, new Index.Entry(
                e.sha, Files.size(filePath), Files.getLastModifiedTime(filePath).toMillis()
            ));
        }
        index.save();

        String headRefPath = repo.readHeadRefPath();
        repo.updateRef(headRefPath, targetSha);

        System.out.println("Fast-forward merge to " + targetSha.substring(0, 8));
    }

    private Map<String, String> treeFiles(Repository repo, String commitSha) throws Exception {
        Map<String, String> files = new HashMap<>();
        Commit commit = Commit.deserialize(repo.readObject(commitSha));
        Tree tree = Tree.deserialize(repo.readObject(commit.getTreeSha()));
        for (Tree.Entry e : tree.getEntries()) {
            files.put(e.name, e.sha);
        }
        return files;
    }

    private String writeConflictBlob(Repository repo, String filename,
                                       String currentSha, String targetSha) throws Exception {
        String currentContent = currentSha != null
                ? new String(stripBlobHeader(repo.readObject(currentSha)), StandardCharsets.UTF_8)
                : "";
        String targetContent = targetSha != null
                ? new String(stripBlobHeader(repo.readObject(targetSha)), StandardCharsets.UTF_8)
                : "";

        String conflictContent =
                "<<<<<<< HEAD\n" + currentContent +
                "=======\n" + targetContent +
                ">>>>>>> incoming\n";

        Blob conflictBlob = new Blob(conflictContent.getBytes(StandardCharsets.UTF_8));
        repo.writeObject(conflictBlob);
        return conflictBlob.getSha();
    }

    private byte[] stripBlobHeader(byte[] data) {
        int nullIndex = 0;
        while (data[nullIndex] != 0) nullIndex++;
        byte[] content = new byte[data.length - nullIndex - 1];
        System.arraycopy(data, nullIndex + 1, content, 0, content.length);
        return content;
    }
}
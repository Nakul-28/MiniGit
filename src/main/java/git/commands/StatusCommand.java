package git.commands;

import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Commit;
import git.objects.Tree;
import git.util.HashUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StatusCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        Index index = Index.load(repo.getGitDir());
        Path root = repo.getRoot();

        // get the tree from HEAD's commit, if any
        Map<String, String> headFiles = new HashMap<>(); // path -> blob sha
        String headSha = repo.resolveRef("HEAD");
        if (headSha != null) {
            Commit headCommit = Commit.deserialize(repo.readObject(headSha));
            Tree tree = Tree.deserialize(repo.readObject(headCommit.getTreeSha()));
            for (Tree.Entry e : tree.getEntries()) {
                headFiles.put(e.name, e.sha);
            }
        }

        Set<String> staged = new TreeSet<>();      // staged for commit (index differs from HEAD)
        Set<String> modified = new TreeSet<>();    // working dir differs from index
        Set<String> untracked = new TreeSet<>();   // in working dir, not in index
        Set<String> deleted = new TreeSet<>();     // tracked in index, missing from disk

        // compare index vs HEAD -> staged changes
        for (var entry : index.getEntries().entrySet()) {
            String path = entry.getKey();
            String indexSha = entry.getValue().sha;
            String headBlobSha = headFiles.get(path);
            if (headBlobSha == null || !headBlobSha.equals(indexSha)) {
                staged.add(path);
            }
        }

        // check for tracked files that no longer exist on disk
        for (var entry : index.getEntries().entrySet()) {
            Path filePath = root.resolve(entry.getKey());
            if (!Files.exists(filePath)) {
                deleted.add(entry.getKey());
            }
        }

        // compare working directory vs index -> modified / untracked
        for (Path file : listTrackableFiles(root)) {
            String relPath = root.relativize(file).toString().replace("\\", "/");
            byte[] content = Files.readAllBytes(file);
            String currentSha = HashUtil.sha256Hex(blobBytes(content));

            Index.Entry indexEntry = index.get(relPath);
            if (indexEntry == null) {
                untracked.add(relPath);
            } else if (!indexEntry.sha.equals(currentSha)) {
                modified.add(relPath);
            }
        }

        if (staged.isEmpty() && modified.isEmpty() && untracked.isEmpty() && deleted.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean.");
            return;
        }

        if (!staged.isEmpty()) {
            System.out.println("Changes to be committed:");
            for (String p : staged) System.out.println("    staged: " + p);
            System.out.println();
        }
        if (!modified.isEmpty()) {
            System.out.println("Changes not staged for commit:");
            for (String p : modified) System.out.println("    modified: " + p);
            System.out.println();
        }
        if (!deleted.isEmpty()) {
            System.out.println("Deleted files:");
            for (String p : deleted) System.out.println("    deleted: " + p);
            System.out.println();
        }
        if (!untracked.isEmpty()) {
            System.out.println("Untracked files:");
            for (String p : untracked) System.out.println("    " + p);
        }
    }

    // recreate the same "blob <size>\0<content>" bytes that Blob.serialize() would produce,
    // so the hash matches exactly what writeObject() would compute
    private byte[] blobBytes(byte[] content) {
        byte[] header = ("blob " + content.length + "\0").getBytes();
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }

    private List<Path> listTrackableFiles(Path root) throws Exception {
        List<Path> files = new ArrayList<>();
        Files.walk(root)
             .filter(Files::isRegularFile)
             .filter(p -> !p.toString().contains(".git"))
             .forEach(files::add);
        return files;
    }
}
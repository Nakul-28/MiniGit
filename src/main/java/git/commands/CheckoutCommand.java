package git.commands;

import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Commit;
import git.objects.Tree;

import java.nio.file.Files;
import java.nio.file.Path;

public class CheckoutCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: mgit checkout <branch>");
            return;
        }

        String branchName = args[1];
        Path branchPath = repo.getGitDir().resolve("refs/heads/" + branchName);

        if (!Files.exists(branchPath)) {
            System.out.println("Branch not found: " + branchName);
            return;
        }

        String targetSha = Files.readString(branchPath).strip();
        Commit commit = Commit.deserialize(repo.readObject(targetSha));
        Tree tree = Tree.deserialize(repo.readObject(commit.getTreeSha()));

        // safety check now also considers untracked files that collide with the target
        // tree
        if (hasUncommittedChanges(repo, tree)) {
            System.out.println("error: your local changes would be overwritten by checkout.");
            System.out.println("Please commit your changes or discard them before switching branches.");
            return;
        }

        repo.checkoutTree(tree, repo.getRoot());

        Index index = Index.load(repo.getGitDir());
        index.getEntries().clear();
        for (Tree.Entry e : tree.getEntries()) {
            Path filePath = repo.getRoot().resolve(e.name);
            index.put(e.name, new Index.Entry(
                    e.sha,
                    Files.size(filePath),
                    Files.getLastModifiedTime(filePath).toMillis()));
        }
        index.save();

        Files.writeString(repo.getGitDir().resolve("HEAD"), "ref: refs/heads/" + branchName + "\n");

        System.out.println("Switched to branch '" + branchName + "'");
    }

    private boolean hasUncommittedChanges(Repository repo, Tree targetTree) throws Exception {
        Index index = Index.load(repo.getGitDir());

        // 1. check tracked files for modifications vs index
        for (var entry : index.getEntries().entrySet()) {
            Path filePath = repo.getRoot().resolve(entry.getKey());
            if (!Files.exists(filePath)) {
                return true;
            }
            byte[] content = Files.readAllBytes(filePath);
            byte[] header = ("blob " + content.length + "\0").getBytes();
            byte[] blobBytes = new byte[header.length + content.length];
            System.arraycopy(header, 0, blobBytes, 0, header.length);
            System.arraycopy(content, 0, blobBytes, header.length, content.length);

            String currentSha = git.util.HashUtil.sha256Hex(blobBytes);
            if (!currentSha.equals(entry.getValue().sha)) {
                return true;
            }
        }

        // 2. check untracked files that would be silently overwritten by the target
        // branch
        for (Tree.Entry e : targetTree.getEntries()) {
            Path filePath = repo.getRoot().resolve(e.name);
            boolean isTracked = index.get(e.name) != null;
            if (!isTracked && Files.exists(filePath)) {
                return true;
            }
        }

        return false;
    }
}
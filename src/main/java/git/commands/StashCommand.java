package git.commands;

import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Blob;
import git.objects.Commit;
import git.objects.Tree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StashCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        String sub = args.length > 1 ? args[1] : "save";

        if (sub.equals("pop")) {
            pop(repo);
        } else {
            save(repo);
        }
    }

    private void save(Repository repo) throws Exception {
        Index index = Index.load(repo.getGitDir());
        if (index.getEntries().isEmpty()) {
            System.out.println("No staged changes to stash.");
            return;
        }

        // build a tree representing the current index state (the "stash")
        List<Tree.Entry> entries = new ArrayList<>();
        for (var e : index.getEntries().entrySet()) {
            entries.add(new Tree.Entry("100644", e.getKey(), e.getValue().sha));
        }
        Tree stashTree = new Tree(entries);
        repo.writeObject(stashTree);

        String headSha = repo.resolveRef("HEAD");
        Commit stashCommit = new Commit(stashTree.getSha(), headSha, "Nakul", "WIP stash");
        repo.writeObject(stashCommit);

        repo.updateRef("refs/stash", stashCommit.getSha());

        // reset working directory back to HEAD's tree (discard the stashed changes)
        if (headSha != null) {
            Commit headCommit = Commit.deserialize(repo.readObject(headSha));
            Tree headTree = Tree.deserialize(repo.readObject(headCommit.getTreeSha()));
            repo.checkoutTree(headTree, repo.getRoot());

            index.getEntries().clear();
            for (Tree.Entry e : headTree.getEntries()) {
                Path filePath = repo.getRoot().resolve(e.name);
                index.put(e.name, new Index.Entry(
                    e.sha, Files.size(filePath), Files.getLastModifiedTime(filePath).toMillis()
                ));
            }
            index.save();
        }

        System.out.println("Saved working directory state: " + stashCommit.getSha().substring(0, 8));
    }

    private void pop(Repository repo) throws Exception {
        String stashSha = repo.resolveRef("refs/stash");
        if (stashSha == null) {
            System.out.println("No stash found.");
            return;
        }

        Commit stashCommit = Commit.deserialize(repo.readObject(stashSha));
        Tree stashTree = Tree.deserialize(repo.readObject(stashCommit.getTreeSha()));

        repo.checkoutTree(stashTree, repo.getRoot());

        Index index = Index.load(repo.getGitDir());
        for (Tree.Entry e : stashTree.getEntries()) {
            Path filePath = repo.getRoot().resolve(e.name);
            index.put(e.name, new Index.Entry(
                e.sha, Files.size(filePath), Files.getLastModifiedTime(filePath).toMillis()
            ));
        }
        index.save();

        // remove the stash ref after popping
        Files.deleteIfExists(repo.getGitDir().resolve("refs/stash"));

        System.out.println("Restored stashed changes.");
    }
}
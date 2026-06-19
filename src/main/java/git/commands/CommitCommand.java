package git.commands;

import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Commit;
import git.objects.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommitCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: mgit commit <message>");
            return;
        }
        String message = args[1];

        Index index = Index.load(repo.getGitDir());
        if (index.getEntries().isEmpty()) {
            System.out.println("Nothing to commit. Use 'mgit add <file>' first.");
            return;
        }

        // build a flat tree from the index (no subdirectories yet)
        List<Tree.Entry> entries = new ArrayList<>();
        for (Map.Entry<String, Index.Entry> e : index.getEntries().entrySet()) {
            entries.add(new Tree.Entry("100644", e.getKey(), e.getValue().sha));
        }
        Tree tree = new Tree(entries);
        repo.writeObject(tree);

        // find current HEAD commit, if any, to use as parent
        String parentSha = repo.resolveRef("HEAD");

        Commit commit = new Commit(tree.getSha(), parentSha, "Nakul", message);
        repo.writeObject(commit);

        // move the current branch to point at the new commit
        String headContent = repo.readHeadRefPath(); // e.g. "refs/heads/main"
        repo.updateRef(headContent, commit.getSha());

        System.out.println("[" + headContent.replace("refs/heads/", "") + " "
                + commit.getSha().substring(0, 8) + "] " + message);
    }
}
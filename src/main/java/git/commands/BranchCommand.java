package git.commands;

import git.Main;
import git.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class BranchCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        Path headsDir = repo.getGitDir().resolve("refs/heads");

        if (args.length < 2) {
            // list all branches, marking the current one
            String currentBranch = repo.readHeadRefPath().replace("refs/heads/", "");
            try (Stream<Path> files = Files.list(headsDir)) {
                files.sorted().forEach(p -> {
                    String name = p.getFileName().toString();
                    System.out.println((name.equals(currentBranch) ? "* " : "  ") + name);
                });
            }
            return;
        }

        String newBranch = args[1];
        String headSha = repo.resolveRef("HEAD");
        if (headSha == null) {
            System.out.println("Cannot create branch: no commits yet.");
            return;
        }

        Path newBranchPath = headsDir.resolve(newBranch);
        if (Files.exists(newBranchPath)) {
            System.out.println("Branch already exists: " + newBranch);
            return;
        }

        repo.updateRef("refs/heads/" + newBranch, headSha);
        System.out.println("Created branch " + newBranch);
    }
}
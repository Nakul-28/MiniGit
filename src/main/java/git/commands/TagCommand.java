package git.commands;

import git.Main;
import git.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TagCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        Path tagsDir = repo.getGitDir().resolve("refs/tags");
        Files.createDirectories(tagsDir);

        if (args.length < 2) {
            // list all tags
            try (Stream<Path> files = Files.list(tagsDir)) {
                files.sorted().forEach(p -> System.out.println(p.getFileName().toString()));
            }
            return;
        }

        String tagName = args[1];
        Path tagPath = tagsDir.resolve(tagName);

        if (Files.exists(tagPath)) {
            System.out.println("Tag already exists: " + tagName);
            return;
        }

        String targetSha = repo.resolveRef("HEAD");
        if (targetSha == null) {
            System.out.println("Cannot create tag: no commits yet.");
            return;
        }

        Files.writeString(tagPath, targetSha + "\n");
        System.out.println("Created tag " + tagName + " -> " + targetSha.substring(0, 8));
    }
}
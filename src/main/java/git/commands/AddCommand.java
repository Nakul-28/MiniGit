package git.commands;

import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Blob;

import java.nio.file.Files;
import java.nio.file.Path;

public class AddCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: mgit add <file>");
            return;
        }

        String relativePath = args[1];
        Path filePath = repo.getRoot().resolve(relativePath);

        if (!Files.exists(filePath)) {
            System.out.println("File not found: " + relativePath);
            return;
        }

        byte[] content = Files.readAllBytes(filePath);
        Blob blob = new Blob(content);
        repo.writeObject(blob);

        Index index = Index.load(repo.getGitDir());
        index.put(relativePath, new Index.Entry(
            blob.getSha(),
            content.length,
            Files.getLastModifiedTime(filePath).toMillis()
        ));
        index.save();

        System.out.println("Added " + relativePath);
    }
}
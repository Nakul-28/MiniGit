package git;

import git.objects.GitObject;
import git.util.HashUtil;
import git.util.ZlibUtil;

import java.io.IOException;
import java.nio.file.*;

public class Repository {
    private final Path root;
    private final Path gitDir;

    public Repository(Path root) {
        this.root = root;
        this.gitDir = root.resolve(".git");
    }

    public static Repository init(Path root) throws IOException {
        Repository repo = new Repository(root);
        Files.createDirectories(repo.gitDir.resolve("objects"));
        Files.createDirectories(repo.gitDir.resolve("refs/heads"));
        Files.writeString(repo.gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
        System.out.println("Initialized empty minigit repository in " + repo.gitDir);
        return repo;
    }

    public void writeObject(GitObject obj) throws IOException {
        byte[] data = obj.serialize();
        String sha = HashUtil.sha256Hex(data);
        byte[] compressed = ZlibUtil.compress(data);
        Path objPath = gitDir.resolve("objects")
                             .resolve(sha.substring(0, 2))
                             .resolve(sha.substring(2));
        Files.createDirectories(objPath.getParent());
        Files.write(objPath, compressed);
        obj.setSha(sha);
    }

    public byte[] readObject(String sha) throws Exception {
        Path objPath = gitDir.resolve("objects")
                             .resolve(sha.substring(0, 2))
                             .resolve(sha.substring(2));
        return ZlibUtil.decompress(Files.readAllBytes(objPath));
    }

    public String resolveRef(String ref) throws IOException {
        Path refPath = gitDir.resolve(ref);
        if (!Files.exists(refPath)) return null;
        String content = Files.readString(refPath).strip();
        if (content.startsWith("ref: ")) return resolveRef(content.substring(5));
        return content;
    }

    public void updateRef(String ref, String sha) throws IOException {
        Path refPath = gitDir.resolve(ref);
        Files.createDirectories(refPath.getParent());
        Files.writeString(refPath, sha + "\n");
    }

    public String readHeadRefPath() throws IOException {
    String content = Files.readString(gitDir.resolve("HEAD")).strip();
    if (content.startsWith("ref: ")) {
        return content.substring(5);
    }
    throw new IllegalStateException("HEAD is detached, not pointing to a branch");
}

    public Path getRoot()   { return root; }
    public Path getGitDir() { return gitDir; }
    public boolean exists() { return Files.exists(gitDir); }
}
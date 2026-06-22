package git;

import git.index.Index;
import git.objects.Commit;
import git.objects.GitObject;
import git.objects.Tree;
import git.objects.Commit;
import git.objects.Tree;
import git.util.HashUtil;
import git.util.ZlibUtil;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

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
        if (!Files.exists(refPath))
            return null;
        String content = Files.readString(refPath).strip();
        if (content.startsWith("ref: "))
            return resolveRef(content.substring(5));
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

    public Path getRoot() {
        return root;
    }

    public Path getGitDir() {
        return gitDir;
    }

    public boolean exists() {
        return Files.exists(gitDir);

    }

    public void checkoutTree(Tree tree, Path targetDir) throws Exception {
        // collect the set of files the new tree wants
        Set<String> desiredFiles = new HashSet<>();
        for (Tree.Entry entry : tree.getEntries()) {
            desiredFiles.add(entry.name);
        }

        // delete any tracked file currently in the working directory
        // that isn't part of the new tree
        Index currentIndex = Index.load(gitDir);
        for (String trackedPath : currentIndex.getEntries().keySet()) {
            if (!desiredFiles.contains(trackedPath)) {
                Path staleFile = targetDir.resolve(trackedPath);
                Files.deleteIfExists(staleFile);
            }
        }

        // write the new tree's files
        for (Tree.Entry entry : tree.getEntries()) {
            byte[] blobData = readObject(entry.sha);
            int nullIndex = 0;
            while (blobData[nullIndex] != 0)
                nullIndex++;
            byte[] content = new byte[blobData.length - nullIndex - 1];
            System.arraycopy(blobData, nullIndex + 1, content, 0, content.length);

            Path filePath = targetDir.resolve(entry.name);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content);
        }
    }

    public String findMergeBase(String sha1, String sha2) throws Exception {
        Set<String> ancestors1 = collectAncestors(sha1);
        Set<String> ancestors2 = collectAncestors(sha2);

        // walk sha1's own ancestry in order, return first one also in sha2's set
        String current = sha1;
        while (current != null) {
            if (ancestors2.contains(current)) {
                return current;
            }
            Commit c = Commit.deserialize(readObject(current));
            current = c.getParentSha();
        }
        return null; // no common ancestor (shouldn't happen in a normal repo)
    }

    private Set<String> collectAncestors(String sha) throws Exception {
        Set<String> ancestors = new HashSet<>();
        String current = sha;
        while (current != null) {
            ancestors.add(current);
            Commit c = Commit.deserialize(readObject(current));
            current = c.getParentSha();
        }
        return ancestors;
    }
}
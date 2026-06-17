package git;

import git.objects.Blob;
import git.objects.Commit;
import git.objects.Tree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommitTest {

    @Test
    void commitRoundTrip(@TempDir Path tempDir) throws Exception {
        Repository repo = new Repository(tempDir);
        Repository.init(tempDir);

        // build blob → tree → commit chain
        Blob blob = new Blob("hello".getBytes());
        repo.writeObject(blob);

        Tree tree = new Tree(List.of(
            new Tree.Entry("100644", "hello.txt", blob.getSha())
        ));
        repo.writeObject(tree);

        Commit commit = new Commit(tree.getSha(), null, "Nakul", "initial commit");
        repo.writeObject(commit);

        assertThat(commit.getSha()).isNotNull().hasSize(64);

        // round-trip
        byte[] stored = repo.readObject(commit.getSha());
        Commit recovered = Commit.deserialize(stored);

        assertThat(recovered.getTreeSha()).isEqualTo(tree.getSha());
        assertThat(recovered.getParentSha()).isNull();
        assertThat(recovered.getMessage()).isEqualTo("initial commit");
        assertThat(recovered.getAuthor()).isEqualTo("Nakul");
    }

    @Test
    void commitWithParent(@TempDir Path tempDir) throws Exception {
        Repository repo = new Repository(tempDir);
        Repository.init(tempDir);

        Blob blob = new Blob("v1".getBytes());
        repo.writeObject(blob);
        Tree tree = new Tree(List.of(new Tree.Entry("100644", "f.txt", blob.getSha())));
        repo.writeObject(tree);

        Commit first = new Commit(tree.getSha(), null, "Nakul", "first");
        repo.writeObject(first);

        Commit second = new Commit(tree.getSha(), first.getSha(), "Nakul", "second");
        repo.writeObject(second);

        byte[] stored = repo.readObject(second.getSha());
        Commit recovered = Commit.deserialize(stored);

        assertThat(recovered.getParentSha()).isEqualTo(first.getSha());
    }
}
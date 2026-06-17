package git;

import git.objects.Blob;
import git.objects.Tree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TreeTest {

    @Test
    void treeRoundTrip(@TempDir Path tempDir) throws Exception {
        Repository repo = new Repository(tempDir);
        Repository.init(tempDir);

        // first create a blob to point at
        Blob blob = new Blob("hello minigit".getBytes());
        repo.writeObject(blob);

        // create a tree with one entry pointing to that blob
        Tree tree = new Tree(List.of(
            new Tree.Entry("100644", "hello.txt", blob.getSha())
        ));
        repo.writeObject(tree);

        assertThat(tree.getSha()).isNotNull().hasSize(64);

        // round-trip
        byte[] stored = repo.readObject(tree.getSha());
        Tree recovered = Tree.deserialize(stored);

        assertThat(recovered.getEntries()).hasSize(1);
        assertThat(recovered.getEntries().get(0).name).isEqualTo("hello.txt");
        assertThat(recovered.getEntries().get(0).sha).isEqualTo(blob.getSha());
    }
}
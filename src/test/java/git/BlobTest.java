package git;

import git.objects.Blob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BlobTest {

    @Test
    void blobRoundTrip(@TempDir Path tempDir) throws Exception {
        Repository repo = new Repository(tempDir);
        Repository.init(tempDir);

        byte[] originalContent = "hello minigit".getBytes();
        Blob blob = new Blob(originalContent);
        repo.writeObject(blob);

        // SHA should now be set
        assertThat(blob.getSha()).isNotNull().hasSize(64); // SHA-256 = 64 hex chars

        // read it back and deserialize
        byte[] stored = repo.readObject(blob.getSha());
        Blob recovered = Blob.deserialize(stored);

        assertThat(recovered.getContent()).isEqualTo(originalContent);
    }
}
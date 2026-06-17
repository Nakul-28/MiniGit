package git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTest {
    @Test
    void initCreatesGitStructure(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        assertThat(tempDir.resolve(".git/HEAD")).exists();
        assertThat(tempDir.resolve(".git/objects")).isDirectory();
        assertThat(tempDir.resolve(".git/refs/heads")).isDirectory();
        assertThat(tempDir.resolve(".git/HEAD"))
            .content().isEqualTo("ref: refs/heads/main\n");
    }
}
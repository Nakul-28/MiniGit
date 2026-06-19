package git;

import git.commands.AddCommand;
import git.index.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AddCommandTest {

    @Test
    void addStagesFileInIndex(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello minigit");

        new AddCommand().execute(repo, new String[]{"add", "hello.txt"});

        Index index = Index.load(repo.getGitDir());
        assertThat(index.get("hello.txt")).isNotNull();
        assertThat(index.get("hello.txt").size).isEqualTo(13);
    }
}
package git;

import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.commands.LogCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogCommandTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void captureStdout() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    @Test
    void noCommitsShowsEmptyMessage(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        new LogCommand().execute(repo, new String[]{"log"});

        assertThat(outContent.toString()).contains("No commits yet.");
    }

    @Test
    void singleCommitShowsShaAuthorAndMessage(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "initial commit"});
        String sha = repo.resolveRef("HEAD");

        outContent.reset();
        new LogCommand().execute(repo, new String[]{"log"});

        String output = outContent.toString();
        assertThat(output).contains("commit " + sha);
        assertThat(output).contains("Author: Nakul");
        assertThat(output).contains("initial commit");
    }

    @Test
    void multipleCommitsShowNewestFirst(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        Files.writeString(tempDir.resolve("a.txt"), "v2");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "second"});

        outContent.reset();
        new LogCommand().execute(repo, new String[]{"log"});

        String output = outContent.toString();
        int secondIndex = output.indexOf("second");
        int firstIndex = output.indexOf("first");

        assertThat(secondIndex).isLessThan(firstIndex); // newest commit appears first
    }
}
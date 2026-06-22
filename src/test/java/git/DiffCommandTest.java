package git;

import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.commands.DiffCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DiffCommandTest {

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
    void noChangesReportsNoDiff(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "unchanged content");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});

        outContent.reset();
        new DiffCommand().execute(repo, new String[]{"diff", "a.txt"});

        assertThat(outContent.toString()).contains("No changes.");
    }

    @Test
    void detectsAddedLine(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "line one");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});

        Files.writeString(tempDir.resolve("a.txt"), "line one\nline two");

        outContent.reset();
        new DiffCommand().execute(repo, new String[]{"diff", "a.txt"});

        String output = outContent.toString();
        assertThat(output).contains("+ line two");
    }

    @Test
    void untrackedFileReportsNotTracked(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("untracked.txt"), "content");

        new DiffCommand().execute(repo, new String[]{"diff", "untracked.txt"});

        assertThat(outContent.toString()).contains("is not tracked.");
    }
}
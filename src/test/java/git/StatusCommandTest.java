package git;

import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.commands.StatusCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StatusCommandTest {

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
    void cleanWorkingTreeReportsNothingToCommit(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "content");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        outContent.reset();
        new StatusCommand().execute(repo, new String[]{"status"});

        assertThat(outContent.toString()).contains("Nothing to commit, working tree clean.");
    }

    @Test
    void untrackedFileIsReported(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("new.txt"), "content");

        new StatusCommand().execute(repo, new String[]{"status"});

        assertThat(outContent.toString()).contains("Untracked files:");
        assertThat(outContent.toString()).contains("new.txt");
    }

    @Test
    void stagedFileIsReported(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("staged.txt"), "content");
        new AddCommand().execute(repo, new String[]{"add", "staged.txt"});

        outContent.reset();
        new StatusCommand().execute(repo, new String[]{"status"});

        assertThat(outContent.toString()).contains("Changes to be committed:");
        assertThat(outContent.toString()).contains("staged.txt");
    }

    @Test
    void modifiedFileIsReported(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("m.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "m.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        Files.writeString(tempDir.resolve("m.txt"), "v2 - changed");

        outContent.reset();
        new StatusCommand().execute(repo, new String[]{"status"});

        assertThat(outContent.toString()).contains("Changes not staged for commit:");
        assertThat(outContent.toString()).contains("modified: m.txt");
    }

    @Test
    void deletedTrackedFileIsReported(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("d.txt"), "content");
        new AddCommand().execute(repo, new String[]{"add", "d.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        Files.delete(tempDir.resolve("d.txt"));

        outContent.reset();
        new StatusCommand().execute(repo, new String[]{"status"});

        assertThat(outContent.toString()).contains("Deleted files:");
        assertThat(outContent.toString()).contains("deleted: d.txt");
    }
}
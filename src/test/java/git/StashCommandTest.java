package git;

import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.commands.StashCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StashCommandTest {

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
    void stashSaveResetsWorkingDirectoryToHead(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "committed content");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        // make an uncommitted change and stage it
        Files.writeString(tempDir.resolve("a.txt"), "WIP change");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});

        new StashCommand().execute(repo, new String[]{"stash", "save"});

        // working directory should be back to the committed version
        assertThat(Files.readString(tempDir.resolve("a.txt"))).isEqualTo("committed content");

        Path stashRef = repo.getGitDir().resolve("refs/stash");
        assertThat(stashRef).exists();
    }

    @Test
    void stashPopRestoresChangesAndRemovesStashRef(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "committed content");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        Files.writeString(tempDir.resolve("a.txt"), "WIP change");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new StashCommand().execute(repo, new String[]{"stash", "save"});

        new StashCommand().execute(repo, new String[]{"stash", "pop"});

        assertThat(Files.readString(tempDir.resolve("a.txt"))).isEqualTo("WIP change");

        Path stashRef = repo.getGitDir().resolve("refs/stash");
        assertThat(stashRef).doesNotExist();
    }

    @Test
    void popWithNoStashReportsNoStashFound(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        outContent.reset();
        new StashCommand().execute(repo, new String[]{"stash", "pop"});

        assertThat(outContent.toString()).contains("No stash found.");
    }

    @Test
    void saveWithNothingStagedReportsNothingToStash(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        outContent.reset();
        new StashCommand().execute(repo, new String[]{"stash", "save"});

        assertThat(outContent.toString()).contains("No staged changes to stash.");
    }
}
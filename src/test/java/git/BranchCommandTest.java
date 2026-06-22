package git;

import git.commands.AddCommand;
import git.commands.BranchCommand;
import git.commands.CommitCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BranchCommandTest {

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
    void createsBranchPointingAtHead(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});
        String headSha = repo.resolveRef("HEAD");

        new BranchCommand().execute(repo, new String[]{"branch", "feature"});

        String featureSha = repo.resolveRef("refs/heads/feature");
        assertThat(featureSha).isEqualTo(headSha);
    }

    @Test
    void listsAllBranchesWithCurrentMarked(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        new BranchCommand().execute(repo, new String[]{"branch", "feature"});

        outContent.reset();
        new BranchCommand().execute(repo, new String[]{"branch"});

        String output = outContent.toString();
        assertThat(output).contains("* main");
        assertThat(output).contains("feature");
    }

    @Test
    void rejectsDuplicateBranchName(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        new BranchCommand().execute(repo, new String[]{"branch", "feature"});
        String featureSha = repo.resolveRef("refs/heads/feature");

        outContent.reset();
        new BranchCommand().execute(repo, new String[]{"branch", "feature"});

        assertThat(outContent.toString()).contains("Branch already exists");
        // ensure it wasn't accidentally moved/recreated
        assertThat(repo.resolveRef("refs/heads/feature")).isEqualTo(featureSha);
    }

    @Test
    void refusesBranchCreationWithNoCommits(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        outContent.reset();
        new BranchCommand().execute(repo, new String[]{"branch", "feature"});

        assertThat(outContent.toString()).contains("Cannot create branch: no commits yet.");
    }
}
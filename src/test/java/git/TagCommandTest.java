package git;

import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.commands.TagCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TagCommandTest {

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
    void createsTagPointingAtHead(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});
        String headSha = repo.resolveRef("HEAD");

        new TagCommand().execute(repo, new String[]{"tag", "v1.0"});

        Path tagPath = repo.getGitDir().resolve("refs/tags/v1.0");
        assertThat(tagPath).exists();
        assertThat(Files.readString(tagPath).strip()).isEqualTo(headSha);
    }

    @Test
    void listsAllTags(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        new TagCommand().execute(repo, new String[]{"tag", "v1.0"});
        new TagCommand().execute(repo, new String[]{"tag", "v2.0"});

        outContent.reset();
        new TagCommand().execute(repo, new String[]{"tag"});

        String output = outContent.toString();
        assertThat(output).contains("v1.0");
        assertThat(output).contains("v2.0");
    }

    @Test
    void rejectsDuplicateTagName(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});
        new TagCommand().execute(repo, new String[]{"tag", "v1.0"});
        String shaAfterFirstTag = Files.readString(repo.getGitDir().resolve("refs/tags/v1.0")).strip();

        // commit again, then try to re-tag with the same name
        Files.writeString(tempDir.resolve("a.txt"), "v2");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "second"});

        outContent.reset();
        new TagCommand().execute(repo, new String[]{"tag", "v1.0"});

        assertThat(outContent.toString()).contains("Tag already exists");
        // tag should NOT have moved to the new commit
        String shaAfterAttempt = Files.readString(repo.getGitDir().resolve("refs/tags/v1.0")).strip();
        assertThat(shaAfterAttempt).isEqualTo(shaAfterFirstTag);
    }

    @Test
    void refusesTagCreationWithNoCommits(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        outContent.reset();
        new TagCommand().execute(repo, new String[]{"tag", "v1.0"});

        assertThat(outContent.toString()).contains("Cannot create tag: no commits yet.");
    }
}
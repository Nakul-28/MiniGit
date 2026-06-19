package git;

import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.objects.Commit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CommitCommandTest {

    @Test
    void commitCreatesCommitAndMovesRef(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("hello.txt"), "hello minigit");
        new AddCommand().execute(repo, new String[]{"add", "hello.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "initial commit"});

        String headSha = repo.resolveRef("HEAD");
        assertThat(headSha).isNotNull();

        byte[] stored = repo.readObject(headSha);
        Commit commit = Commit.deserialize(stored);
        assertThat(commit.getMessage()).isEqualTo("initial commit");
        assertThat(commit.getParentSha()).isNull();
    }

    @Test
    void secondCommitHasParent(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});
        String firstSha = repo.resolveRef("HEAD");

        Files.writeString(tempDir.resolve("a.txt"), "v2");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "second"});
        String secondSha = repo.resolveRef("HEAD");

        Commit second = Commit.deserialize(repo.readObject(secondSha));
        assertThat(second.getParentSha()).isEqualTo(firstSha);
        assertThat(secondSha).isNotEqualTo(firstSha);
    }
}
package git;

import git.commands.AddCommand;
import git.commands.BranchCommand;
import git.commands.CheckoutCommand;
import git.commands.CommitCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutCommandTest {

    @Test
    void checkoutSwitchesBranchAndUpdatesWorkingDirectory(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("main.txt"), "main content");
        new AddCommand().execute(repo, new String[]{"add", "main.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "main commit"});
        String mainSha = repo.resolveRef("HEAD");

        new BranchCommand().execute(repo, new String[]{"branch", "feature"});

        // switch to feature and commit a new file there
        new CheckoutCommand().execute(repo, new String[]{"checkout", "feature"});
        Files.writeString(tempDir.resolve("feature.txt"), "feature content");
        new AddCommand().execute(repo, new String[]{"add", "feature.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "feature commit"});

        assertThat(tempDir.resolve("feature.txt")).exists();

        // switch back to main - feature.txt should disappear
        new CheckoutCommand().execute(repo, new String[]{"checkout", "main"});

        assertThat(tempDir.resolve("main.txt")).exists();
        assertThat(tempDir.resolve("feature.txt")).doesNotExist();
        assertThat(repo.resolveRef("HEAD")).isEqualTo(mainSha);
    }

    @Test
    void checkoutBlocksWhenTrackedFileIsModified(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});
        new BranchCommand().execute(repo, new String[]{"branch", "other"});

        // modify a.txt without committing
        Files.writeString(tempDir.resolve("a.txt"), "uncommitted change");

        String shaBeforeAttempt = repo.resolveRef("HEAD");
        new CheckoutCommand().execute(repo, new String[]{"checkout", "other"});

        // checkout should have been blocked - HEAD unchanged, file unchanged
        assertThat(repo.resolveRef("HEAD")).isEqualTo(shaBeforeAttempt);
        assertThat(Files.readString(tempDir.resolve("a.txt"))).isEqualTo("uncommitted change");
    }

    @Test
    void checkoutBlocksWhenUntrackedFileWouldBeOverwritten(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        // create clash.txt on main and commit it
        Files.writeString(tempDir.resolve("clash.txt"), "main version");
        new AddCommand().execute(repo, new String[]{"add", "clash.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "add clash.txt"});
        String mainSha = repo.resolveRef("HEAD");

        new BranchCommand().execute(repo, new String[]{"branch", "other"});
        new CheckoutCommand().execute(repo, new String[]{"checkout", "other"});

        // on "other", clash.txt isn't tracked (checkout wiped it since other's
        // tree at branch-creation time still had clash.txt from main - so let's
        // simulate a genuine untracked collision by deleting it from index manually
        // and rewriting with different content, untracked
        Files.writeString(tempDir.resolve("clash.txt"), "other version (untracked)");

        // force it out of the index to simulate "untracked"
        var index = git.index.Index.load(repo.getGitDir());
        index.remove("clash.txt");
        index.save();

        new CheckoutCommand().execute(repo, new String[]{"checkout", "main"});

        // checkout should be blocked - HEAD should still be on "other"
        String headRefPath = repo.readHeadRefPath();
        assertThat(headRefPath).isEqualTo("refs/heads/other");
        assertThat(Files.readString(tempDir.resolve("clash.txt"))).isEqualTo("other version (untracked)");
    }

    @Test
    void checkoutFailsForNonexistentBranch(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("a.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "a.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});

        String shaBefore = repo.resolveRef("HEAD");
        new CheckoutCommand().execute(repo, new String[]{"checkout", "does-not-exist"});

        // HEAD should be unchanged since the branch doesn't exist
        assertThat(repo.resolveRef("HEAD")).isEqualTo(shaBefore);
    }
}
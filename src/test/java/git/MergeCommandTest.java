package git;

import git.commands.AddCommand;
import git.commands.BranchCommand;
import git.commands.CommitCommand;
import git.commands.MergeCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MergeCommandTest {

    @Test
    void cleanMergeWithNoConflict(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        // commit on main
        Files.writeString(tempDir.resolve("base.txt"), "base content");
        new AddCommand().execute(repo, new String[]{"add", "base.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "base commit"});
        String baseSha = repo.resolveRef("HEAD");

        // create and switch to feature branch
        repo.updateRef("refs/heads/feature", baseSha);
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/feature\n");

        // feature adds a new file (no overlap with main)
        Files.writeString(tempDir.resolve("feature.txt"), "feature content");
        new AddCommand().execute(repo, new String[]{"add", "feature.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "feature commit"});

        // back to main
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/main\n");

        // merge feature into main - should succeed with no conflict
        new MergeCommand().execute(repo, new String[]{"merge", "feature"});

        // both files should now exist in working directory
        assertThat(tempDir.resolve("base.txt")).exists();
        assertThat(tempDir.resolve("feature.txt")).exists();
        assertThat(Files.readString(tempDir.resolve("feature.txt"))).isEqualTo("feature content");
    }

    @Test
    void conflictingMergeWritesMarkers(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        // base commit
        Files.writeString(tempDir.resolve("shared.txt"), "original");
        new AddCommand().execute(repo, new String[]{"add", "shared.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "base"});
        String baseSha = repo.resolveRef("HEAD");

        // branch off, modify shared.txt differently
        repo.updateRef("refs/heads/feature", baseSha);
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/feature\n");
        Files.writeString(tempDir.resolve("shared.txt"), "feature version");
        new AddCommand().execute(repo, new String[]{"add", "shared.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "feature edit"});

        // back to main, modify shared.txt differently too
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.writeString(tempDir.resolve("shared.txt"), "main version");
        new AddCommand().execute(repo, new String[]{"add", "shared.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "main edit"});

        // merge - should conflict
        new MergeCommand().execute(repo, new String[]{"merge", "feature"});

        String result = Files.readString(tempDir.resolve("shared.txt"));
        assertThat(result).contains("<<<<<<< HEAD");
        assertThat(result).contains("main version");
        assertThat(result).contains("=======");
        assertThat(result).contains("feature version");
        assertThat(result).contains(">>>>>>> incoming");
    }

    @Test
    void fastForwardMergeMovesPointer(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        Files.writeString(tempDir.resolve("f.txt"), "v1");
        new AddCommand().execute(repo, new String[]{"add", "f.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "first"});
        String firstSha = repo.resolveRef("HEAD");

        // branch off, current main does NOT move forward
        repo.updateRef("refs/heads/feature", firstSha);
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/feature\n");
        Files.writeString(tempDir.resolve("f.txt"), "v2");
        new AddCommand().execute(repo, new String[]{"add", "f.txt"});
        new CommitCommand().execute(repo, new String[]{"commit", "second"});
        String secondSha = repo.resolveRef("HEAD");

        // main is still at firstSha - merging feature into main should fast-forward
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        new MergeCommand().execute(repo, new String[]{"merge", "feature"});

        String mainNowAt = repo.resolveRef("HEAD");
        assertThat(mainNowAt).isEqualTo(secondSha);
    }
}
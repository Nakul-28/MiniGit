package git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MergeBaseTest {

    @Test
    void findsCommonAncestor(@TempDir Path tempDir) throws Exception {
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        // commit A on main
        Files.writeString(tempDir.resolve("f.txt"), "v1");
        new git.commands.AddCommand().execute(repo, new String[]{"add", "f.txt"});
        new git.commands.CommitCommand().execute(repo, new String[]{"commit", "A"});
        String shaA = repo.resolveRef("HEAD");

        // create branch "feature" pointing at A
        repo.updateRef("refs/heads/feature", shaA);

        // commit B on main (main moves forward)
        Files.writeString(tempDir.resolve("f.txt"), "v2-main");
        new git.commands.AddCommand().execute(repo, new String[]{"add", "f.txt"});
        new git.commands.CommitCommand().execute(repo, new String[]{"commit", "B"});
        String shaB = repo.resolveRef("HEAD");

        // switch to feature, commit C (feature moves forward from A)
        Files.writeString(tempDir.resolve(".git/HEAD"), "ref: refs/heads/feature\n");
        Files.writeString(tempDir.resolve("f.txt"), "v2-feature");
        new git.commands.AddCommand().execute(repo, new String[]{"add", "f.txt"});
        new git.commands.CommitCommand().execute(repo, new String[]{"commit", "C"});
        String shaC = repo.resolveRef("HEAD");

        String mergeBase = repo.findMergeBase(shaB, shaC);
        assertThat(mergeBase).isEqualTo(shaA);
    }
}
package git.commands;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import git.Main;
import git.Repository;
import git.index.Index;
import git.objects.Blob;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class DiffCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: mgit diff <file>");
            return;
        }

        String relPath = args[1];
        Index index = Index.load(repo.getGitDir());
        Index.Entry indexEntry = index.get(relPath);

        if (indexEntry == null) {
            System.out.println(relPath + " is not tracked.");
            return;
        }

        // get the indexed (last staged/committed) version
        byte[] indexedRaw = repo.readObject(indexEntry.sha);
        String indexedContent = stripBlobHeader(indexedRaw);
        List<String> oldLines = Arrays.asList(indexedContent.split("\n", -1));

        // get the current working directory version
        Path filePath = repo.getRoot().resolve(relPath);
        String currentContent = Files.readString(filePath, StandardCharsets.UTF_8);
        List<String> newLines = Arrays.asList(currentContent.split("\n", -1));

        Patch<String> patch = DiffUtils.diff(oldLines, newLines);

        if (patch.getDeltas().isEmpty()) {
            System.out.println("No changes.");
            return;
        }

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            System.out.println("@@ " + delta.getType() + " @@");
            delta.getSource().getLines().forEach(line -> System.out.println("- " + line));
            delta.getTarget().getLines().forEach(line -> System.out.println("+ " + line));
        }
    }

    private String stripBlobHeader(byte[] data) {
        int nullIndex = 0;
        while (data[nullIndex] != 0) nullIndex++;
        return new String(data, nullIndex + 1, data.length - nullIndex - 1, StandardCharsets.UTF_8);
    }
}
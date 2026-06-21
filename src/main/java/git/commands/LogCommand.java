package git.commands;

import git.Main;
import git.Repository;
import git.objects.Commit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LogCommand implements Main.Command {

    @Override
    public void execute(Repository repo, String[] args) throws Exception {
        String sha = repo.resolveRef("HEAD");
        if (sha == null) {
            System.out.println("No commits yet.");
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("EEE MMM d HH:mm:ss yyyy")
                .withZone(ZoneId.systemDefault());

        while (sha != null) {
            Commit commit = Commit.deserialize(repo.readObject(sha));

            System.out.println("commit " + sha);
            System.out.println("Author: " + commit.getAuthor());
            System.out.println("Date:   " + fmt.format(Instant.ofEpochSecond(commit.getTimestamp())));
            System.out.println();
            System.out.println("    " + commit.getMessage());
            System.out.println();

            sha = commit.getParentSha();
        }
    }
}
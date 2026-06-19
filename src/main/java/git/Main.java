package git;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import git.commands.AddCommand;
import git.commands.CommitCommand;

public class Main {
    public interface Command {
        void execute(Repository repo, String[] args) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: mgit <command> [args]");
            return;
        }

        Map<String, Command> commands = new HashMap<>();
        commands.put("init", (repo, a) -> Repository.init(Paths.get(".").toAbsolutePath().normalize()));
        commands.put("add", new AddCommand());
        commands.put("commit", new CommitCommand());

        String cmd = args[0];
        if (!commands.containsKey(cmd)) {
            System.out.println("Unknown command: " + cmd);
            return;
        }

        Repository repo = new Repository(Paths.get(".").toAbsolutePath().normalize());
        commands.get(cmd).execute(repo, args);
    }
}
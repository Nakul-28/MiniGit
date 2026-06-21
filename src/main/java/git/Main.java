package git;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import git.commands.AddCommand;
import git.commands.CommitCommand;
import git.commands.LogCommand;
import git.commands.StatusCommand;
import git.commands.BranchCommand;
import git.commands.CheckoutCommand;
import git.commands.DiffCommand;

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
        commands.put("log", new LogCommand());
        commands.put("status", new StatusCommand());
        commands.put("branch", new BranchCommand());
        commands.put("checkout", new CheckoutCommand());
        commands.put("diff", new DiffCommand());

        String cmd = args[0];
        if (!commands.containsKey(cmd)) {
            System.out.println("Unknown command: " + cmd);
            return;
        }

        Repository repo = new Repository(Paths.get(".").toAbsolutePath().normalize());
        commands.get(cmd).execute(repo, args);
    }
}
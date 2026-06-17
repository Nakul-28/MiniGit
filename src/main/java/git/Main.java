package git;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Main {
    interface Command {
        void execute(Repository repo, String[] args) throws Exception;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: mgit <command> [args]");
            return;
        }

        Map<String, Command> commands = new HashMap<>();
        commands.put("init", (repo, a) -> Repository.init(Paths.get(".").toAbsolutePath().normalize()));
        // add more commands here as you build them

        String cmd = args[0];
        if (!commands.containsKey(cmd)) {
            System.out.println("Unknown command: " + cmd);
            return;
        }

        Repository repo = new Repository(Paths.get(".").toAbsolutePath().normalize());
        commands.get(cmd).execute(repo, args);
    }
}
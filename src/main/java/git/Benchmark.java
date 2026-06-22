package git;

import git.objects.Blob;
import java.nio.file.Files;
import java.nio.file.Path;

public class Benchmark {
    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("minigit-bench");
        Repository.init(tempDir);
        Repository repo = new Repository(tempDir);

        int warmup = 1000;
        int n = 10_000;

        // warmup run - let JIT kick in, don't count this
        for (int i = 0; i < warmup; i++) {
            Blob blob = new Blob(("warmup " + i).getBytes());
            repo.writeObject(blob);
        }

        // actual measured run
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            Blob blob = new Blob(("test content " + i).getBytes());
            repo.writeObject(blob);
        }
        long elapsed = System.nanoTime() - start;

        double seconds = elapsed / 1e9;
        double objectsPerSec = n / seconds;
        double avgMicros = (elapsed / 1000.0) / n;

        System.out.printf("Wrote %d objects in %.2f ms%n", n, elapsed / 1e6);
        System.out.printf("Throughput: %.0f objects/sec%n", objectsPerSec);
        System.out.printf("Average latency: %.1f microseconds/object%n", avgMicros);
    }
}
package git.objects;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class Commit extends GitObject {

    private final String treeSha;
    private final String parentSha;  // null for the very first commit
    private final String author;
    private final long timestamp;
    private final String message;

    public Commit(String treeSha, String parentSha, String author, String message) {
        this.treeSha   = treeSha;
        this.parentSha = parentSha;
        this.author    = author;
        this.timestamp = Instant.now().getEpochSecond();
        this.message   = message;
    }

    // used by deserialize — lets us restore the original timestamp
    private Commit(String treeSha, String parentSha, String author,
                   long timestamp, String message) {
        this.treeSha   = treeSha;
        this.parentSha = parentSha;
        this.author    = author;
        this.timestamp = timestamp;
        this.message   = message;
    }

    @Override
    public String getType() { return "commit"; }

    @Override
    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("tree ").append(treeSha).append("\n");
        if (parentSha != null)
            sb.append("parent ").append(parentSha).append("\n");
        sb.append("author ").append(author).append(" ").append(timestamp).append("\n");
        sb.append("\n");
        sb.append(message);

        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] header = ("commit " + body.length + "\0")
                            .getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[header.length + body.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(body,   0, result, header.length, body.length);
        return result;
    }

    public static Commit deserialize(byte[] data) {
        // skip header ("commit <size>\0")
        int i = 0;
        while (data[i] != 0) i++;
        i++;

        String body = new String(data, i, data.length - i, StandardCharsets.UTF_8);
        String[] lines = body.split("\n", -1);

        String treeSha   = null;
        String parentSha = null;
        String author    = null;
        long   timestamp = 0;
        StringBuilder message = new StringBuilder();
        boolean inMessage = false;

        for (String line : lines) {
            if (inMessage) {
                message.append(line).append("\n");
            } else if (line.isEmpty()) {
                inMessage = true;
            } else if (line.startsWith("tree ")) {
                treeSha = line.substring(5);
            } else if (line.startsWith("parent ")) {
                parentSha = line.substring(7);
            } else if (line.startsWith("author ")) {
                // format: "author <name> <timestamp>"
                String[] parts = line.substring(7).split(" ");
                timestamp = Long.parseLong(parts[parts.length - 1]);
                author = line.substring(7, line.lastIndexOf(" "));
            }
        }

        return new Commit(treeSha, parentSha, author,
                          timestamp, message.toString().strip());
    }

    public String getTreeSha()   { return treeSha; }
    public String getParentSha() { return parentSha; }
    public String getAuthor()    { return author; }
    public long   getTimestamp() { return timestamp; }
    public String getMessage()   { return message; }
}
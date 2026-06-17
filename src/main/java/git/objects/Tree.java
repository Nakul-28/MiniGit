package git.objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Tree extends GitObject {

    public static class Entry {
        public final String mode;  // "100644" for file, "040000" for subtree
        public final String name;
        public final String sha;

        public Entry(String mode, String name, String sha) {
            this.mode = mode;
            this.name = name;
            this.sha   = sha;
        }
    }

    private final List<Entry> entries;

    public Tree(List<Entry> entries) {
        this.entries = entries;
    }

    @Override
    public String getType() { return "tree"; }

    @Override
    public byte[] serialize() {
        // format: "tree <size>\0" + for each entry: "<mode> <name>\0<sha-as-bytes>"
        try {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            for (Entry e : entries) {
                body.write((e.mode + " " + e.name + "\0").getBytes(StandardCharsets.UTF_8));
                // store sha as 32 raw bytes (SHA-256), not hex string
                body.write(hexToBytes(e.sha));
            }
            byte[] bodyBytes = body.toByteArray();
            byte[] header = ("tree " + bodyBytes.length + "\0")
                                .getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(header);
            out.write(bodyBytes);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Tree deserialize(byte[] data) {
        // skip header ("tree <size>\0")
        int i = 0;
        while (data[i] != 0) i++;
        i++; // skip null byte

        List<Entry> entries = new ArrayList<>();
        while (i < data.length) {
            // read "<mode> <name>\0"
            int start = i;
            while (data[i] != 0) i++;
            String modeAndName = new String(data, start, i - start, StandardCharsets.UTF_8);
            i++; // skip null byte

            String[] parts = modeAndName.split(" ", 2);
            String mode = parts[0];
            String name = parts[1];

            // read 32 raw bytes for SHA-256
            byte[] shaBytes = new byte[32];
            System.arraycopy(data, i, shaBytes, 0, 32);
            i += 32;

            entries.add(new Entry(mode, name, bytesToHex(shaBytes)));
        }
        return new Tree(entries);
    }

    public List<Entry> getEntries() { return entries; }

    // helpers
    private static byte[] hexToBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++)
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
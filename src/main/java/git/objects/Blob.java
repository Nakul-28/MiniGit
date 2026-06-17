package git.objects;

import java.nio.charset.StandardCharsets;

public class Blob extends GitObject {

    private final byte[] content;

    public Blob(byte[] content) {
        this.content = content;
    }

    @Override
    public String getType() {
        return "blob";
    }

    @Override
    public byte[] serialize() {
        // format: "blob <size>\0<content>"
        byte[] header = ("blob " + content.length + "\0")
                            .getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }

    public byte[] getContent() {
        return content;
    }
    public static Blob deserialize(byte[] data) {
    // data is "blob <size>\0<content>" — find the null byte
    int nullIndex = 0;
    while (data[nullIndex] != 0) nullIndex++;
    // everything after the null byte is the content
    byte[] content = new byte[data.length - nullIndex - 1];
    System.arraycopy(data, nullIndex + 1, content, 0, content.length);
    return new Blob(content);
}
}
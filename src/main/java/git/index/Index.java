package git.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Index {

    public static class Entry {
        public String sha;
        public long size;
        public long mtime; // last modified time, epoch millis

        public Entry(String sha, long size, long mtime) {
            this.sha = sha;
            this.size = size;
            this.mtime = mtime;
        }
    }

    private final Map<String, Entry> entries; // path -> entry
    private final Path indexPath;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Index(Path indexPath, Map<String, Entry> entries) {
        this.indexPath = indexPath;
        this.entries = entries;
    }

    public static Index load(Path gitDir) throws IOException {
        Path indexPath = gitDir.resolve("index");
        if (!Files.exists(indexPath)) {
            return new Index(indexPath, new LinkedHashMap<>());
        }
        String json = Files.readString(indexPath, StandardCharsets.UTF_8);
        var type = new TypeToken<LinkedHashMap<String, Entry>>(){}.getType();
        Map<String, Entry> entries = GSON.fromJson(json, type);
        if (entries == null) entries = new LinkedHashMap<>();
        return new Index(indexPath, entries);
    }

    public void save() throws IOException {
        Files.writeString(indexPath, GSON.toJson(entries), StandardCharsets.UTF_8);
    }

    public void put(String path, Entry entry) {
        entries.put(path, entry);
    }

    public void remove(String path) {
        entries.remove(path);
    }

    public Entry get(String path) {
        return entries.get(path);
    }

    public Map<String, Entry> getEntries() {
        return entries;
    }
}
package git.objects;

public abstract class GitObject {
    private String sha;

    public abstract byte[] serialize();
    public abstract String getType();   // "blob", "tree", "commit"

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
}
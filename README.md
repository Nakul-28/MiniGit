# MiniGit
A Git-inspired version control system in Java implementing content-addressable storage, commits, branches, and checkout from scratch.

## Why I built this

To genuinely understand how Git works under the hood rather than just using it
as a black box, I rebuilt its core engine: content-addressable storage, the
staging area, branch refs, and merge logic — including real conflict resolution.

## Architecture

### The object model

Every piece of data in MiniGit is one of three immutable, content-addressed
objects, identified by the SHA-256 hash of their serialized content:

- **Blob** — raw file content, no filename or metadata
- **Tree** — a directory snapshot: a list of (mode, filename, SHA) entries
  pointing to blobs or other trees
- **Commit** — points to a tree (the project snapshot), a parent commit SHA,
  author, timestamp, and message

Objects are zlib-compressed and stored at `.git/objects/<first 2 chars of
SHA>/<remaining chars>`, exactly mirroring real Git's storage layout. Because
objects are addressed by content hash, two commits that share an unchanged
file automatically share the same blob — no duplication.

**Design choice: SHA-256, not SHA-1.** Real Git defaults to SHA-1 for backward
compatibility with its 15-year-old ecosystem. Since MiniGit has no such
constraint, it uses SHA-256 throughout, avoiding the collision vulnerabilities
demonstrated against SHA-1 (e.g. the SHAttered attack, 2017).

### Staging area (the index)

`.git/index` tracks staged files as a JSON map of path → {blob SHA, size,
mtime}. `add` hashes a file, writes it as a blob, and updates this map.
`commit` reads the index, builds a `Tree` from its entries, and creates a
`Commit` pointing to that tree.

### Branches and refs

Branches are plain text files under `.git/refs/heads/<name>` containing a
commit SHA. `HEAD` either points to a branch (`ref: refs/heads/main`) or
directly to a commit SHA (detached state). `checkout` writes the target
branch's tree into the working directory, rebuilds the index to match, and
moves `HEAD`.

A safety check blocks checkout if it would silently overwrite uncommitted
changes — including the often-missed case of an *untracked* file colliding
with a file the target branch is about to create.

### Diff

Uses the [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils)
library to compute line-level deltas between a file's last-staged version and
its current working-directory state.

### Merge

1. **Merge base detection** — collects the full ancestor set of one branch's
   tip, then walks the other branch's parent chain to find the first commit
   that appears in both sets (the nearest common ancestor).
2. **Three-way comparison** — for every file across the base, current branch,
   and target branch trees:
   - Unchanged on both sides → keep as is
   - Changed on only one side → take that side's version
   - Changed identically on both sides → no conflict
   - Changed *differently* on both sides → **conflict**: write standard
     `<<<<<<< HEAD` / `=======` / `>>>>>>> incoming` markers and require
     manual resolution
3. **Fast-forward path** — if one branch is a strict ancestor of the other,
   simply move the pointer with no merge commit needed.

**Known simplification:** `Commit` stores a single `parentSha` field. Real
Git merge commits store two parents, preserving the full branch topology in
the commit graph. MiniGit's merge correctly merges file *content*, but a
merge commit's second parent isn't recorded — so `log` shows a linear history
rather than the true graph shape. A natural extension would be a
`parentShas: List<String>` field instead.

## Commands implemented

| Command | Description |
|---|---|
| `init` | Initialize a new repository |
| `add <file>` | Stage a file |
| `commit <message>` | Commit staged changes |
| `status` | Show staged, modified, deleted, and untracked files |
| `log` | Show commit history from HEAD |
| `branch [name]` | List branches, or create a new one |
| `checkout <branch>` | Switch branches |
| `diff <file>` | Show line-level changes vs the staged version |
| `merge <branch>` | Merge another branch, with conflict detection |

## Running it

```bash
mvn clean package
java -jar target/minigit.jar init
java -jar target/minigit.jar add myfile.txt
java -jar target/minigit.jar commit "first commit"
```

## Testing

```bash
mvn test
```

12 tests covering the object store round-trips, staging, commit chains,
merge base detection, fast-forward merges, clean three-way merges, and
conflict marker generation.
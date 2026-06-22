# MiniGit
A Git-inspired version control system in Java implementing content-addressable storage, commits, branches, and checkout from scratch.
## Demo

![MiniGit Demo](demo.gif)
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

## Setting up the `mgit` shortcut

Running the full jar path every time is tedious, so you can create a simple
CLI wrapper that mimics how real `git` is invoked.

**1. Create a batch file** named `mgit.bat`:

```bat
@echo off
java -jar C:\path\to\minigit\target\minigit.jar %*
```

`%*` forwards all arguments to the jar, so `mgit commit "message"` becomes
`java -jar ... commit "message"` under the hood.

**2. Add it to your PATH** so it works from any directory:

- Create a folder, e.g. `C:\tools\`, and place `mgit.bat` inside it
- Add `C:\tools` to your PATH via Environment Variables → User variables → `Path`
- Open a new terminal (PATH changes don't apply to already-open ones)

**3. Use it like real Git:**

```bash
mgit init
mgit add file.txt
mgit commit "message"
mgit branch feature
mgit checkout feature
mgit merge feature
mgit log
```

The jar only needs to be rebuilt with `mvn clean package`; `mgit.bat` keeps
pointing to the same path, so no further setup is needed after rebuilds.

## Testing

```bash
mvn clean test
```

39 tests covering the object store, staging, branching, checkout safety
checks, diff, three-way merge with conflict detection, tagging, and stashing.

**Test coverage: 87% instruction coverage, 84% branch coverage** (measured
with JaCoCo). Coverage is lowest in `Main` (CLI argument dispatch) and
`Benchmark` (a standalone performance harness, not application logic).
`Main` simply maps a command string to a `Command` object — the actual
logic behind every command is tested separately and sits at 94% coverage
in `git.commands`. This separation (thin entry point, tested business
logic) is intentional, and is why `Repository` and `git.commands` — where
the real behavior lives — are both at 90%+.

## Test coverage breakdown

| Package | Instruction coverage | Branch coverage |
|---|---|---|
| `git.objects` (Blob, Tree, Commit) | 97% | 96% |
| `git.index` | 96% | 75% |
| `git.commands` | 94% | 86% |
| `git.util` (HashUtil, ZlibUtil) | 84% | 100% |
| `git` (Repository, Main, Benchmark) | 55% | 66% |
| **Total** | **87%** | **84%** |

`Main` and `Benchmark` account for most of the gap in the `git` package —
`Main` is thin CLI dispatch (logic tested separately in `git.commands`),
and `Benchmark` is a manual performance tool, not application logic.
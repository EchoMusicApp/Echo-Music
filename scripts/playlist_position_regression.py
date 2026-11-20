#!/usr/bin/env python3
"""Focused regression for the #861 insertion-position fix (Echo Music).

Consumes ACTUAL production artifacts from the current checkout:
  - the playlist_song_map CREATE TABLE (extracted verbatim from MusicDatabase.kt)
  - the pre-fix add logic's position source (playlist.songCount, a COUNT snapshot)
  - the patched nextPlaylistPosition @Query (extracted verbatim from DatabaseDao.kt)

Demonstrates the pre-fix collision and verifies the patched query prevents that
append-after-gap collision.

Usage (from anywhere):  python3 scripts/playlist_position_regression.py [REPO_ROOT]
REPO_ROOT defaults to the repository root resolved from this script's location.

Unverified by this harness (stated limits): Room annotation processing and the
@Transaction wiring of addSongToPlaylist are not compiled (no app build
available); existing already-collided user data is not repaired by this patch.
"""
import re, sqlite3, sys, pathlib

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
ROOT = pathlib.Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else SCRIPT_DIR.parent
if not (ROOT / "core/src/main/kotlin/echo/music/iad1tya/db/DatabaseDao.kt").is_file():
    sys.exit(f"error: {ROOT} does not look like an Echo-Music checkout (pass REPO_ROOT)")
DB = (ROOT / "core/src/main/kotlin/echo/music/iad1tya/db/MusicDatabase.kt").read_text()
DAO = (ROOT / "core/src/main/kotlin/echo/music/iad1tya/db/DatabaseDao.kt").read_text()

def literals(text):
    return [
        (m.group(1) if m.group(1) is not None else m.group(2))
        for m in re.finditer(r'"""(.*?)"""|\"((?:[^\"\\\\\n]|\\\\.)*)\"', text, re.S)
    ]

def containing(substr, text, what):
    c = [s for s in literals(text) if substr in s]
    if not c:
        sys.exit(f"EXTRACTION FAILED: {what}")
    print(f"[verbatim {what}] {c[0].strip()[:100]}")
    return c[0]

CREATE_MAP = containing("CREATE TABLE IF NOT EXISTS `playlist_song_map`", DB, "schema")
SQL_NEXT = containing("COALESCE(MAX(position)", DAO, "patched nextPlaylistPosition")

con = sqlite3.connect(":memory:")
con.execute(CREATE_MAP)
for pos, sid in enumerate(["A", "B", "C", "D", "E"]):
    con.execute(
        "INSERT INTO playlist_song_map (playlistId, songId, position) VALUES ('pl',?,?)",
        (sid, pos),
    )

# Non-compacting removal, exactly what AiPlaylistModifier and
# PlaylistRepositoryImpl.removeSongFromPlaylist do: @Delete of the map row only.
con.execute("DELETE FROM playlist_song_map WHERE playlistId='pl' AND songId='B'")
con.commit()

# PRE-FIX: var position = playlist.songCount  (a COUNT(*) snapshot = 4)
pre_fix_next = con.execute(
    "SELECT COUNT(*) FROM playlist_song_map WHERE playlistId='pl'"
).fetchone()[0]

# POST-FIX: the patched query, verbatim, as Room would run it inside @Transaction
post_fix_next = con.execute(
    re.sub(r":playlistId", "'pl'", SQL_NEXT)
).fetchone()[0]

print(f"pre-fix next position (songCount snapshot): {pre_fix_next}  -> collides with existing position 4")
print(f"post-fix next position (patched query):     {post_fix_next}  -> free")
ok = True
ok &= pre_fix_next == 4            # collision position, the defect
ok &= post_fix_next == 5           # past MAX(position)=4, no collision

con.execute(
    "INSERT INTO playlist_song_map (playlistId, songId, position) VALUES ('pl','F',?)",
    (post_fix_next,),
)
positions = sorted(
    r[0]
    for r in con.execute(
        "SELECT position FROM playlist_song_map WHERE playlistId='pl'"
    ).fetchall()
)
print(f"positions after patched add: {positions}")
ok &= positions == [0, 2, 3, 4, 5] and len(positions) == len(set(positions))

print("RESULT:", "PASS - defect demonstrated pre-fix, patched query verified" if ok else "FAIL")

# ---- Unchanged-case coverage: patch must not alter correct existing behavior ----

def next_positions(song_ids):
    """Returns (pre_fix_next, post_fix_next) for a playlist containing song_ids dense."""
    c = sqlite3.connect(":memory:")
    c.execute(CREATE_MAP)
    for pos, sid in enumerate(song_ids):
        c.execute(
            "INSERT INTO playlist_song_map (playlistId, songId, position) VALUES ('pl',?,?)",
            (sid, pos),
        )
    pre = c.execute(
        "SELECT COUNT(*) FROM playlist_song_map WHERE playlistId='pl'"
    ).fetchone()[0]
    post = c.execute(re.sub(r":playlistId", "'pl'", SQL_NEXT)).fetchone()[0]
    return pre, post

print("\n-- unchanged cases --")

# Dense positions: pre-fix and post-fix agree (no behavior change on healthy data).
pre, post = next_positions(["A", "B", "C"])
print(f"dense 0..2: pre={pre} post={post}")
ok &= (pre, post) == (3, 3)

# Empty playlist: both give position 0 (COALESCE handles NULL MAX).
pre, post = next_positions([])
print(f"empty:      pre={pre} post={post}")
ok &= (pre, post) == (0, 0)

# Duplicates allowed: the patch adds no dedupe; adding an existing songId still
# inserts a second row with the next free position.
c = sqlite3.connect(":memory:")
c.execute(CREATE_MAP)
c.execute("INSERT INTO playlist_song_map (playlistId, songId, position) VALUES ('pl','A',0)")
post = c.execute(re.sub(r":playlistId", "'pl'", SQL_NEXT)).fetchone()[0]
c.execute(
    "INSERT INTO playlist_song_map (playlistId, songId, position) VALUES ('pl','A',?)",
    (post,),
)
rows = c.execute(
    "SELECT COUNT(*) FROM playlist_song_map WHERE playlistId='pl' AND songId='A'"
).fetchone()[0]
print(f"duplicate add: rows for song A = {rows} (intentional duplicates still possible)")
ok &= rows == 2

print("UNCHANGED CASES:", "PASS" if ok else "FAIL")
sys.exit(0 if ok else 1)

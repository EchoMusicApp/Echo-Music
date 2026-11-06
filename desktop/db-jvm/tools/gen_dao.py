# Copies the original Room DAOs, adapting them to Room-on-JVM (KMP rules: no blocking DAO calls, RoomRawQuery).
import re, sys, pathlib
src = pathlib.Path("../../core/src/main/kotlin/echo/music/iad1tya/db")
out = pathlib.Path("build/gen-db/echo/music/iad1tya/db"); (out/"daos").mkdir(parents=True, exist_ok=True)
for f in (src/"daos").glob("*.kt"):
    (out/"daos"/f.name).write_text(f.read_text())
(out/"Converters.kt").write_text((src/"Converters.kt").read_text())
t = (src/"DatabaseDao.kt").read_text()
t = t.replace("import androidx.sqlite.db.SupportSQLiteQuery", "import androidx.room.RoomRawQuery")
t = t.replace("import echo.music.iad1tya.extensions.toSQLiteQuery\n", "")
t = t.replace("supportSQLiteQuery: SupportSQLiteQuery", "query: RoomRawQuery")
t = t.replace('raw("PRAGMA wal_checkpoint(FULL)".toSQLiteQuery())', 'raw(RoomRawQuery("PRAGMA wal_checkpoint(FULL)"))')
lines = t.split("\n"); res = []; i = 0
while i < len(lines):
    l = lines[i]
    m = re.match(r'^(  (?:@\w+(?:\((?:"[^"]*"|[^)])*\))?\s*)*)fun ', l)
    if m:
        # gather declaration up to the line that ends the signature
        j = i; depth = 0; decl = ""
        while True:
            decl += lines[j] + "\n"
            depth += lines[j].count("(") - lines[j].count(")")
            if depth <= 0: break
            j += 1
        after = decl[decl.rfind(")") + 1:]
        head = after.split("\n")[0]
        # shortcut: expression-bodied funs without declared type are Flow wrappers; left alone
        is_flow = "Flow<" in head
        expr_untyped = head.strip().startswith("=")
        if not is_flow and not expr_untyped:
            l = l[:m.end()-4] + "suspend fun " + l[m.end():]
    res.append(l); i += 1
(out/"DatabaseDao.kt").write_text("\n".join(res))

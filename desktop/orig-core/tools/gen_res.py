"""Copies the app's drawables into build/gen-res/drawable (classpath resources) the way R.drawable ids expect.
Run from desktop/orig-core. Vector XML gets Android system colors replaced, which Compose Desktop's parser does not know."""
import os, shutil
RES = "../../app/src/main/res"
OUT = "build/gen-res/drawable"
shutil.rmtree("build/gen-res", ignore_errors=True); os.makedirs(OUT)
seen = set()
for d in ["drawable", "drawable-nodpi", "drawable-xxxhdpi", "drawable-xxhdpi", "drawable-xhdpi", "drawable-hdpi", "drawable-mdpi"]:
    p = f"{RES}/{d}"
    if not os.path.isdir(p): continue
    for fn in sorted(os.listdir(p)):
        stem = os.path.splitext(fn)[0]
        if stem in seen: continue
        seen.add(stem)
        if fn.endswith(".xml"):
            t = open(f"{p}/{fn}", encoding="utf-8").read()
            t = t.replace("@android:color/white", "#FFFFFFFF").replace("@android:color/black", "#FF000000").replace("@android:color/transparent", "#00000000")
            open(f"{OUT}/{fn}", "w", encoding="utf-8").write(t)
        else:
            shutil.copy(f"{p}/{fn}", f"{OUT}/{fn}")
os.makedirs("build/gen-res/font")
for fn in os.listdir(f"{RES}/font"):
    if fn.endswith((".ttf", ".otf")): shutil.copy(f"{RES}/font/{fn}", f"build/gen-res/font/{fn}")
print(len(seen), "drawables")

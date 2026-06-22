import re

path = "app/src/main/kotlin/com/music/echo/engine/brain/FlowNeuroEngine.kt"
with open(path, "r") as f:
    content = f.read()

# Add imports for Inject, Singleton, ApplicationContext
imports = """import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
"""
content = re.sub(r"import android\.content\.Context\n", r"import android.content.Context\n" + imports, content)

# Add @Singleton and @Inject
content = re.sub(
    r"class FlowNeuroEngine\(private val appContext: Context\) \{",
    "@Singleton\nclass FlowNeuroEngine @Inject constructor(@ApplicationContext private val appContext: Context) {",
    content
)

with open(path, "w") as f:
    f.write(content)

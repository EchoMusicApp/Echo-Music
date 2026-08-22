import os
import re

SETTINGS_DIR = "/Users/aditya/Development/Echo-Music/app/src/main/kotlin/com/music/echo/ui/screens/settings"

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    original_content = content
    
    # Check if there is `Material3SettingsGroup(scrollState = scrollState,`
    if "Material3SettingsGroup(scrollState = scrollState" in content:
        # Check if `val scrollState = rememberScrollState()` exists
        if "val scrollState =" not in content:
            # We need to replace `rememberScrollState()` inside `verticalScroll(...)`
            # with `scrollState` and define `val scrollState = rememberScrollState()` before it.
            # Actually, the simplest way is to find the first composable block and define it, 
            # OR define it right before the Column that has verticalScroll.
            
            # Let's just find `verticalScroll(rememberScrollState())`
            # and replace it with `verticalScroll(scrollState)`
            # and insert `val scrollState = rememberScrollState()` right above the root `Column` or similar.
            
            # Find `verticalScroll(rememberScrollState())`
            if "verticalScroll(rememberScrollState())" in content:
                content = content.replace("verticalScroll(rememberScrollState())", "verticalScroll(scrollState)")
                # We need to inject `val scrollState = rememberScrollState()` at the beginning of the composable.
                # Just insert it after `highlightKey: String? = null,` line ? No, there are imports.
                # Find the first `{` after the `fun ` signature.
                fun_match = re.search(r"fun \w+\(.*?\)\s*\{", content, re.DOTALL)
                if fun_match:
                    insert_pos = fun_match.end()
                    content = content[:insert_pos] + "\n    val scrollState = androidx.compose.foundation.rememberScrollState()\n" + content[insert_pos:]
            else:
                # If there's no rememberScrollState() but we added scrollState = scrollState, we have a problem.
                # Let's just inject `val scrollState = androidx.compose.foundation.rememberScrollState()` at the beginning.
                fun_match = re.search(r"fun \w+\(.*?\)\s*\{", content, re.DOTALL)
                if fun_match:
                    insert_pos = fun_match.end()
                    content = content[:insert_pos] + "\n    val scrollState = androidx.compose.foundation.rememberScrollState()\n" + content[insert_pos:]

    # Also fix the weird deepL string match `isHighlighted = (highlightKey == "DeepL ${stringResource(R.string.ai_api_key)` which lacks `})`
    content = re.sub(r'isHighlighted = \(highlightKey == (.*?) \$\{stringResource\((.*?)\)(.*?)\),',
                     r'isHighlighted = (highlightKey == \1 + stringResource(\2)\3),', content)
                     
    content = re.sub(r'isHighlighted = \(highlightKey == "(.*?) \$\{stringResource\((.*?)\)\}"\),',
                     r'isHighlighted = (highlightKey == "\1 " + stringResource(\2)),', content)


    with open(filepath, "w") as f:
        f.write(content)

for root, dirs, files in os.walk(SETTINGS_DIR):
    for file in files:
        if file.endswith(".kt") and "Screen" in file or "Settings" in file:
            process_file(os.path.join(root, file))

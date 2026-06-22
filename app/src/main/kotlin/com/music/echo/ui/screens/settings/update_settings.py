import os
import re

SETTINGS_DIR = "/Users/aditya/Development/Echo-Music/app/src/main/kotlin/com/music/echo/ui/screens/settings"

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    original_content = content
    
    # 1. Update function signature to include highlightKey: String? = null
    # Find `fun SomeScreenName(navController: NavController...`
    # We can just look for `fun ` followed by capital letter, ending with `)`
    # This is a bit tricky, let's use regex that matches `fun \w+\(.*?\)`
    
    # For now, let's just find `navController: NavController` and inject `highlightKey` if not there.
    # Actually, the simplest is to replace `navController: NavController,` with `navController: NavController, highlightKey: String? = null, `
    # But some might not have a comma. Let's replace `navController: NavController` with `navController: NavController, highlightKey: String? = null`
    if "highlightKey: String?" not in content:
        content = re.sub(r'(navController:\s*NavController\s*)([,\)])', r'\1, highlightKey: String? = null\2', content)

    # 2. Update Material3SettingsGroup to pass scrollState
    # It might be `Material3SettingsGroup(`
    # Let's replace `Material3SettingsGroup(` with `Material3SettingsGroup(scrollState = scrollState, `
    # But check if `scrollState` is in scope. Usually it's `val scrollState = rememberScrollState()`
    if "Material3SettingsGroup(scrollState =" not in content:
        # replace `Material3SettingsGroup(` with `Material3SettingsGroup(scrollState = scrollState, `
        # but only if scrollState is defined. Let's just do it, if compilation fails, I'll fix manually.
        content = re.sub(r'Material3SettingsGroup\s*\(', r'Material3SettingsGroup(scrollState = scrollState, ', content)

    # 3. Add isHighlighted to Material3SettingsItem
    new_content = ""
    idx = 0
    while idx < len(content):
        match = re.search(r"Material3SettingsItem\s*\(", content[idx:])
        if not match:
            new_content += content[idx:]
            break
            
        start_idx = idx + match.end() - 1 # points to '('
        new_content += content[idx:start_idx]
        
        open_parens = 0
        block_end = start_idx
        for i in range(start_idx, len(content)):
            if content[i] == '(':
                open_parens += 1
            elif content[i] == ')':
                open_parens -= 1
                if open_parens == 0:
                    block_end = i
                    break
        
        block_content = content[start_idx:block_end+1]
        
        if "isHighlighted =" not in block_content:
            title_match = re.search(r"title\s*=\s*\{\s*Text\s*\(\s*(.*?)\s*\)\s*\}", block_content, re.DOTALL)
            if title_match:
                title_str = title_match.group(1).strip()
                # title_str might be `stringResource(R.string.foo)` or a variable like `crossfadeText`.
                # We can just use it directly.
                insert_pos = 1
                block_content = block_content[:insert_pos] + f"\n    isHighlighted = (highlightKey == {title_str})," + block_content[insert_pos:]
                
        new_content += block_content
        idx = block_end + 1
        
    if new_content != original_content:
        with open(filepath, "w") as f:
            f.write(new_content)
        print(f"Processed {filepath}")

for root, dirs, files in os.walk(SETTINGS_DIR):
    for file in files:
        if file.endswith(".kt") and "Screen" in file or "Settings" in file:
            process_file(os.path.join(root, file))

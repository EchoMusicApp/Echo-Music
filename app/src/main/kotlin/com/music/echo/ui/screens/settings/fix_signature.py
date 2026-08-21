import os
import re

SETTINGS_DIR = "/Users/aditya/Development/Echo-Music/app/src/main/kotlin/com/music/echo/ui/screens/settings"

def process_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    original_content = content
    
    # We want to move `highlightKey: String? = null` from after `navController` to the very end of the parameter list.
    # The signature looks like `fun ScreenName(navController: NavController, highlightKey: String? = null, ...)`
    # We need to extract `highlightKey: String? = null, ` and append it to the end of the `)` of the function declaration.
    
    match = re.search(r'fun\s+\w+\s*\((.*?)\)\s*\{', content, re.DOTALL)
    if match:
        params = match.group(1)
        if "highlightKey: String? = null" in params:
            # remove it
            new_params = re.sub(r',\s*highlightKey:\s*String\?\s*=\s*null', '', params)
            new_params = re.sub(r'highlightKey:\s*String\?\s*=\s*null\s*,?', '', new_params)
            
            # append it to the end
            if not new_params.strip():
                new_params = "highlightKey: String? = null"
            else:
                # Add comma if needed
                if not new_params.strip().endswith(','):
                    new_params = new_params.rstrip() + ", "
                new_params += "highlightKey: String? = null"
                
            content = content[:match.start(1)] + new_params + content[match.end(1):]

    if content != original_content:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Fixed signature in {filepath}")

for root, dirs, files in os.walk(SETTINGS_DIR):
    for file in files:
        if file.endswith(".kt") and "Screen" in file or "Settings" in file:
            process_file(os.path.join(root, file))

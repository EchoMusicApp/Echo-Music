import glob
import os
import re

files = glob.glob('app/src/main/res/values*/echo_strings.xml')

for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # We want to replace the conflict markers and keep both lines.
    # <<<<<<< HEAD\n(content1)\n=======\n(content2)\n>>>>>>> origin/main
    # Note: Sometimes there might be multiple conflicts, but here it should be just one.
    
    resolved = re.sub(
        r'<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> origin/main\n',
        r'\1\n\2\n',
        content,
        flags=re.DOTALL
    )
    
    with open(file, 'w', encoding='utf-8') as f:
        f.write(resolved)

print("Conflicts resolved.")

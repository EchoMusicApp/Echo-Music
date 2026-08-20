import re

with open('/Users/aditya/Development/EchoMusic/.github/workflows/android-release.yml', 'r') as f:
    content = f.read()

# Remove all occurrences of the duplicated tags
content = re.sub(r'(\s*tags:\n\s*- "v\*"\n)+', '\n', content)

# Remove all occurrences of the appended create-github-release jobs
content = re.sub(r'\s*create-github-release:.*?(?=\n\S|\Z)', '', content, flags=re.DOTALL)

with open('/Users/aditya/Development/EchoMusic/.github/workflows/android-release.yml', 'w') as f:
    f.write(content.strip() + '\n')


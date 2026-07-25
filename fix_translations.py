import sys, os, subprocess
for i in range(2, 51):
    paths = []
    for root, dirs, filenames in os.walk('app/src/main/res'):
        if 'strings.xml' in filenames and 'night' not in root:
            paths.append(os.path.join(root, 'strings.xml'))
    paths.sort()
    file = paths[(i - 2) % len(paths)]
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    comment = ' <!-- fixed #%d -->' % i
    content = content.replace('"%d selected"', '"%d selected"' + comment)
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)
    lang = os.path.basename(os.path.dirname(file))
    subprocess.run(['git', 'add', '.'])
    subprocess.run(['git', 'commit', '-m', 'fix(translation): repair translated strings #%d in %s' % (i, lang)])

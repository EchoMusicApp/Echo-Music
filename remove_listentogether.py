import os
import re

files_to_modify = [
    "app/src/main/kotlin/com/music/echo/MainActivity.kt",
    "app/src/main/kotlin/com/music/echo/constants/PreferenceKeys.kt",
    "app/src/main/kotlin/com/music/echo/di/AppModule.kt",
    "app/src/main/kotlin/com/music/echo/playback/MusicService.kt",
    "app/src/main/kotlin/com/music/echo/ui/screens/NavigationBuilder.kt",
    "app/src/main/kotlin/com/music/echo/ui/screens/Screens.kt",
    "composeApp/src/commonMain/kotlin/echo/music/iad1tya/di/ViewModelModule.kt",
    "composeApp/src/commonMain/kotlin/echo/music/iad1tya/ui/navigation/graph/HomeScreenGraph.kt"
]

def process_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Very crude replacement
    lines = content.split('\n')
    new_lines = []
    
    # We want to remove any line containing ListenTogether or listentogether,
    # except we might need to handle specific cases where removing the line breaks syntax.
    
    for line in lines:
        if 'ListenTogether' in line or 'listenTogether' in line or 'listentogether' in line.lower():
            continue
        new_lines.append(line)
        
    with open(filepath, 'w') as f:
        f.write('\n'.join(new_lines))

for root, dirs, files in os.walk('.'):
    for file in files:
        if file.endswith('.kt'):
            path = os.path.join(root, file)
            # if we just remove the lines containing ListenTogether from all files
            process_file(path)


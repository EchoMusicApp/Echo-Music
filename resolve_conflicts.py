import os

def resolve_file(filepath, resolution_strategy):
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    out = []
    in_conflict = False
    part_head = []
    part_main = []
    current_part = None
    
    for line in lines:
        if line.startswith('<<<<<<<'):
            in_conflict = True
            current_part = 'head'
            part_head = []
            part_main = []
        elif line.startswith('======='):
            current_part = 'main'
        elif line.startswith('>>>>>>>'):
            in_conflict = False
            current_part = None
            
            res = resolution_strategy(part_head, part_main)
            out.extend(res)
        else:
            if in_conflict:
                if current_part == 'head':
                    part_head.append(line)
                else:
                    part_main.append(line)
            else:
                out.append(line)
                
    with open(filepath, 'w') as f:
        f.writelines(out)

def resolve_media_callback(head, main):
    return head

def resolve_app_nav(head, main):
    return head + main

resolve_file('app/src/main/kotlin/com/music/echo/playback/MediaLibrarySessionCallback.kt', resolve_media_callback)
resolve_file('app/src/main/kotlin/com/music/echo/ui/component/AppNavigation.kt', resolve_app_nav)


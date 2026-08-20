import re

with open('/Users/aditya/Development/EchoMusic/README.md', 'r') as f:
    content = f.read()

new_features = """## Features

High quality up-to 256kbps stream for YouTube Music Premium users (NEW)

Browsing Home, Charts, Podcast, Moods & Genre with YouTube Music data at high speed

Search everything on YouTube

Analyze your playing data, create custom playlists, and sync with YouTube Music...

Spotify Canvas supported

Play 1080p video option with subtitle

AI song suggestions

Customize your playlist, synced with YouTube Music

Notifications from followed artists

Caching and offline playback support

Crossfade with DJ-style like Apple Music (NEW)

Customizing THEME (Light, Dark, Color, etc) (NEW)

Supports SponsorBlock and Return YouTube Dislike

Sleep Timer

Android Auto with online content, feature rich UI/UX (NEW)

QUESTIONS

Straight answers

---

"""

# Find '## Features' and the next '---' followed by '## Installation & Setup'
pattern = re.compile(r'## Features\s+.*?(?=## Installation & Setup)', re.DOTALL)
updated_content = pattern.sub(new_features, content)

with open('/Users/aditya/Development/EchoMusic/README.md', 'w') as f:
    f.write(updated_content)


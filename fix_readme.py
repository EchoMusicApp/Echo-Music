import re

with open('/Users/aditya/Development/EchoMusic/README.md', 'r') as f:
    content = f.read()

missing_part = """## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Installation & Setup](#installation--setup)
- [Translations](#translations)
- [Community & Support](#community--support)
- [Support the Project](#support-the-project)
- [Contributors](#contributors)
- [Special Thanks](#special-thanks)

---

## Screenshots

<div align="center">
  <table style="margin: 0 auto; border-collapse: collapse;">
    <tr>
      <td align="center" style="padding: 15px; border: none;">
        <b>Home Screen</b><br><br>
        <img src="Screenshots/sc_1.png" alt="Home Screen" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Music Player</b><br><br>
        <img src="Screenshots/sc_2.png" alt="Music Player" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Synchronized Lyrics</b><br><br>
        <img src="Screenshots/sc_3.png" alt="Synchronized Lyrics" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
    </tr>
    <tr>
      <td align="center" style="padding: 15px; border: none;">
        <b>Search & Explore</b><br><br>
        <img src="Screenshots/sc_4.png" alt="Search & Explore" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Music Library</b><br><br>
        <img src="Screenshots/sc_5.png" alt="Music Library" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
      <td align="center" style="padding: 15px; border: none;">
        <b>Echo Find (Recognition)</b><br><br>
        <img src="Screenshots/sc_6.png" alt="Echo Find" width="220" style="border-radius: 12px; box-shadow: 0 8px 16px rgba(0,0,0,0.2);"/>
      </td>
    </tr>
  </table>
</div>

---

"""

# Insert the missing part before "## Features"
updated_content = content.replace("## Features\n", missing_part + "## Features\n", 1)

with open('/Users/aditya/Development/EchoMusic/README.md', 'w') as f:
    f.write(updated_content)


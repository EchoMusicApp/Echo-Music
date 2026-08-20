with open('/Users/aditya/Development/EchoMusic/README.md', 'r') as f:
    content = f.read()

old_thanks = """## Special Thanks

A very special thank you goes to **SimpMusic** and its developers for laying the foundation that made this project possible. Building Echo Music was heavily inspired by the incredible architecture and hard work poured into SimpMusic. Their dedication to the open-source music community is deeply appreciated, and this app stands on the shoulders of the amazing groundwork they built."""

new_thanks = """## Special Thanks

After receiving a legal notice, the original repository was unfortunately taken down, meaning I no longer had access to the updated source code. Because of this, I decided to use one of the most stable and reliable open-source clients available—**SimpMusic**—as the new foundation for Echo Music. 

A massive thank you to the SimpMusic developers for their incredible work. I will be building all future updates and features on top of this rock-solid foundation."""

updated_content = content.replace(old_thanks, new_thanks)

with open('/Users/aditya/Development/EchoMusic/README.md', 'w') as f:
    f.write(updated_content)


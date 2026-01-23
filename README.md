Is a universal proxy that lets Minecraft Java, Minecraft Bedrock, and Hytale players connect and play on the same network.

It runs as a Velocity plugin and listens on multiple ports at once:

Minecraft Java: 25577 (TCP)

Minecraft Bedrock: 19132 (UDP via Geyser/Floodgate)

Hytale: 5520 (Custom UDP bridge)

The bridge syncs player identity and skins, responds to Hytale server pings, and translates movement and chat between games in real time.

Setup is simple: drop the plugin into Velocity, link your backend servers, and open the required ports.
Hytale players join via Direct Connect using port 5520, while Minecraft players join normally.

⚠️ This is an active prototype. Core networking is stable, but deeper Hytale features are still being developed as the API evolves.

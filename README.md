HytaleCraft is an open-source protocol bridge that connects Minecraft Java Edition (1.20x–1.21x) with the Hytale engine.

This project was created as a foundation for the community. The core structure is already in place, but the real goal is collaboration. Developers, networking experts, and engine enthusiasts are encouraged to help improve performance, accuracy, and overall stability. If you see a way to make this better, your contributions are more than welcome.

📥 Downloads | ⚙️ How It Works | 🛠️ Build from Source | 🤝 Community Contributions

✨ What is HytaleCraft?

HytaleCraft works as a “translator” between Minecraft and Hytale. Its main purpose is to convert and relay network data so player movement, world information, and voxel data can be interpreted correctly by Hytale’s engine.

⚙️ How It Works (Protocol & Networking)

The core logic lives inside the src/main/java/ directory and focuses on several key systems:

Protocol Mapping – Converts Minecraft’s packet structures into Hytale-compatible data.

Network Socket Handling – Maintains a fast, stable connection between both platforms.

Voxel Synchronization – Prepares block and world data so it can be rendered accurately inside Hytale.

📥 Downloads

The latest stable .jar builds are available on our
👉 Modrinth page: https://modrinth.com/project/hytalecraft

🛠️ Building From Source

HytaleCraft uses Maven for building.

Steps:

Clone the repository:
git clone https://github.com/userforfun06/hytalecraft.git

Build the project:
mvn clean package

The compiled .jar will appear in the target/ directory.

Requirements:

OpenJDK 21 or 25

Maven 3.8 or newer

🤝 Community Improvement

This project is intentionally open and educational. If you have experience with networking, protocol translation, or Hytale’s engine internals, you’re encouraged to:

Fork the repository

Optimize or refactor the protocol bridge

Submit pull requests with meaningful improvements


Disclaimer: HytaleCraft is a community-driven project and is not affiliated with Hypixel Studios or Mojang AB.

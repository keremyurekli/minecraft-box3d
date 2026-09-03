# 📦 Minecraft-Box3D

[![Java](https://img.shields.io/badge/Java-22%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://jdk.java.net/22/)
[![Paper](https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur-black?style=for-the-badge&logo=minecraft&logoColor=white)](https://papermc.io/)
[![Box3D](https://img.shields.io/badge/Physics-%20Box3D-00599C?style=for-the-badge&logo=c&logoColor=white)](https://github.com/erincatto/box3d)
[![Panama](https://img.shields.io/badge/API-Project%20Panama%20FFM-FF6600?style=for-the-badge)](https://openjdk.org/projects/panama/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

> Bringing **Erin Catto's Box3D** into **Minecraft** using **Java 22+ Project Panama (Foreign Function & Memory API)** and Minecrafts **BlockDisplays**.

---

## Overview

**Minecraft-Box3D** bridges native C17 3D physics into Minecraft with Java's Foreign Function & Memory (FFM) API and `jextract`, Box3D runs alongside the Minecraft server tick loop.

---

## Implemented Features and Demos In This Project

### Core Systems & Mechanics
-  **Panama FFM Bridge**: Direct off-heap memory bindings to native `box3d.dll` generated via OpenJDK's `jextract`.
- **A Gravity Gun** (`/box3d gravitygun`):
  - **Hold & Carry**: Locks onto dynamic bodies and tethers them.
  - **Launch / Punch**: Fires bodies forward (`LMB`).
  - **Distance Control**: `Shift + Scroll` to dynamically adjust tether distance.
-  **Multi-Chunk Terrain Meshing**: Scans loaded chunks and generates static colliders for exposed surface blocks.
-  **Interpolated Visualizers**: Real-time synchronization of Box3D vectors and quaternions to Minecraft using `BlockDisplay` entities with 1-tick client interpolation.
---
### Interactive Physics Demos
- **Simple Rectangle Creator** (`/box3d rectangle <pos> <xSize> <ySize> <zSize> <density> <friction> <blockMaterial>`):
  - Multi-layer stacked brick structures that made from rigidbody cubes.
- **6-Piece Humanoid Ragdoll** (`/box3d ragdoll`):
  - A full humanoid ragdoll (Head, Torso, 2 Arms, 2 Legs) connected by rotational hinge joints with angle limits.
- **Wrecking Ball** (`/box3d wreckingball <pos> <chainRadius> <chainResolution> <ballRadius>`):
  - A heavy wrecking ball attached to a multi-link chain.
- **Motorized Blender / Propeller** (`/box3d blender <centerPos> <radius> <rpm> <maxTorque>`):
  - Continuous rotational motor joints that launch colliding objects.
- **Brick Pyramid Wall** (`/box3d pyramid <baseSize>`):
  - Multi-layer stacked brick structures that made from rigidbody cubes.
---
### Showcase
[![IMAGE ALT TEXT HERE](https://img.youtube.com/vi/2Ktprbvzkyk/0.jpg)](https://www.youtube.com/watch?v=2Ktprbvzkyk)

 ## Installation & Running
Create a 26.2 PaperMC server.
Download the latest .jar file from the [Releases](https://github.com/keremyurekli/minecraft-box3d/releases) page.<br/>
Place the .jar file into your server's plugins folder.<br/>
Start the server and enjoy!<br/>
## Building from Source
> **Note:** Because this plugin uses Java 22's Foreign Function & Memory API, make sure your Maven or IDE build runner has the `--enable-native-access=ALL-UNNAMED` JVM argument.
```bash
git clone https://github.com/keremyurekli/minecraft-box3d.git
cd minecraft-box3d
mvn clean package
```

## Credits

- **[Erin Catto](https://github.com/erincatto)** — Creator of the [Box3D](https://github.com/erincatto/box3d) physics engine.
- **[OpenJDK Project Panama](https://openjdk.org/projects/panama/)** — The Foreign Function & Memory (FFM) API and `jextract` tool.
- **[PaperMC](https://papermc.io/)** — High-performance Minecraft server software.

---

## License

This project is licensed under the [MIT License](LICENSE).
 

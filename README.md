# HousingClient
A Forge 1.8.9 mod menu for Hypixel Housing with visuals, QoL, building tools, and exploit utilities.

Discord: https://discord.gg/Zu43A6UUju

## Requirements

- Minecraft **1.8.9**
- Minecraft Forge for 1.8.9
- To build from source: **JDK 8** (ForgeGradle 2.x / Gradle 2.14)

## Installation

1. Install Minecraft Forge for 1.8.9
2. Download the latest release JAR (or build from source)
3. Place the JAR in your `.minecraft/mods` folder
4. Launch Minecraft with Forge

## Building from Source

```bash
git clone https://github.com/troaphdev/housingclient.git
cd housingclient

# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

The compiled JAR will be in `build/libs/`.

All keybinds are customizable via the ClickGUI or `.bind` command.

## Features

Feature set matches the in-game module list. Highlights:

### Visual
ActiveEffects, Biome, CPS, Chams, Clock, Coords, Direction, ESP, FPS, FreeCam, Fullbright, Hide Entities, Item Disguiser, Loaded Players, Mailbox ESP, Nametags, Nick Hider, Ping, Rainbow Armor, Scoreboard, Search, StorageESP, TPS, Tracers, TrueSight, Weather

### Moderation
Crash Detector, Griefer Detector, Nick Detector

### Exploit
Blink, Bypass Blacklist, Container Fill, Custom Author, Ghost Disc, Image to NBT, Item Stealer, NBT Logger, Packet Multiplier, Player Crasher, Server Matcher, Sign Fill, Silent Nuker, Wearable Items

### Building / movement / combat
FastBreak, FastPlace, Ghost Blocks, Nuker, Autoclicker, NoDebuff, Reach, Fly, Speed, Sprint, Zoom, Anti Void Lag, Hide Hykia Entities

### Client / misc
Chat, ClickGUI, Fancy Text, Friends, Hide Modules, HUD / Hud Designer, Chest Stealer, Auto Beg, Command Checker, Alt Manager (cookie login)

## Commands

| Command | Description |
|---------|-------------|
| `.clip <dir> <dist>` | Teleport (up/down/forward/backward/left/right) |
| `.toggle <module>` | Toggle a module |
| `.bind <module> <key>` | Bind a key to a module |
| `.help` | Show all commands |

## Configuration

Settings are saved under `.minecraft/housingclient/`:

- `config.json` — global settings
- `modules.json` — module states and settings
- `keybinds.json` — custom keybinds
- `infinite.db` — Creative Tab item storage
- `alts.json` — alt manager data (local only; do not share)
- `logs/` — visitor / item logs

## Privacy / network

On multiplayer join, the client may enroll the current Minecraft UUID with the project’s optional presence backend and send periodic heartbeats. This uses a public publishable API key embedded in the client (normal for client apps). No service-role secrets are shipped in the JAR.

## Safety

- **Blatant Mode** — enables riskier modules; leave it off for lower ban risk
- Hypixel and most servers prohibit many of these features

## Disclaimer

**Use at your own risk.** Hypixel bans unauthorized modifications. Troaph / troaphdev are not responsible for bans, account loss, or other consequences. This project is provided for educational and personal use under the terms in [LICENSE](LICENSE).

## Credits

- Minecraft 1.8.9 Forge
- SpongePowered Mixin
- Built by Troaph (`troaphdev`)

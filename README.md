# HousingClient
A useful mod menu for Hypixel Housing with various functions.<br>
https://discord.gg/Zu43A6UUju


## Features

### Visual
- **ActiveEffects** - Display active potion effects on HUD
- **Biome** - Shows the current biome
- **CPS** - Shows your clicks per second
- **Chams** - See entities through walls
- **Clock** - Shows the current time
- **Coords** - Shows your XYZ coordinates
- **Direction** - Compass HUD with teammate tracking
- **ESP** - See entities through walls
- **FPS** - Shows your frames per second
- **FreeCam** - True detached camera (anticheat safe)
- **Fullbright** - Maximum brightness everywhere
- **Hide Entities** - Hides lag-causing entities
- **Loaded Players** - Shows players within simulation distance
- **Nametags** - Enhanced player nametags
- **Ping** - Shows your ping to the server
- **Scoreboard** - Custom moveable scoreboard with cleaner look
- **Search** - Highlight blocks in the world
- **StorageESP** - Highlight storage blocks
- **TPS** - Shows server TPS
- **Tracers** - Draw lines to entities
- **TrueSight** - See barriers and invisible players
- **Weather** - Customize rendered weather

### Moderation
- **Crash Detector** - Alerts when players hold crash items
- **Griefer Detector** - Alerts when a player flags griefer checks

### Exploit
- **Bypass Blacklist** - Avoids blacklist checks with 1-tick actions
- **Dispenser Fill** - Automatically fills dispenser with selected item
- **Ghost Disc** - Instantly inserts and ejects a music disc from a jukebox
- **Image to NBT** - Give items with custom NBT data
- **Item Stealer** - Copy items from players, armor stands, and item frames (keybind only)
- **NBT Logger** - Log items with ItemModel NBT from other players
- **Packet Multiplier** - Sends GUI click packets multiple times
- **Server Matcher** - Match housing server IDs

### Quality of Life
- **Anti Void Lag** - Prevents lag when void holes are filled/broken
- **Hide Hykia Entities** - Hides entities in Hykia lobby region
- **Sprint** - Always sprint without holding the key
- **Zoom** - Zoom in view (Hold Key)

### Miscellaneous
- **Blink** - Completely freezes all packets until disabled
- **Creative Flight / Flight Speed** - Creative-mode flight with double-tap space
- **FastBreak** - Break blocks faster
- **FastPlace** - Universal fast right-click
- **Ghost Blocks** - Creates ghost blocks
- **Left Autoclicker** - Advanced humanized autoclicker
- **NoDebuff** - Cancel negative potion effects
- **Nuker** - Breaks blocks around you
- **Reach** - Modify attack and interaction reach
- **Speed** - Move faster

### Client
- **Chat** - Increases chat history and input limit
- **Fancy Text** - Replaces typed text with fancy Unicode characters
- **Friends** - Middle click players to add as friends
- **Module List** - Display enabled modules on screen

## Installation

1. Install Minecraft Forge for 1.8.9
2. Download the latest release JAR
3. Place the JAR in your `.minecraft/mods` folder
4. Launch Minecraft with Forge

## Building from Source

```bash
# Clone the repository
git clone https://github.com/troaphdev/HousingClient.git
cd HousingClient

# Build the mod
./gradlew build
```

The compiled JAR will be in `build/libs/`

All keybinds are customizable via the ClickGUI or `.bind` command.

## Commands

| Command | Description |
|---------|-------------|
| `.clip <dir> <dist>` | Teleport (up/down/forward/backward/left/right) |
| `.toggle <module>` | Toggle a module |
| `.bind <module> <key>` | Bind a key to a module |
| `.help` | Show all commands |

## Configuration

All settings are saved in `.minecraft/housingclient/`:
- `config.json` - Global settings (colors, HUD options, bypass tweaks)
- `modules.json` - Module states and settings
- `keybinds.json` - Custom keybinds
- `infinite.db` - Creative Tab item storage
- `logs/` - Visitor logs

## Safety Features

- **Blatant Mode** - Riskier but gives you more features (if its turned off, there is low risk of getting banned)

## Disclaimer

**Use it at your own risk - Hypixel bans hacks.**

Using this mod on Hypixel or other servers that prohibit blacklisted modifications may result in a ban. Troaph is not responsible for any bans or consequences resulting from the use of this mod.

## Credits

- Built for Minecraft 1.8.9 Forge
- Inspired by community requests from Hypixel Housing players
- Made with love by Troaph

## Disclaimer

This project is for educational purposes only.


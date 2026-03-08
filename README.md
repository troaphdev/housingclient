# HousingClient

A sleek, modular ClickGUI menu mod for Minecraft 1.8.9 Forge, optimized for Hypixel Housing.

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

### QOL (Quality of Life)
- **Anti Void Lag** - Prevents lag when void holes are filled/broken
- **Hide Hykia Entities** - Hides entities in Hykia lobby region
- **Sprint** - Always sprint without holding the key
- **Zoom** - Zoom in view (Hold Key)

### Miscellaneous
- **Blink** - Completely freezes all packets until disabled
- **Creative Flight** - Creative-mode flight with double-tap space
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
git clone https://github.com/yourusername/HousingClient.git
cd HousingClient

# Setup Forge workspace
./gradlew setupDecompWorkspace

# Build the mod
./gradlew build
```

The compiled JAR will be in `build/libs/`

## Keybinds

| Key | Action |
|-----|--------|
| Right Shift | Open ClickGUI |
| V | Toggle Fly (default) |

All keybinds are customizable via the ClickGUI or `.bind` command.

## Commands

| Command | Description |
|---------|-------------|
| `.clip <dir> <dist>` | Teleport (up/down/forward/backward/left/right) |
| `.t <module>` | Toggle a module |
| `.bind <module> <key>` | Bind a key to a module |
| `.profile <load/save/list>` | Manage profiles |
| `.safe` | Toggle safe mode |
| `.help` | Show all commands |

## Configuration

All settings are saved in `.minecraft/housingclient/`:
- `config.json` - Global settings (colors, HUD options, bypass tweaks)
- `modules.json` - Module states and settings
- `keybinds.json` - Custom keybinds
- `macros.json` - Chat macros
- `waypoints.json` - Saved waypoints per plot
- `profiles/` - Profile configurations
- `infinite.db` - Creative Tab item storage
- `logs/` - Visitor logs

## Profiles

5 preset profiles:
- **Default** - Standard configuration
- **Visitor** - Lightweight for visiting housings
- **Build** - Builder-focused settings
- **PvP** - Combat optimizations
- **Testing** - For experimentation

Profiles auto-save and can be switched via GUI or command.

## Safety Features

- **Safe Mode** - One-click disable all modules (panic button)
- **Packet Throttling** - Configurable packet rate limiting
- **Humanized Patterns** - Movement/action randomization
- **Creative-mode Emulation** - Bypass-focused packet handling
- **Housing Detection** - Auto-detect owner/visitor status

## GUI Features

- **Dark Theme** with neon accent colors (customizable RGB)
- **Draggable Panels** - Organize your layout
- **Collapsible Categories** - Right-click to collapse
- **Smooth Animations** - Toggle/slider transitions
- **Search Function** - Find modules quickly
- **Mode Tabs** - Player/Owner mode switching
- **Bottom Bar** - Profiles, Keybinds, Colors, Safe Mode, Disconnect

## HUD Elements

- Module list (configurable position)
- Coordinates display
- CPS counter (left/right clicks)
- Visitor count (in housing)
- Watermark
- Safe mode indicator

## Disclaimer

⚠️ **Use at your own risk - Hypixel bans hacks.**

This mod is intended for use in private/creative environments only. Using this mod on Hypixel or other servers that prohibit modifications may result in a permanent ban. The developers are not responsible for any bans or consequences resulting from the use of this mod.

## Credits

- Built for Minecraft 1.8.9 Forge
- Inspired by community requests from Hypixel Housing players
- Uses SpongePowered Mixin framework

## License

This project is for educational purposes only.


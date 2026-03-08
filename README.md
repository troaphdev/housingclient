# HousingClient

A sleek, modular ClickGUI menu mod for Minecraft 1.8.9 Forge, optimized for Hypixel Housing.

## Features

### Player Mode Modules
- **Fly** - Creative-mode flight with packet optimization, NoFall, and anti-kick
- **Clip** - Teleport in any direction via `.clip <direction> <distance>` command
- **Sand NoClip** - Phase through falling blocks (sand/gravel) when suffocating
- **Scaffold** - Auto-place blocks under feet for bridging
- **Speed** - Multiple speed modes (Strafe, OnGround, Boost)
- **NoFall** - Prevent fall damage with various bypass methods
- **Autoclicker** - Vape V4-style humanized clicking with jitter and fatigue simulation
- **Storage ESP** - Highlight chests, ender chests, hoppers with customizable colors
- **Player Hider** - Hide other players to reduce lag (with whitelist)
- **Plot ESP** - Visualize housing plot boundaries
- **Anti-AFK** - Prevent AFK kicks with subtle movements
- **Visitor Logger** - Log player joins/leaves with timestamps
- **Chat Macros** - Quick-send preset chat messages with hotkeys
- **Waypoints** - Save and teleport to locations per housing plot
- **Creative Tab** - Infinite storage tab with NBT persistence

### Owner Mode Modules
- **Fast Place** - Rapid block placement (10+ blocks/sec)
- **Fast Break** - Accelerated block breaking
- **Auto-Fill** - Fill selections with blocks (Fill/Wall/Floor/Hollow modes)
- **Mirror Build** - Symmetrical block placement across X/Y/Z axes
- **NPC Preview** - Preview NPC/Hologram/Actionpad placement
- **NBT Editor** - View and edit item NBT data
- **Unbreakable** - Make items unbreakable
- **Lore Editor** - Edit item descriptions with color code support
- **Rename** - Rename items with color code formatting

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


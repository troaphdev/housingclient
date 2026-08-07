# Project Architecture

## Package structure

```
com.housingclient/
├── HousingClient.java          # Main mod entry point
├── altmanager/                 # Cookie / session alt login
├── command/                    # Chat commands (.help, .bind, etc)
├── config/                     # Config + profiles
├── event/                      # Event handlers
├── gui/                        # ClickGUI, HUD, alt GUIs
├── imagetonbt/                 # Image → NBT helpers
├── itemlog/                    # Item logging
├── mixin/                      # Mixins (including mod-list filtering)
├── module/                     # Module system
│   ├── Module.java
│   ├── ModuleManager.java
│   └── modules/                # building, client, combat, exploit, …
├── storage/                    # Item storage
├── util/bg/                    # Optional presence enroll / heartbeat (client-side)
└── utils/                      # Chat, render, fonts, etc.
```

## Startup flow

1. `HousingClient` initializes managers, GUI, mixins wiring
2. Config / modules / keybinds load from `.minecraft/housingclient/`
3. On multiplayer join, optional presence (`BgService`) may auto-enroll and heartbeat

There is no separate license-key gate in the current tree. Older docs that mentioned `com.housingclient.license.*` are obsolete.

## Key classes

| Class | Purpose |
|-------|---------|
| `HousingClient` | Mod entry, managers |
| `Module` / `ModuleManager` | Module base + registry |
| `ClickGUI` / `LegacyClickGUI` | In-game settings UI |
| `BgService` | Presence enroll + heartbeat |
| `CookieAltManager` | Cookie → Microsoft / Xbox / MC session |

# Project Architecture

## Package Structure

```
com.housingclient/
├── HousingClient.java          # Main mod entry point, startup flow
├── command/                    # Chat commands (.help, .bind, etc)
├── config/                     # Config management, profiles
├── event/                      # Event handlers
├── gui/                        # ClickGUI, HUD, notifications
├── itemlog/                    # Item logging system
├── license/                    # License validation (see LICENSE_SYSTEM.md)
│   ├── LicenseManager.java     # Core license logic + version check
│   ├── StartupLicenseDialog.java # Swing dialog for license entry
│   └── HWIDGenerator.java      # Hardware ID generation
├── module/                     # Module system
│   ├── Module.java             # Base class (has license guard)
│   ├── ModuleManager.java      # Module registry
│   └── modules/                # All modules by category
│       ├── building/
│       ├── client/
│       ├── combat/
│       ├── exploit/
│       ├── items/
│       ├── movement/
│       ├── render/
│       └── visuals/
├── storage/                    # Item storage systems
└── utils/                      # Utilities (chat, detection, etc)
```

## Startup Flow

1. `HousingClient.preInit()` runs
2. **Version Check** - Calls `/rpc/check_version` to see if version is blocked
3. **License Check** - `StartupLicenseDialog.showAndValidate()` blocks until valid
4. **Periodic Re-validation** starts (every 5 min)
5. Config loads, modules initialize

## Key Classes

| Class | Purpose |
|-------|---------|
| `HousingClient` | Mod entry, startup, managers |
| `Module` | Base class with license guard at line ~50 |
| `LicenseManager` | All license/version logic, Supabase calls |
| `ModuleManager` | Module registry, toggle handling |
| `ClickGUI` | The main GUI interface |

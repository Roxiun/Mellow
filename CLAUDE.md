# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mellow is a Minecraft 1.8.9 Forge client mod for Hypixel BedWars & Duels stats checking. It's a fork of [Fontaine](https://github.com/xanning/Fontaine), using OneConfig for GUI (Right Shift to open). No API key required for core features.

## Build Commands

```bash
# Requires Java 21 locally (CI uses Java 17)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Build (also runs tests, copies jar to PrismLauncher mods dir automatically)
./gradlew build

# Tests only (JUnit 4)
./gradlew test
```

Output jar: `versions/1.8.9-forge/build/libs/Mellow-1.8.9-forge-{version}.jar`

## Build System

Polyfrost Gradle Toolkit with multi-version support (currently only 1.8.9-forge active). Uses ShadowJar to shade OkHttp3, Hypixel Mod API, and XZ. Blossom for token replacement. Mixin 0.7.11 for bytecode injection. Target compatibility is Java 8.

## Architecture

### Entry Point
`Mellow.java` — `@Mod` class. Initializes all services, registers 19 commands and 7 event routers on the Forge event bus. Static fields hold singleton services accessed throughout the codebase.

- `Mellow.isEnabled()` — central mod toggle check. **All features must gate on this** before doing work.
- `Mellow.config` — the `MellowOneConfig` instance (OneConfig-based, ~1600 lines).

### Event Flow
Forge events → **Event Routers** (`core/event/`) → **Feature Services**

Routers are thin dispatchers registered on `MinecraftForge.EVENT_BUS`:
- `ChatEventRouter` — chat messages → denicker, pregame stats, request popups, auto /who
- `ClientTickRouter` — per-tick → HypixelFeatures game state, replay manager
- `TabOverlayRouter` — tab key rendering → extended stats tab overlay
- `WorldLifecycleRouter` — world load → cache clearing
- `NametagColorRouter` — pre/post render → nametag color + client icon context
- `RequestPopupRouter` — overlay render + keybind input → popup accept/deny
- `ReplayHudRouter` / `ReplayInputRouter` — replay UI

### Game State
`HypixelFeatures` (singleton) tracks Hypixel server state via scoreboard/tab parsing. Produces `GameSnapshot` objects consumed by listeners via `addGameStateListener()`. This runs even when the mod is toggled off so state is available on re-enable.

### Stats Pipeline
1. **ProviderManager** selects active provider (HypixelPublicApi, NadeshikoApi, AbyssApi)
2. **PlayerCache** fetches/caches profiles with scoped stat requests (`StatScope`)
3. **StatsChecker** formats stats for display
4. **InGameTabStatsSyncService** pushes stats into `Mellow.tabStats` map (consumed by tab mixin)
5. **GuiPlayerTabOverlayMixin** reads `tabStats` to modify vanilla tab list names

### Async Execution
`AsyncExecutor` singleton manages 5 thread pools: `profileIo` (8), `chat` (4), `command` (4), `supplementalIo` (4), `replayIo` (1). `MainThreadDispatcher` posts runnables back to the client thread.

### Mixin Layer (`mixin/`)
- `GuiPlayerTabOverlayMixin` — injects stats into tab list player names
- `PingMixin` — overrides `NetworkPlayerInfo.getResponseTime` with external ping data
- `nametag/` — color backgrounds + client icon rendering on nametags
- `hitbox/` — team-colored hitboxes
- `replay/` — packet capture via `NetworkManager` injection
- `compat/` — compatibility with PolyNametag, PolyHitbox, VanillaHUD, Overflow Animations

### API Integrations (`api/`)
External services with their own caching: Aurora (denicking, pings, winstreaks), Seraph (client detection, pings, tags), Urchin (tags), Luna (pings), Mojang (UUID/profile), Hypixel Mod API (game state).

### Feature Modules (`feature/`)
- `stats/` — pregame stats, in-game tab sync, extended tab overlay with scrolling
- `replay/` — packet-level game recording/playback with seeking, browser GUI
- `nicks/` — skin denicking + number-pattern denicking
- `requestpopup/` — friend/party request accept/deny overlay
- `profileviewer/` — `/pv` GUI with parsed stats
- `party/` — blacklisted party member warnings
- `tags/` — Urchin/Seraph tag display

### Lists (`util/`)
Blacklist, annoylist, and tag-ignore — file-backed with import support. Stored in `.minecraft/config/mellow/`.

## Key Conventions

- Config lives in one large `MellowOneConfig` class with OneConfig annotations (`@Switch`, `@Dropdown`, `@Number`, etc.)
- Commands extend Forge's `ICommand`, registered in `Mellow.init()`
- Mixin config: `src/main/resources/mixins.mellow.json`
- Assets: `src/main/resources/assets/mellow/` (textures for client icons, GUI, socials)
- All API calls should go through the async executor, never on the main thread
- Feature gating pattern: check `Mellow.isEnabled()` at event handler / mixin entry points

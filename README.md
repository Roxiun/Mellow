<h1 align="center">Mellow</h1>
<p align="center">
<img width="1920" height="392" src="https://i.ibb.co/hRmQV02D/Ads-z.png">
</p>
<div align="center">
Keyless stats mod for Hypixel BedWars &amp; Duels
</div>

<br>

<div align="center">
This project is a fork continuation of <a href="https://github.com/xanning/Fontaine" >Fontaine</a>
</div>

## Features

![Powered by OneConfig](https://polyfrost.org/media/branding/badges/badge_1.svg)

- **No API key required for core features**

- View Bedwars stats in the tablist, similar to Lilith.

- See other players' ping. Recommended to use with [VanillaHUD](https://modrinth.com/mod/vanillahud)
 to view ping numerically instead of as bars.

- Supports [Coral API](https://api.urchin.gg/) tags (API key required)

- Identity lookup uses Mowojang with fallback providers. Retired Seraph tag, reporting, ping, and client-detection integrations have been removed.

- Supports [Xadia API](https://xadia.sniped.me/) tags, including verified and unverified reports

- Import your own local blacklist

- Check BedWars stats and blacklist tags from all enabled APIs and your local blacklist with `/bw`

- Supports skin denicking and finals/beds denicking.

- Auto-check provider-backed Duels stats in Duels matches (mode-aware with division display)

- Pretty & Aesthetic

## Showcase

<details>
<summary>Showcase</summary>

![Tabstats Example 2](https://github.com/user-attachments/assets/654cc0d4-bc77-4110-b5f2-1592007acd0d)
![Tabstats and Skin Denicker Example 1](https://github.com/user-attachments/assets/a7d7c3b7-5181-45b8-a80c-cf944fd203a0)
<img width="1440" height="900" alt="Mellow" src="https://github.com/user-attachments/assets/f9162aaf-8ceb-4203-b778-a23836a391ef" />
<img width="1440" height="900" alt="Settings" src="https://github.com/user-attachments/assets/3da167f9-8748-48b6-80eb-f10d1c3aabd2" />
<img width="1440" height="900" alt="Authie" src="https://github.com/user-attachments/assets/209a7730-6e93-4896-bf9b-3a98d2b8e432" />

</details>

## Download

Choose the release JAR matching your **Minecraft 1.8.9 loader**:

| Target | Client Java | Settings |
| --- | --- | --- |
| Forge | Java 8 | OneConfig v0 |
| Ornithe / OneClient | Java 25 | OneConfig v1 1.2.13+ |

Ornithe requires the complete OneConfig release and its dependencies (Compose Multiplatform and Fabric Language Kotlin), plus Pylon 0.1.7+. Optional integrations are PolyHitbox 1.3.1+, PolyNametag 1.2.1+ and VanillaHUD 3.5.3+. Use dependency releases matching your loader.

Forge and Ornithe have independent settings and HUD layouts. Ornithe starts with fresh defaults in `mellow-v1.json`; Forge uses `mellow.json`. Blacklist and replay formats are preserved.

## Building and testing

Install JDK 8, 21 and 25. Run Gradle on **JDK 21**; the targets select their own compiler toolchains. Forge code and shaded dependencies remain Java 8 compatible.

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21) # macOS
./gradlew :1.8.9-forge:build :1.8.9-ornithe:build
./gradlew :1.8.9-ornithe:runClient -PclientTest
./gradlew :1.8.9-ornithe:runClient -PclientTest -PcompatMods
./gradlew :1.8.9-forge:runClient -PclientTest
./gradlew :1.8.9-forge:runClient -PclientTest -PcompatMods
```

Release JARs are written to `versions/<target>/build/libs/`. Client smoke tests require a graphical environment and use isolated directories under each target's `build/client-test`. Omit `-PclientTest` for an interactive development client. Ornithe additionally supports `-PoneClientBaseline` and `-PoneClientCurrent` with `-PcompatMods` to check its dependency combinations. Forge smoke tests on Apple Silicon can use `-Plwjgl2Dir=/absolute/path/to/lwjgl2` for a compatible local LWJGL 2 distribution.

## Source layout

Stonecutter manages both targets with **Ornithe as the active development target**. Shared feature logic lives in `src/main`; small API differences use `//? if forge` / `//? if ornithe` conditions. Loader-specific settings, HUDs, launch support and resources live in `src/forge` and `src/ornithe`. Both loaders are maintained together on the same branch.

Each target has its own build script. Keep MCP development names on both targets; Loom remaps the release JAR for its loader. The Ornithe mapping overlay is checked in at `mappings/mcp-1.8.9.tiny`. `python3 tools/generate_mappings.py` regenerates it and the canonical replay packet table. Stonecutter selects source branches; it does not replace these mappings.

## Usage

Open OneConfig with **Right Shift**. New configurations use **Bordic** for keyless stats; existing provider choices are preserved. Disable **Show Automatic Stats Errors** to hide background errors.

Match stats fetch automatically. `/who` is optional; use `/refresh` to re-fetch stats during a supported live match.

Enable Coral or Xadia and add their keys under **API Keys**. Xadia keys come from `/key generate` in its Discord bot; **Verified Tags Only** hides unverified reports. Number denicking requires an Aurora key and its feature toggle.

Under **Tab Stats**, Ornithe uses draggable entries that you can uncheck to hide. Forge uses stat-slot dropdowns. Configure HUDs in the OneConfig HUD editor.

### Commands

`<player>` means a username; `[player]` is optional. Run `/mellow` or `/st` for in-game help and aliases.

| Command | Purpose |
| --- | --- |
| `/bw <player>` | BedWars stats and blacklist tags from all enabled APIs and your local blacklist |
| `/sw <player>` | SkyWars stats |
| `/pv [player]` | Profile viewer (defaults to yourself) |
| `/coral <player>`, `/xadia <player>` | Provider tag lookups |
| `/blacklist add/remove/list/import` | Manage your local blacklist; `/bl` also works |
| `/annoylist add/remove/list/import` | Manage your annoy list |
| `/tagignore add/remove/list/import` | Suppress Coral/Xadia tag alerts for selected players |
| `/skindenick <player>` | Skin-based denicking |
| `/denick <finals/beds> <number>` | Number-based denicking |
| `/refresh` | Refresh stats in a live match |
| `/mstatus <player>` | Online status, last login, and Luna lobby data |
| `/namehistory <player>` | Name history |
| `/winstreak <player>` | Visible BedWars winstreak, with Aurora fallback if enabled |
| `/mellowstats <player> [game\|auto] [mode]` | Query any supported game; tab completion lists games and modes |
| `/clearcache` | Clear cached player data |
| `/mdebug <all/state/scoreboard/pregame>` | Game-state diagnostics |
| `/mreplay` | Open the replay browser |

List commands take `add <player> [reason]`, `remove <player>`, `list`, or `import <filename>`. Put import files in your instance's `config/mellow` folder. `/bl` and `/mblacklist` are aliases.

Replay subcommands: `list`, `open <id/index>`, `info <id/index>`, `delete <id/index>`, and `tp <player>` (also `spectate`).

Coral and Xadia use native tag integrations. Stats and tags load independently; failed sources are reported separately.

## Data and game-state flow

- `gamestate/GameStateManager` owns connection, party, location, and game phase. `ScoreboardObservation` extracts sidebar facts; Bedwars chat signals provide explicit match-start evidence. A missing sidebar preserves the established phase. A new world/server starts an unknown session; lobby location and disconnect packets reset the relevant state. Session IDs are separate from ordinary snapshot revisions.
- `feature/stats/InGameTabStatsSyncService` scans the live roster every 1.5 seconds. `StatsChecker` applies local checks immediately, schedules stats and tags independently, and applies results on the client thread only while their session and refresh generation remain current. Party checks use membership UUIDs independently of the match and never fetch stats. Successful remote checks are retained per member for the connection session, including across leaving/rejoining the party; newcomers are still checked. Local lists remain checked locally.
- `cache/PlayerCache` owns request sharing and freshness. Adapters live in `api/hypixel/provider`; `HypixelPlayerData` handles provider envelopes and identity/rank differences. Raw responses are shared across commands, views, and game scopes. `stats/PlayerStatsService` parses a caller-supplied `StatsSelection`, without consulting live client state.
- `stats/GameRegistry` lists supported games. Each game owns its parser, typed stats, submodes, tab columns, defaults, and chat output. Tab renderers consume `StatDefinition` metadata. `PlayerProfile` stores typed game results without a field per game. All five games configure their column layouts inside OneConfig: checkable draggable lists on Ornithe and ordered dropdowns on Forge, using each platform's existing config file. Missing game data does not fall back to Bed Wars.
- Native Coral and Xadia adapters share request caching through `api/tags/TagRequests`. Their responses become `TagReport` / `PlayerTag` values used by alerts and overlays. A report preserves failures separately from empty successful results. `TagPolicy` centralizes automatic tag suppression and tab visibility; manual lookups show the fetched report. `CubelifyParser` supplies shared envelope decoding with explicit provider mappings for warning tags, metadata, and HTTP-200 error badges.
- Roster lookups use Coral/Xadia batches and Bordic bulk stats, in groups of at most 100. Single and batch requests share cache entries. Shared stats and tag responses stay fresh for five minutes. Successful identity lookups are retained for the connection session (bounded to 4,096 entries per cache), and cleared on disconnect or explicit cache clearing; identity failures expire after 30 seconds. Successful roster lookups are retained for the match; expiry does not trigger polling. Automatic failures receive at most one retry after 30 seconds; stats rate limits impose a provider cooldown. Upstream provider caches may contain older data.
- `/refresh` invalidates current players' stats and tags while retaining good displayed rows until replacements arrive. `/clearcache` invalidates cached and pending results without changing local lists. `RequestCache` bounds retained entries and shares pending work; clearing an entry detaches its old completion. Ping discovery runs from ticks, with rendering limited to cache reads; successful values remain for the match instead of being periodically refreshed.

## Adding game stats

1. Create a package under `stats/` containing the typed player stats, parser, and a `GameDefinition` (plus a mode enum when needed). Parsers receive normalised `HypixelPlayerData`, never a provider ID.
2. Add a `StatScope` and register the definition in `GameRegistry`. Give each column a stable ID, label, formatting style, and default position. For compatibility with team/name composition, keep Team at index 0 and Name at index 2. The existing games illustrate badge and numeric columns at index 1.
3. Define game detection and, if applicable, submode detection. Preserve missing data instead of silently substituting another game's or mode's totals.
4. Add fixtures covering the API fields, calculations, and detection rules that are specific to the game. The shared command and tab renderers pick it up. Add its native OneConfig controls to each platform and connect them in `ExtendedTabStatsColumns`; keep the game's column IDs and defaults stable.

Add submodes inside their game's package. The detailed Bed Wars profile viewer uses the same `BedwarsMode` definitions; its custom visual layout remains unchanged. Provider-specific transport or envelope changes belong under `api/hypixel`, independently of game definitions.

## Community

[Join the Discord](https://discord.gg/psbdtEKkxa) · [Report a bug](https://github.com/Roxiun/Mellow/issues)

## Credits
Original Creator: `melissalmao` - Melissa (fwrina)

[Fontaine](https://github.com/xanning/Fontaine): `xanning`

Mellow (Fork): `Roxiun`

[YedelMod](https://github.com/Yedelo/YedelMod): `Yedelo` — inspiration for coordinating the bundled Hypixel Mod API loader with OneConfig and existing API installations.

[Hypixel Forge Mod API](https://github.com/HypixelDev/ForgeModAPI): bundled Forge integration and adapted MIT-licensed API loader.

[Lucid](https://github.com/afterlikeorg/Lucid): Many anticheat checks were adopted from Lucid, and wouldn't be possible without them

### Features:
Name & Upgrade HUD: `zifro`

Original Emerald Counter: `jqsie`

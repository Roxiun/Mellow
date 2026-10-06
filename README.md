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

- Seraph integration is deprecated. Settings and legacy code remain, but all Seraph requests (tags, reports, ping, client detection, and UUID fallback) are disabled.

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

Download the Ornithe jar from [Modrinth](https://modrinth.com/mod/statsify) into your instance's `mods` folder. This branch targets **Minecraft 1.8.9 on Ornithe**, with **Java 25** and **OneConfig v1 1.2.13 or newer**. Use the Ornithe/OneClient editions of the dependencies:

- [OneConfig](https://modrinth.com/mod/oneconfig), including its required dependencies (Compose Multiplatform and Fabric Language Kotlin).
- Pylon 0.1.7 or newer for the LWJGL 3 runtime.
- Optional: [PolyHitbox](https://modrinth.com/mod/hitbox) 1.3.1, [PolyNametag](https://modrinth.com/mod/polynametag) 1.2.1, and [VanillaHUD](https://modrinth.com/mod/vanillahud) 3.5.3 or newer. The compatibility smoke tests cover OneClient’s OneConfig 1.2.13 / VanillaHUD 3.5.3 and the newer 1.2.16 / 3.5.4 pair.

Install the **complete OneConfig release jar**; its nested `oneconfigv1` module is required. The Forge editions of Mellow and these mods cannot be used in the Ornithe instance.

Matching scalar settings import once from the old `mellow.json` into `mellow-v1.json`, leaving the original file untouched. Configure HUD layout and appearance in the new OneConfig HUD editor. Existing blacklists and replay files retain their formats.

## Building and testing

Install JDK 21 and JDK 25. Gradle runs on 21 and selects the Java 25 compiler/client toolchain:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21) # macOS
./gradlew build
./gradlew runClient -PclientTest
./gradlew runClient -PclientTest -PcompatMods
./gradlew runClient -PclientTest -PcompatMods -PoneClientBaseline
```

The release jar is written to `build/libs/Mellow-1.8.9-ornithe-<version>.jar`. Client smoke tests launch a real client, verify initialization and replay packet round trips, then exit; they require a graphical environment. Their separate configurations live under `build/client-test`. `-PcompatMods` adds the three pinned optional mods. Omit `-PclientTest` for an interactive development client.

## Usage

Open OneConfig with **Right Shift**. New configurations use **Bordic** for keyless stats; existing provider choices are preserved. Disable **Show Automatic Stats Errors** to hide background errors.

Match stats fetch automatically. `/who` is optional; use `/refresh` to re-fetch stats during a supported live match.

Enable Coral or Xadia and add their keys under **API Keys**. Xadia keys come from `/key generate` in its Discord bot; **Verified Tags Only** hides unverified reports. Number denicking requires an Aurora key and its feature toggle.

Under **Tab Stats**, drag entries to reorder them and uncheck entries to hide them. Configure HUDs in the OneConfig HUD editor.

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
| `/clearcache` | Clear cached player data |
| `/mdebug <all/state/scoreboard/pregame>` | Game-state diagnostics |
| `/mreplay` | Open the replay browser |

List commands take `add <player> [reason]`, `remove <player>`, `list`, or `import <filename>`. Put import files in your instance's `config/mellow` folder. With the Seraph mod installed, use `/mblacklist` or `/bl` instead of `/blacklist`.

Replay subcommands: `list`, `open <id/index>`, `info <id/index>`, `delete <id/index>`, and `tp <player>` (also `spectate`).

Seraph requests are disabled; `/seraph`, `/client`, and legacy Seraph reporting remain deprecated.

## Community

[Join the Discord](https://discord.gg/psbdtEKkxa) · [Report a bug](https://github.com/Roxiun/Mellow/issues)

## Credits
Original Creator: `melissalmao` - Melissa (fwrina)

[Fontaine](https://github.com/xanning/Fontaine): `xanning`

Mellow (Fork): `Roxiun`

[Lucid](https://github.com/afterlikeorg/Lucid): Many anticheat checks were adopted from Lucid, and wouldn't be possible without them

### Features:
Name & Upgrade HUD: `zifro`

Original Emerald Counter: `jqsie`

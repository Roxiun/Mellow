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

- Seraph integration is deprecated. All Seraph requests (tags, reports, ping, client detection, and UUID fallback) are disabled.

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

Download the **Minecraft 1.8.9 Forge** jar from [Releases](https://github.com/Roxiun/Mellow/releases) and place it in your instance's `mods` folder. This edition targets Java 8 and uses OneConfig; Ornithe jars are for a separate loader.

## Building

Run Gradle with **JDK 21**; Forge artifacts remain Java 8 compatible.

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21) # macOS
./gradlew build
```

## Usage

Open OneConfig with **Right Shift**. New configurations use **Bordic** for keyless stats; existing provider choices are preserved. Disable **Show Automatic Stats Errors** to hide background errors.

Match stats fetch automatically. `/who` is optional; use `/refresh` to re-fetch stats during a supported live match.

Enable Coral or Xadia and add their keys under **API Keys**. Xadia keys come from `/key generate` in its Discord bot; **Verified Tags Only** hides unverified reports. Number denicking requires an Aurora key and its feature toggle.

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

[YedelMod](https://github.com/Yedelo/YedelMod): `Yedelo` — inspiration for coordinating the bundled Hypixel Mod API loader with OneConfig and existing API installations.

[Hypixel Forge Mod API](https://github.com/HypixelDev/ForgeModAPI): bundled Forge integration and adapted MIT-licensed API loader.

[Lucid](https://github.com/afterlikeorg/Lucid): Many anticheat checks were adopted from Lucid, and wouldn't be possible without them

### Features:
Name & Upgrade HUD: `zifro`

Original Emerald Counter: `jqsie`

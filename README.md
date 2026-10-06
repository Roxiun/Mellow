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

- Supports [Coral API](https://api.urchin.gg/) Tags

- Seraph integration is deprecated. Settings and legacy code remain, but all Seraph requests (tags, reports, ping, client detection, and UUID fallback) are disabled.

- Supports [Xadia API](https://xadia.sniped.me/) tags, including verified and unverified reports

- Import your own local blacklist

- Check any player's BedWars stats with `/bw`

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


</div>
</details>

## Download

This branch targets **Minecraft 1.8.9 on Ornithe**, with **Java 25** and **OneConfig v1 1.2.13 or newer**. Use the Ornithe/OneClient editions of the dependencies:

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

The development build unpacks OneConfig's nested modules before Loom remaps them. This prevents the `oneconfigv1 1.2.16 is missing` development launch error. Published compile APIs are pinned to 1.2.10; runtime verification covers the complete 1.2.13 and 1.2.16 releases.

If Fabric reports many incompatible OSL variants alongside a Mellow dependency mismatch, update Mellow first. The OSL bundle contains variants for multiple Minecraft versions, and Fabric can list unsuitable alternatives when another mod prevents dependency resolution. Mellow 7.0.0 resolves with OneClient’s OSL 0.22.0 bundle; replacing individual OSL modules is unnecessary for that setup.

Development sources retain MCP names using the checked-in `mappings/mcp-1.8.9.tiny` overlay; Loom remaps the release to Ornithe Calamus generation 2. Regenerate the overlay with `python3 tools/generate_mappings.py`. Replay packet identifiers remain the original MCP names so old recordings can be read across the port.

## Usage

All settings can be configured through OneConfig (press **Right Shift**). New configurations use **Bordic** for stats; existing provider choices are preserved.

Under **Tab Stats**, drag entries in each game's stat list to change the order, and uncheck entries to hide them. Existing numeric slot selections migrate once into the lists. Duplicate selections collapse into a single entry; hidden slots are omitted. All available stats can now be enabled, beyond the old ten-slot limit.

### Commands

To display **help command** type `/mellow`

To **check the stats of players in your game**, type `/who` in-game, or enable Auto Who in settings

To check an **individual player’s** BedWars stats, type `/bw <username>`

Enable **Xadia** in OneConfig and set your key under **API Keys > Xadia**. Generate a personal key with `/key generate` in the Xadia Discord bot. Use `/xadia <username>` for a manual lookup. Verified tags show a green check in tab; unverified reports show `?` in tab and `[Unverified]` in chat. Enable **Verified Tags Only** to hide unverified reports. Xadia tags also appear in player stat lookups, pregame/in-game alerts, and party warnings. Automatic lookups are off by default.

To add a player to your **blacklist**, type `/blacklist add <username>`

The legacy Seraph report syntax remains available, but report submission is disabled. Use `/blacklist add <username>` for your local blacklist.

You can **import** your own **blacklist** by doing, type `/blacklist import <filename>`, place the file in `.minecraft/config/mellow`

To add a player to your **annoy list**, type `/annoylist add <username>`

You can **import** your own **annoy list** by doing, type `/annoylist import <filename>`, place the file in `.minecraft/config/mellow`

To suppress Coral/Seraph tag alert lines for a player, type `/tagignore add <username>`

To check a player's **Coral tags**, type `/coral <username>` (legacy `/urchin` aliases are also supported)

You can **import** your own **tag ignore list** by doing, type `/tagignore import <filename>`, place the file in `.minecraft/config/mellow`

To **skin denick** type `/skindenick <username>`

To use the **number denicker** add your Aurora API key

- You can obtain one [here](https://discord.com/oauth2/authorize?client_id=1244205279697174539)
- After setup, denicking happens automatically during games, but you can also manually run: `/denick <finals | beds> <number>`

## Known issues

- Occasionally stats fail to fetch due to rate limits. You can either try switching the stats provider in settings or try running `/who` again after a while.
- Does not work on some VPNs

If any other issues or bugs are found please report them [here](https://github.com/Roxiun/Mellow)


## Credits
Original Creator: `melissalmao` - Melissa (fwrina)

[Fontaine](https://github.com/xanning/Fontaine): `xanning`

Mellow (Fork): `Roxiun`

[Lucid](https://github.com/afterlikeorg/Lucid): Many anticheat checks were adopted from Lucid, and wouldn't be possible without them

### Features:
Name & Upgrade HUD: `zifro`

Original Emerald Counter: `jqsie`

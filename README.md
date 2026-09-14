# spacebod-mods

Small Fabric mods for Minecraft 26.1, 26.2 and 26.3, written as a way of learning Java. Each mod is its own Gradle project in its own folder and ships as one jar per Minecraft version.

All mods are optional-config: they work out of the box, and every setting can be changed by hand in `config/<mod>.json` or in game through Mod Menu when Mod Menu and Cloth Config are installed.

## Mods

### Farsight (client)

Hold a key to zoom. Scroll to change magnification.

- Default key Z, rebindable in Controls
- Default 4x, scroll up to 20x, both adjustable
- Smooth zoom and cinematic camera while zoomed, each can be turned off
- Optional overlay showing the coordinates of and distance to the block under the crosshair, plus the current zoom level
- Overlay position: top centre, or any corner

### Tidy (client)

Middle-click any inventory to sort it. Works on vanilla servers.

- Sort by type (creative menu order, like items together) or by quantity (biggest stacks first)
- Shift + click runs the other sort, or bind it to its own key
- Optional grouping: tools, armour, food and blocks first
- Sorts the container or the player inventory depending on where you click
- Rate-limited on servers to stay clear of anti-cheat, instant in single-player

### Cascade (server)

Crouch and chop one log to fell the whole tree.

- Works on vanilla, modded and nether trees, and huge mushrooms
- Leaves decay quickly and a sapling is replanted at the stump
- Vein mining for ores with the same crouch-and-mine gesture
- Bigger trees and bigger veins take longer to break, and tool durability and hunger are charged as if each block was broken by hand
- Drops are collected into stacks instead of a pile of item entities
- Refuses anything that does not look like a natural tree, so log cabins are safe

### SilkSpawners (server)

Break a spawner with Silk Touch to pick it up with all its data intact.

- The dropped item is named after its mob, for example Zombie Spawner
- Configurable drop chance, a failed roll gives the vanilla XP instead
- Slowness while a spawner is in your inventory, can be turned off
- Anyone can place the dropped spawner, not just operators

### Salvage (server)

Turn junk into materials. Pure data, each recipe can be switched off.

- Rotten flesh smelts, smokes or campfire-cooks into leather
- One wool of any colour crafts into two string

## Installing

Download the jar whose name ends in the Minecraft version you play, for example `farsight-0.1.0+mc26.2.jar`, and drop it into your mods folder along with Fabric API. Server mods go on the server; client mods go on your own game. Mod Menu and Cloth Config are optional and only add the in-game settings screen.

## Building

Requirements: JDK 25. Minecraft 26.x needs Java 25 or newer.

```
cd farsight
./gradlew runClient   # dev client with the mod loaded
./gradlew build       # jar into build/libs/
```

To build every mod for every supported Minecraft version:

```
.\release-matrix.ps1                 # all mods, all versions, into releases/<mod>/
.\release-matrix.ps1 -Mod tidy       # one mod
```

## How versions work

Code that is the same on every Minecraft version lives in `src/main` and `src/client`. The few classes that differ live in `src/versions/<version>/`, one copy per version, and the build compiles in the matching folder. Each jar is therefore plain compiled code for exactly the game it targets, with no reflection or runtime version checks.

Jars are named `<mod>-<mod version>+mc<minecraft version>.jar`. The mod version is semver per mod, set in that mod's `gradle.properties`. Git tags are per mod, for example `farsight-v0.1.0`.

When a new Minecraft version comes out: add it to the default list in `release-matrix.ps1`, build, fix what fails to compile, add a `src/versions/<version>/` folder only for classes that must differ, then cut a release.

## Licence

MIT

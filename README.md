# spacebod-mods

Small Fabric mods for Minecraft 26.1, 26.2 and 26.3, written as a way of learning Java. Each mod is its own Gradle project in its own folder and ships as one jar per Minecraft version.

All mods are optional-config: they work out of the box, and every setting can be changed by hand in `config/<mod>.json` or in game through Mod Menu when Mod Menu and Cloth Config are installed.

## Mods

### Farsight (client)

Hold a key to zoom. Scroll to change magnification.

**Why:** most zoom mods stop at magnification. They don't tell you how far away the thing you're looking at is, and a lot of them hard-code the zoom level and keybind instead of letting you change them. Farsight adds a distance readout for judging travel time at a glance, and every number in it is configurable.

- Default key Z, rebindable in Controls
- Default 4x, scroll up to 20x, both adjustable
- Smooth zoom and cinematic camera while zoomed, each can be turned off
- Optional overlay showing the coordinates of and distance to the block under the crosshair, plus the current zoom level
- Overlay position: top centre, or any corner

### Tidy (client)

Middle-click any inventory to sort it. Works on vanilla servers.

**Why:** most inventory-sorting mods add a new button onto the inventory GUI, which is small and fiddly to aim for and click. Tidy uses a middle-click anywhere in the inventory instead, which is faster and doesn't need you to hunt for a button.

- Sort by type (creative menu order, like items together) or by quantity (biggest stacks first)
- Shift + click runs the other sort, or bind it to its own key
- Optional grouping: tools, armour, food and blocks first
- Sorts the container or the player inventory depending on where you click
- Rate-limited on servers to stay clear of anti-cheat, instant in single-player

### Cascade (server)

Crouch and chop one log to fell the whole tree, or crouch and mine one ore to take the whole vein.

**Why:** tree-felling and vein-mining mods tend to pick one side of a trade-off and stay there — either they're free (no durability or hunger cost, so they trivialise gathering) or they're clunky (huge lag from hundreds of dropped item entities and floating XP orbs). Cascade tries to keep both features from being overpowered as well as clean: durability is charged as if every block had been broken by hand, hunger drains faster than that to make clearing a whole tree or vein a real cost rather than a free lunch, and it doesn't leave a mess behind.

- Works on vanilla, modded and nether trees, and huge mushrooms
- Vein mining for ores using the same crouch-and-mine gesture
- Not a free lunch: durability is charged per block as if broken by hand, and hunger drains faster than the vanilla per-block rate, so felling a big tree or stripping a big vein costs noticeably more food than mining it block by block
- Mining/cutting speed scales with the size of the tree or vein, so bigger jobs take proportionally longer
- Drops are collected and dropped as pre-stacked item entities, and XP is clumped into fewer orbs, instead of the lag-inducing pile of single-item entities and separate orbs that other mods leave behind
- Same clumped-drop treatment extended to bamboo, sugar cane, seaweed, vines and similar plants, so farming and clean-up are less tedious
- Leaves decay quickly and a sapling is replanted at the stump
- Refuses anything that does not look like a natural tree, so log cabins are safe

### SilkSpawners (server)

Break a spawner with Silk Touch to pick it up with all its data intact.

**Why:** spawner-pickup mods exist, but they're usually all-or-nothing — you always get the spawner, and it comes out generic. SilkSpawners adds a chance of the spawner just breaking on you, same as any other silk-touched block can fail you, and it keeps the spawner's actual NBT data instead of resetting it. A custom spawner that was tuned to spawn, say, mobs with unusual armour, keeps those exact properties when it's picked back up and replaced.

- The dropped item is named after its mob, for example Zombie Spawner
- Configurable chance to break the spawner instead of dropping it; a failed roll gives the vanilla XP instead
- Preserves custom NBT data, so a modified spawner (custom mob variants, armour, equipment, etc.) keeps its exact configuration when moved
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

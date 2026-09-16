# Black Mark

Minecraft 1.20.1, Forge 47.4.10. Multiplayer, server-authoritative — all logic on the server, the client only renders.

[Modrinth](https://modrinth.com/mod/black-mark) (full description, curse list, lore) · [Releases](https://github.com/Holego/black-mark/releases) · License: MIT

---

## Curses

One interface, one registry line each.

| Curse | id |
|---|---|
| Clinging | `slot_devourer` |
| Feeble-mindedness | `dementia` |
| Blight | `blight` |
| Everlasting Wounds | `eternal_wounds` |
| Dead Man's Tongue | `pirate` |

## Adding your own curse

One class and one line. Nothing else needs to change.

**1. The class:**

```java
package com.goshan.blackmark.curse.impl;

public class MyCurse implements Curse {

    @Override
    public ResourceLocation id() {
        return new ResourceLocation(BlackMarkMod.MODID, "my_curse");
    }

    @Override
    public int weight() {
        return 10; // relative rarity: higher rolls more often
    }

    @Override
    public void serverTick(ServerPlayer player, MarkData data, CompoundTag state) {
        // state is this curse's own private memory on this player.
        // Saved with the player and synced to their client.
        // Call data.markDirty() after changing it.
    }

    @Override
    public void onDefiance(ServerPlayer player, MarkData data, CompoundTag state, int amount) {
        // the bearer just tried to get rid of the mark - answer if you like
    }
}
```

Available hooks: `onApply`, `onRemove`, `serverTick`, `onDefiance`, `onRespawn`.

**2. Registration** — in `CurseRegistry.bootstrap()`:

```java
register(new MyCurse());
```

**3. Strings** in `lang/en_us.json` (and `lang/ru_ru.json` if you want a Russian line too):

```json
"curse.blackmark.my_curse": "Display name",
"curse.blackmark.my_curse.onset": "The line the bearer hears the moment the curse takes hold."
```

**One important thing about the client.** `Config` is `COMMON` type — it is **never synced** from server to client. If a curse needs to render something, the server has to put the finished number into `state` (the way `dementia` writes `Slip`, and `slot_devourer` writes the blocked-slot array), and the client reads only that. Never call `Config.*.get()` from client-side code.

---

## Configuration

`config/blackmark-common.toml` is generated on first run. Sections:

- `[affliction]` — frequency, multipliers, "raid days", how many players can be marked at once, whether to announce in chat and name the bearer;
- `[mark]` — mist/flesh timings, minimum carry time;
- `[wake]` — the rite's requirements, the tribute item;
- `[curses.*]` — one section per curse.

For quick testing without waiting:

```
/blackmark afflict <player> [curse]
/blackmark info [player]
/blackmark free <player>
/blackmark curses
```

Requires permission level 2.

---

## Building

**Gradle itself must run on JDK 17.** Not 21, not 25 — verified on this machine: under 21, ForgeGradle 6.0 breaks mapping generation. Gradle downloads JDK 17 for *compilation* on its own via the toolchain, but it takes the daemon's own JVM from `JAVA_HOME`.

Since the system `java` here is 25, set `JAVA_HOME` explicitly:

```bash
JAVA_HOME="$USERPROFILE/.gradle/jdks/eclipse_adoptium-17-amd64-windows/jdk-17.0.20.1+1" ./gradlew build
```

That's the JDK 17 Gradle already downloaded on its own. More reliably, install one permanently instead of depending on Gradle's cache:

```bash
winget install EclipseAdoptium.Temurin.17.JDK
```

To avoid setting `JAVA_HOME` every time, add a path to `gradle.properties` (forward slashes required):

```
org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-17.0.x-hotspot
```

**Run the first build single-threaded:**

```bash
./gradlew build --max-workers=1
```

Under parallel resolution, ForgeGradle can read its own not-yet-finished mapping artifact and crash with an `ArrayIndexOutOfBoundsException` in `loadProguard`. The files themselves are fine — it's a race, not corruption. Once the first build succeeds, the cache is warm and the flag isn't needed again.

The built jar is `build/libs/blackmark-0.1.0.jar` (~80 KB).

To run for testing:

```bash
./gradlew runClient
```

There's a second configuration, `runClient2` (nickname `Deckhand`), for checking multiplayer between two clients over LAN.

### Compiler warnings

Five of them, all `deprecated and marked for removal` — `FMLJavaModLoadingContext.get()`, `ModLoadingContext.get()`, the `ResourceLocation(String, String)` constructor. On 1.20.1 these are working APIs flagged for removal in later versions. There's no point touching them now: their replacements only exist in 1.21+, and switching to them would break the 1.20.1 build.

---

## Architecture

```
mark/          the mark itself: item, state, capability, server manager, the rite
  MarkData         everything the mark remembers about its bearer (survives death)
  MarkManager      server-side authority: afflict, free, tick, police the inventory
  MarkAffliction   who and when to choose
  WakeRitual       the one way out
  SlotLock         shared view of devoured slots (curse writes it, sweep and client read it)
curse/         the curse registry plus five implementations
net/           syncs state to the client as one NBT packet
event/         all the Forge event wiring
client/        rendering: blackened slots, mist, wounds, drifting names
command/       /blackmark for admins
```

`MarkManager.sweep()` holds the one invariant everything else depends on: a marked player carries exactly one mark, inside their pack, and nowhere else. It runs every tick and catches every route out — dropping, death, `/clear`, foreign containers, the cursor, shulkers, other mods.

---

## What's verified, and what isn't

The mod builds and has been **actually launched** in a 1.20.1 dev client. Confirmed from the log:

- `Found valid mod file main with {blackmark} mods - versions {0.1.0}` — `mods.toml` is valid;
- `Creating FMLModContainer instance for com.goshan.blackmark.BlackMarkMod` — the entry point comes up;
- `Black Mark loaded with 5 curse(s) registered.` — the curse registry and networking initialize;
- `ForgeEvents` and `ClientEvents` are subscribed on the FORGE bus, `MarkCapability$ModBusHandler` on the MOD bus;
- `blackmark-common.toml` is generated with every section;
- no warnings about missing textures or models;
- the game reaches the main menu without crashing.

**Not verified by hand** (needs the actual game): afflicting a player, the blackened slots, mist/flesh behavior, food rotting, the hotbar scramble, black hearts, enchantment swapping, and the Wake itself. The logic is written and loads, but the gameplay hasn't been played through by hand.

Suggested order to check, most noticeable first:

```
/blackmark afflict <name> slot_devourer
/blackmark afflict <name> dementia
/blackmark info <name>
```

For feeble-mindedness you need an enchanted item: drop it in your pack and hover — the enchantments should read as someone else's. Wear it or hold it — they become real.

For `pirate` — type something in chat, then pull out a gold ingot and try to drop it.

## What isn't done

- 1.20.1 only. A 1.19.2 backport would mean reworking a couple of classes (`GuiGraphics` doesn't exist in 1.19.2, rendering goes through `PoseStack`), but as a separate branch.
- A mark on a corpse, and it jumping to a different entity after the bearer's death — that's in the lore, not yet in the code.
- No custom sounds; everything uses vanilla ones.

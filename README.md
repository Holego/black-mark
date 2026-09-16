# Black Mark

A mod for **Minecraft 1.20.1, Forge 47.4.10**. Built for multiplayer: all logic lives on the server, the client only renders.

> It has no living master. It simply carries out a last will. A pirate's, presumably.

[Modrinth](https://modrinth.com/mod/black-mark) · [GitHub Releases](https://github.com/Holego/black-mark/releases) · License: MIT

---

## What happens in game

Every 5 minutes (configurable) the server rolls a die on every player. The chance is tiny, but it climbs:

| Condition | Multiplier |
|---|---|
| Coastal biome (beach / ocean) | ×6 |
| Night | ×2 |
| Thunderstorm | ×2.5 |
| "Raid day" — every 13th in-game day | ×8 |

All of it stacks: at night, on a beach, in a storm, on a raid day, the chance is 240 times the base rate. But there's no requirement — the mark can settle on someone who has never seen the ocean.

The chosen player finds the **Black Mark** in their inventory. It:

- **cannot be dropped** — `Q` does nothing; no item entity for it exists in the world, ever;
- **cannot be stashed** — a chest, a shulker, an item frame, someone else's container: it's back in your pack the same tick;
- **cannot be picked up while it's mist** — the click passes straight through;
- **survives death** — it's already back in the inventory after respawn;
- keeps score of every attempt to get rid of it. Each one is **defiance**, and every active curse answers to it.

The mark constantly drifts between two states:

- **Mist** (200–700 ticks) — translucent, the slot is filled with haze, cannot be taken;
- **Flesh** (60–200 ticks) — can be lifted with the cursor, but it runs back down.

Any attempt to touch it **immediately flips its state** — exactly as the lore describes.

---

## Curses

The mark is the carrier. The curse is what it brings — usually one, with a 6% chance of two.

| Curse | What it actually does |
|---|---|
| **Clinging** (`slot_devourer`) | Inventory slots around the mark go black and stop working. Spreads every 8 minutes and with every act of defiance, up to 12 slots. A blocked slot can't be selected in the hotbar and nothing can be placed in it — items get pushed out. |
| **Feeble-mindedness** (`dementia`) | Lies about everything in your pack — see below. Chat speech gets slurred. Every 45 seconds, two hotbar slots silently swap places. |
| **Blight** (`blight`) | Every 3 minutes, a stack of food in the inventory rots. Saturation from any meal is halved. Natural regeneration doesn't work — only potions, golden apples and beacons can heal. |
| **Everlasting Wounds** (`eternal_wounds`) | Any hit dealing 4+ damage takes half a heart **permanently**, for as long as the mark stays. Up to 8 half-hearts. Lost hearts render black. |
| **Dead Man's Tongue** (`pirate`) | The pirate shows through the bearer: someone else's speech, outbursts in chat, blows against allies, and gold-driven frenzy — see below. |

All of this is inventory and interface mechanics, not potion effects. There is no "Weakness" anywhere in the effects list.

### The veil of feeble-mindedness

Two separate lies, each with its own way to be seen through.

**Enchantments are swapped.** While an item just sits in the pack, its tooltip shows *other, entirely real-looking* enchantments — "Sharpness IV" where it's actually "Bane of Arthropods II". Not garbled letters — a plausible untruth you'd believe. The lie is **stable** per item (seeded from the item and its NBT), so the tooltip doesn't flicker between variants: the bearer is consistently wrong. The truth returns only once the item is **worn or held** — the only way to learn what sword you actually have is to pick it up.

**Names and descriptions drift until you've touched them.** Letters are swapped for other letters of the same alphabet. This clears for one specific item the moment the bearer has actually handled it — tapped the slot, or lifted it onto the cursor. Anything never touched stays a stranger. The memory is keyed by "item + NBT", so moving a stack between slots doesn't reset it, and logging off the server wipes it clean again.

Enchantment lines are never scrambled letter-by-letter — they follow their own rule. The item's mechanics never change at all: only the interface lies, the sword hits exactly as it always did. All of this logic is client-side (`client/DementiaVeil.java`) and reads only what the server has already synced.

---

### Dead Man's Tongue

The mark carries out a pirate's last will, and over time the pirate starts showing through the bearer.

**Speech.** Whole words are substituted, not individual letters — the sentence stays readable, the bearer is possessed, not unintelligible. "hello" → "ahoy", "yes" → "aye", "money" → "doubloons". Works in both Russian and English — the language is picked by whether the text contains Cyrillic, and the oaths are matched to it. Case is preserved.

The profanity is salty sailor oaths ("Shiver me timbers!", "Blast me barnacles!"), not real swearing. That's a deliberate choice: this is a server mod, and automatic foul language coming out of a player's mouth means complaints and bans. It reads just as piratical without putting anyone at risk. The list lives in `util/PirateTongue.java`.

**Shouting.** A message the bearer actually typed sometimes comes out in ALL CAPS (`shoutOnSendChance`, 25%). On top of that, roughly every ninety seconds the pirate bellows into chat entirely on his own — under the bearer's name, in gold.

**Striking allies.** Every 45 seconds, with a 15% chance, the bearer swings at the nearest ally within 3.5 blocks: a player, a tamed animal, a villager, or an iron golem. Half a heart, with a swing animation and a shout. **With PvP disabled server-side, vanilla's own check simply refuses the hit on a player** — there is no bypass and there won't be one. Striking players can be turned off entirely with the `strikePlayers` flag.

**Gold frenzy.** The bearer senses gold in their inventory, lying on the ground within 8 blocks, and buried in the walls within 5 blocks — a gold vein in a cave triggers the frenzy just as well. For 20 seconds the screen is washed in a pulsing golden haze, shouting and striking become four times as likely, and **gold cannot be dropped at all**. Trying to get rid of the mark also triggers the frenzy.

Scanning blocks is the one place in the mod that actually costs server time: an 11×11×11 cube every 3 seconds per cursed player. `goldScanRadius = 0` turns off the search through walls, leaving only the inventory and the ground.

## Getting free: the Wake

There is exactly one way out — the very case from the lore: a gathering for the lost, from whose ashes the mark was once seen to dissolve into the air.

All of the following, at once:

1. The mark must have been carried **at least 25 minutes** — any sooner, and it "hasn't had its fill";
2. **Night**;
3. **A coastal biome** (beach or ocean);
4. **A lit campfire**;
5. Enough players nearby (within 12 blocks) — 1 by default, though 2+ makes more sense on a real server;
6. The mark **in the main hand**, a **gold ingot in the off hand** — the tribute.

Right-click the campfire. It goes out, the ingot is consumed, the mark dissolves. If anything doesn't line up, it will say what's missing.

---

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

The key invariant is held by `MarkManager.sweep()`: a marked player carries **exactly one** mark, **inside their pack**, and nowhere else. Sweep runs every tick and catches every route out — dropping, death, `/clear`, foreign containers, the cursor, shulkers, other mods.

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

# Slay the Sector

A roguelite layered on top of an intact Starsector campaign: the player leaves the normal sector
for short, self-contained runs and keeps permanent progress between them.

## Language

### The run

**Run**:
One self-contained roguelite attempt, from leaving the Hub to winning or losing.
_Avoid_: Descent, dive, run system, expedition

**Hub**:
The mod's own station in the normal sector where a Run is started and where the player returns when it ends.
_Avoid_: Base, home system, Hub station

**The Descent**:
The contained star system where every Run takes place; a place name, never a synonym for Run.
_Avoid_: Run system, depths, abyss

**Act**:
One third of a Run: its own map of Nodes ending in a Boss.
_Avoid_: Floor, chapter, sector

**Ascension**:
An optional difficulty level unlocked per Archetype by winning Runs.
_Avoid_: Difficulty, heat, tier

**Node**:
One stop in a Run's map; the player resolves exactly one Node per step.
_Avoid_: Room, encounter, location

**Node type**:
The kind of a Node: Combat, Elite, Ambush, Boss, Event, Shop, Rest or Treasure.
_Avoid_: Node kind, room type

**Depth**:
How many steps below Start a Node sits; drives enemy strength and reward size.
_Avoid_: Floor, level, layer

**Path lock**:
The rule that only the current Node's successors are offered, and choosing one forfeits the others.
_Avoid_: Fog, lock-in

**Event**:
A Node with a story choice that may lead to a special fight (battle station, third-party battle, convoy raid) or a setback.
_Avoid_: Random encounter, question mark

**Shop**:
A Node where Supplies buy ships, weapons, officers and repairs, once per visit.
_Avoid_: Store, merchant, market

**Rest**:
A Node offering one free benefit: full fleet repair, an S-mod, or an officer level.
_Avoid_: Campfire, camp, repair node

**Treasure**:
A Node that grants one rare item (rare ship, rare weapon or an S-mod) with no fight.
_Avoid_: Chest, loot room

### Fleets

**Run fleet**:
The temporary fleet the player fights with during a Run; it is discarded when the Run ends.
_Avoid_: Deck, active fleet

**Home fleet**:
The player's real campaign fleet, stored safely while a Run is in progress and returned after.
_Avoid_: Original fleet, stash, main fleet

**Fleet DP cap**:
The Run fleet's total deployment points limit, growing with each Act: above the soft cap every fight reward loses Supplies per DP over; above the hard cap no new course can be set.
_Avoid_: Fleet limit, deck size

**Archetype**:
The faction a Run is played as; it sets the ship pool for the Starting pick and Rewards.
_Avoid_: Character, class, start

**Starting pick**:
The player's choice of a 30 DP opening Run fleet from the Archetype's ships.
_Avoid_: Starting fleet, loadout

**Reward pick**:
The choice of one item out of three (ship, officer or weapon bundle) offered after a won fight; never an S-mod.
_Avoid_: Loot, recruit, card reward

**Supplies**:
The Run's only currency and repair resource, kept apart from the player's campaign supplies.
_Avoid_: Gold, run credits, money

### Persistence

**Meta progress**:
Progress that survives every Run (wins, best Depth, unlocks).
_Avoid_: Meta, profile, save data

**Payout**:
The campaign credits granted when a Run ends, scaled by Depth reached; the Run's only effect on the campaign.
_Avoid_: Reward, prize

**Unlock**:
A permanent addition to Meta progress, such as a hull available as a starting ship.
_Avoid_: Reward, achievement

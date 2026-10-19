# Slay the Sector

A roguelite layered on top of an intact Starsector campaign: the player leaves the normal sector
for short, self-contained runs and keeps permanent progress between them.

## Language

### The run

**Run**:
One self-contained roguelite attempt, from leaving the Hub to winning or losing.
_Avoid_: Descent, dive, run system, expedition

**Hub**:
The vanilla market (Jangala) where a Run is started and where the player is when it ends.
_Avoid_: Base, home system

**Node**:
One stop in a Run's map; the player resolves exactly one Node per step.
_Avoid_: Room, encounter, location

**Node type**:
The kind of a Node: Start, Combat, Elite, Event, Shop or Boss.
_Avoid_: Node kind, room type

**Depth**:
How many steps below Start a Node sits; drives enemy strength and reward size.
_Avoid_: Floor, level, layer, act

**Path lock**:
The rule that only the current Node's successors are offered, and choosing one forfeits the others.
_Avoid_: Fog, lock-in

### Fleets

**Run fleet**:
The temporary fleet the player fights with during a Run; it is discarded when the Run ends.
_Avoid_: Deck, active fleet

**Home fleet**:
The player's real campaign fleet, stored safely while a Run is in progress and returned after.
_Avoid_: Original fleet, stash, main fleet

**Reward pick**:
The choice of one ship, from three of the defeated faction's, offered after a non-boss Combat win.
_Avoid_: Loot, recruit, card reward

### Persistence

**Meta progress**:
Progress that survives every Run (wins, best Depth, unlocks).
_Avoid_: Meta, profile, save data

**Unlock**:
A permanent addition to Meta progress, such as a hull available as a starting ship.
_Avoid_: Reward, achievement

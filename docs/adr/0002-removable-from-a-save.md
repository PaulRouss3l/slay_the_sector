# 0002 — The mod can be removed from a save, after a cleanup step

Date: 2026-10-30. Status: accepted.

## Context

Players asked whether the mod can be removed. A save that has used it would not load without it:
the save holds our intel classes (`STS_HubIntel`, `STS_RunIntel`, `STS_RunResultIntel`), the Descent
Relay (custom entity type `sts_hub_station`) in a core system, the Descent star system with its
beacons, our sector memory keys, and the factions `sts_descent` and `sts_ally`. Missing classes give
`CannotResolveClassException` on load; missing entity specs are expected to fail too.

## Options

- **Store nothing removable-hostile**: keep intel and entities out of the save (transient intel,
  re-created each load). Not possible for the Relay, a real entity the player flies to, nor for the
  Run state and the stashed Home fleet while a Run is going.
- **Cleanup step before removal**: the player triggers it with no Run going, saves, then disables
  the mod.
- **`"utility": true`** in `mod_info.json`: only changes how the launcher treats the mod; the save
  still holds our classes.

## Decision

Cleanup step (`ModRemoval`), triggered by `"sts_prepareRemoval": true` in `data/config/settings.json`
(applies on the next load, no other mod needed) or by `sts remove` in Console Commands. It refuses
while a Run is active or the Home fleet is stashed, then removes every intel entry of our package,
every custom entity whose type starts with `sts_`, fleets under our two factions, the Descent
(`removeStarSystemNextFrame`), every `$sts_` key in sector and player memory, and our campaign
plugin. On a removal load nothing of ours is re-added. `scripts/save_residue.py` scans a save's
`campaign.xml` and fails if anything known to break loading is left.

## Consequences

- **New rule:** anything new the mod puts in a save (class, entity type, hullmod on a ship that
  can come home, memory key) must be covered by `ModRemoval` and by the scanner's patterns.
- **Factions stay**: the API has no way to remove a faction, so `sts_descent` and `sts_ally` remain
  in the sector's faction map. Verified in game (2026-10-30): a cleaned save loads with the mod
  disabled despite them, so the scanner lists them as harmless residue.
- Removal is one-way for that save until the next load with the setting off: the Relay and its
  intel come back then, but Meta progress lives in `saves/common/` and is not touched.

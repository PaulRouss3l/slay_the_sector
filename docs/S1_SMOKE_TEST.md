# S1 smoke test (~30-45 min): one Act, Combat + Boss

**Setup**: `./build.sh`, mod enabled, **new game** (saves from older builds are not supported).
Afterwards run `python3 tools/log_scan.py --mod .` and send it with your notes. Mod log lines
start with `[SlayTheSector]`. Tunables are in `data/config/settings.json` (`sts_*`).

## 1. Hub
- [x] On load, a **Descent Relay** station orbits the star of the most populated core system
      (log: `Hub placed in …`). Find it (intel/map search or fly there).
- [x] Clicking it opens the Hub menu: **Begin a Run**, Leave.
- [x] Intel screen, **Important** tab: a **Descent Relay** entry (also in existing saves).
      **Show on map** centres the map on the relay; **Lay in course** plots a course to it.
- [x] New game only: the relay sprite stays upright (no spin) and its orbit clears the planets.

## 2. Archetype + Starting pick
- [x] **Begin a Run** lists factions (vanilla + modded ones with ships). "More factions..." pages.
- [x] Picking one opens the **card grid**: ship cards with previews, DP counter `x / 30`,
      `+ Add` disabled when it would exceed 30, Prev/Next pages, Cancel returns to the list.
- [x] Confirm → message about the Home fleet docking. Your fleet is now the picked ships,
      150 supplies, crew, fuel, 1 unassigned officer (assign it in the Fleet screen).
- [x] **Refit the Run fleet** opens Refit; no market buying/selling. Note whether hullmod
      changes are allowed here.
- [x] **Jump into the Descent** → jump to "The Descent", arrive at the Entry Beacon, camera OK.

## 3. Choosing the next Node
- [x] At the Entry Beacon: 2-3 "Set course: Combat — <Faction> (Depth 1)" options.
- [x] Picking one closes the dialog and lays in a course to a new beacon ~3500 units away.
- [x] Transverse jump unusable. **Supplies don't drop while flying** (hidden logistics hullmod).
- [x] The Entry Beacon and Node beacons show a nav-buoy sprite and a name in space and on the map.

## 4. Combat Node
- [x] At the beacon: description (faction, Depth, strength) → **Engage** → vanilla battle.
- [x] Depth 1 is winnable with 30 DP (tune `sts_enemyBaseFP` / `sts_enemyFPPerDepth` if not).
- [x] After a win: vanilla loot/recovery screens (salvage goes to Run cargo, no credits),
      then "Victory … 30 supplies", then the **Reward pick** grid: 2 ships + officer or weapons.
      Pick one / Skip; it's applied.
- [x] Back at the Node: Supplies + ship count, next courses (Depth 2), Fleet services, Refit.
- [x] CR after the fight is at least ~70 %; hull damage stays.
- [x] After several won fights the fleet is **not over capacity**: no salvaged fuel, fuel and
      crew capped at capacity, each Run ship has +300 cargo (`sts_cargoPerShip`).

## 5. Fleet services
- [x] Repair one ship / Repair all costs supplies; disabled when you can't afford it.
- [x] Sell a non-flagship ship → supplies added.

## 6. Retreat / save-load
- [x] Engage a fight and retreat: "You withdrew … no reward", the Run continues from that Node.
- [x] Once a course is set it can't be changed (the Node offers only "Resume course").
- [x] Save and load mid-Run (in the Run system, no dialog open): everything intact.

## 7. Boss and win
- [x] Reach the Boss (Depth 7, Remnant). Its description warns there is no retreat.
- [ ] Every fight (Boss included) starts with each ship at ≥ ~70 % CR, even after a refit.
- [ ] Win → "The Run is won", Payout 50,000 → **Return to the Hub** → jump to the relay,
      **Home fleet back** (ships, officers, cargo) + 50,000 credits. Run fleet gone.

## 8. Loss paths (any order, new Runs)
- [x] **Abandon the Run** (at a Node or the Hub) → loss screen, Payout by deepest Node **won**
      (0 before Depth 3).
- [x] **Campaign untouched**: after a Run, your character's XP/level/story points, veteran
      marines, colony income, faction reputations and commission bounties are as before (apart
      from the Payout). Enemy fleets show as "The Descent" in the battle screens.
- [x] **Fleet wipe** → our "Run fleet destroyed" screen instead of vanilla's → Hub, Home fleet back.
- [x] After every end: Begin a Run works again; no leftover beacons from the old Run.

Note anything weird about officers (duplicated, missing) or the player's captain after a Run.

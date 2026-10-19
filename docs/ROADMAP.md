# ROADMAP — Slay the Sector

Each milestone is testable in game and ends with `./build.sh --strict` at 0 errors plus a
smoke-test list for the human tester. Design: `DESIGN.md`. Terms: `GLOSSARY.md`.

**Keep this file current**: when a feature lands, tick it and update the milestone status line.

Status legend: ⬜ planned · 🟡 code-complete, needs in-game test · ✅ verified in game.

**Current target: S2** — code-complete, awaiting the in-game smoke test (`S2_SMOKE_TEST.md`).

## Done before the redesign

- 🟡 **Tooling + release pipeline** (PR #1): `starsector-modding` skill adopted; build/validate/log-scan/
  package vendored in `tools/`; Version Checker file + CHANGELOG.
- 🗑️ **M0-M5 (old designs)**: isolated-system teleport, then dialog-only Run. Superseded; see
  `DESIGN.md` "History" and `adr/0001-run-in-contained-system.md`.

## Spike — prove the risky mechanisms ✅

Throwaway code in `src/com/slaythesector/spike/`; in-game checklist: `SPIKE_SMOKE_TEST.md`.
- ✅ Jump transition from the Hub into a contained Run system and back, no camera bug; `fracture_jump` blocked.
- ✅ Own `BattleCreationPlugin` picked for an enemy fleet tagged `$sts_scenario` (no objectives, no enemy retreat, fight to the last).
- ✅ Fleet wipe intercepted via `pickRespawnPlugin` → Run ends cleanly.
- ✅ Custom canvas (`showCustomVisualDialog`) draws nodes/edges and receives clicks.
- ✅ Home fleet + cargo detached into Run state survive save/load and restore intact (officers included).

Smoke-tested in game 2026-10-19 (RC8, with Nexerelin/RAT/MagicLib loaded); no mod errors in the log.
Carried into S1:
- Vanilla post-battle loot/recovery screens after a *won* tagged fight: not yet seen (test fight was
  too strong: 7 pirate ships at 40 FP vs Wolf + Hammerhead).
- `getContainingLocation()` stays set after `removeEntity` — check `loc.getFleets().contains(fleet)`
  before re-adding a swapped fleet.

## S1 — One Act, Combat + Boss ✅

Smoke-tested in game 2026-10-19 (RC8, Nexerelin/RAT/MagicLib loaded), checklist
`S1_SMOKE_TEST.md`; no mod errors in the log.
- ✅ Hub station (no market), Hub Intel entry (Important, Show on map, Lay in course), Hub dialog,
  Archetype list (runtime faction filter).
- ✅ Starting pick (card grid, 30 DP), free refit (weapon stock deferred).
- ✅ Run system with Node entities (beacon sprite), course choice as dialog options (map canvas in
  S2), Path lock.
- ✅ Combat and Boss Nodes via the vanilla fleet encounter flow; vanilla loot/recovery after a win.
- ✅ Reward pick (card grid: one ship of the defeated faction, one of the Archetype, plus officer or
  weapons), Supplies, attrition (CR floor `sts_crRegenFraction` after and before each fight,
  crew/fuel refill, logistics hullmod, no over-capacity drain).
- ✅ Fleet services (repair/sell), retreat, save/load mid-Run, Abandon, fleet-wipe intercept, Run end
  + Payout, Home fleet restored, campaign untouched.
- 🟡 Boss **win** path (Payout 50,000, Home fleet back): not seen — the tester lost the Boss.
  Boss strength lowered afterwards (`sts_bossFPMult` 2.0 → 1.65, ~114 → ~94 FP at Depth 7).

Known, not fixed (review, low impact): debris/wrecks inside a Run still yield supplies via
scavenging; core's `xpAtLastStoryPointGain` has no API, so a max-level player who earns story
points in a Run re-earns that XP afterwards; an AI fleet already pursuing the player at the Hub
may target the parked Home fleet (untested). One unexplained 0 % CR at a Boss start (not
reproduced after reload; covered by the pre-fight CR floor).

## S2 — Map and economy Nodes 🟡

Code-complete on branch `s2/map-economy`; in-game checklist: `S2_SMOKE_TEST.md`.
- 🟡 Act layout with StS placement rules: rows 1-2 Combat, middle row Treasure, row before the
  Boss Rest, other rows weighted Combat/Shop/Rest (`sts_nodeWeight*`), ≥ 1 Shop, no Rest in row 3.
- 🟡 Node map canvas (GL lines + vanilla intel icons), hover info, click + "Set course"; replaces
  the course text options. Read-only copy as the large description of a Run Intel entry.
- 🟡 Shop: card grid in budget mode priced in Supplies (ships one size above the Reward cap,
  weapons, an officer); dockyard repair at half price; stock gone once you leave.
- 🟡 Rest: one of free full repair / S-mod / officer +1 level (skill picked in the vanilla Fleet screen).
- 🟡 Treasure: one of a rare (bigger) ship, top-tier weapons, an S-mod.
- 🟡 Fleet DP cap: soft cap `sts_fleetDPCap` (Act 1 = 80), each DP over cuts fight rewards by
  `sts_overCapSuppliesPerDP` (5); hard cap = soft + `sts_fleetDPHardMargin` (30) blocks setting or
  resuming a course until ships are sold. Warnings on the beacon, encounter, map, grids and Intel.

Independent review done and its findings fixed (DP cap bypass via Resume course, repeat
purchases, wasted officer training, payroll charged to the Home fleet, GL state, course set from
inside the map's dismissal). Salaries made exact: Run-fleet pay is refunded and the Home fleet's
normal pay still charged at Run end (`RunPayrollListener`).

## S3 — Special fights and Events ⬜

Elite (Escape), Ambush (marked target, enemy flees), Events: battle station, sabotage, side
battle with allies, convoy raid.

## S4 — Full Run and Meta progress ⬜

3 Acts with Act transitions, enemy faction draw per Act, Boss pool, cross-save Meta progress,
Ascension per Archetype, starting bonuses.

## S5 — Polish and release ⬜

Balance pass, descriptions/flavor, settings exposure, `package_mod.py --strict`, release.

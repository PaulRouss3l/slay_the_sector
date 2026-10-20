# ROADMAP — Slay the Sector

Each milestone is testable in game and ends with `./build.sh --strict` at 0 errors plus a
smoke-test list for the human tester. Design: `DESIGN.md`. Terms: `GLOSSARY.md`.

**Keep this file current**: when a feature lands, tick it and update the milestone status line.

Status legend: ⬜ planned · 🟡 code-complete, needs in-game test · ✅ verified in game.

**Current target: S4** — code-complete, awaiting the in-game smoke test (`S4_SMOKE_TEST.md`).

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
- ✅ Boss **win** path (Payout 50,000, Home fleet back): seen in game 2026-10-19 (S3 build).
  Boss strength lowered afterwards (`sts_bossFPMult` 2.0 → 1.65, ~114 → ~94 FP at Depth 7).

Known, not fixed (review, low impact): debris/wrecks inside a Run still yield supplies via
scavenging; core's `xpAtLastStoryPointGain` has no API, so a max-level player who earns story
points in a Run re-earns that XP afterwards; an AI fleet already pursuing the player at the Hub
may target the parked Home fleet (untested). One unexplained 0 % CR at a Boss start (not
reproduced after reload; covered by the pre-fight CR floor).

## S2 — Map and economy Nodes ✅

Smoke-tested in game 2026-10-19 (RC8, Nexerelin/RAT/MagicLib loaded), checklist `S2_SMOKE_TEST.md`;
no mod errors in the log. The Boss win path was confirmed later, in the S3 test.
- ✅ Act layout generated like Slay the Spire (grid + non-crossing paths, 10 rows + Boss) with its
  placement rules (see DESIGN "Map"); checked offline on 5,000 maps. Enemy strength per Depth
  4.5 FP; Reward ships: destroyers from Depth 4, cruisers from Depth 8.
- ✅ Boss eased after the S2 playtest: no officers or AI cores (`sts_bossOfficers` = none / gamma /
  normal), `sts_bossFPMult` 1.65 → 1.3 (~78 FP at Depth 11).
- ✅ Node map canvas (GL lines + vanilla intel icons), hover info, click + "Set course"; replaces
  the course text options. Read-only copy as the large description of a Run Intel entry.
- ✅ Shop: card grid in budget mode priced in Supplies (ships one size above the Reward cap,
  weapons, an officer); dockyard repair at half price; stock gone once you leave.
- ✅ Rest: one of free full repair / S-mod / officer +1 level (skill picked in the vanilla Fleet screen).
- ✅ Treasure: one of a rare (bigger) ship, top-tier weapons, an S-mod.
- ✅ Fleet DP cap: soft cap `sts_fleetDPCap` (Act 1 = 80), each DP over cuts fight rewards by
  `sts_overCapSuppliesPerDP` (5); hard cap = soft + `sts_fleetDPHardMargin` (30) blocks setting or
  resuming a course until ships are sold. Warnings on the beacon, encounter, map, grids and Intel.

Independent review done and its findings fixed (DP cap bypass via Resume course, repeat
purchases, wasted officer training, payroll charged to the Home fleet, GL state, course set from
inside the map's dismissal). Salaries made exact: Run-fleet pay is refunded and the Home fleet's
normal pay still charged at Run end (`RunPayrollListener`).

## S3 — Special fights and Events ✅

Tested in game 2026-10-19 (RC8, Nexerelin/RAT/MagicLib loaded): the new Nodes work, the Boss
was beaten (Payout 50,000); log shows both `battle setup: escape` and `pursuit`, no mod errors.
- ✅ Map: Elite, Ambush and Event Node types with weights and placement rules (checked offline on
  5,000 maps), icons and legend.
- ✅ Escape/pursuit battle setups via our `BattleCreationPlugin` (rebuilt context goals).
- ✅ Elite (break out with ≥ 60 % DP), Ambush (marked flagship, ring + status in combat).
- ✅ Events: battle station, sabotage, side battle with an allied fleet, convoy raid.
- ✅ Beacon menu "Repair the fleet" + "Refit" label; D-mod removal (Rest, Treasure, Shop dockyard).
- ✅ A won straight battle as a total victory (`RunEncounterDialog`: survivors let go, no
  re-engagement); reputation unchanged after a side battle.

Independent review done and its findings fixed (pursuit "win" counted the mark/convoy as taken,
Elite scored on undeployed ships, a non-station hull at low Depth, layout written to a copy of the
battle context). Known: the Event and the sabotage roll can be re-rolled by reloading a save made
before arriving (save-scumming is accepted).

## S4 — Full Run and Meta progress 🟡

Design settled 2026-10-19 (DESIGN "Acts and Bosses", "Ascension and Meta progress"). Branch `s4/full-run`.
- 🟡 3 Acts: Act transition after the Boss, fresh map, enemy factions per Act, Act difficulty factor.
- 🟡 Bosses per Act (Apex / Derelict / Remnant) and the Boss reward choice (S-mod slot + S-mod,
  +20 DP cap, Remnant warship with an AI core).
- 🟡 Ascension Gamma → Omega per Archetype, its handicaps and Payout scaling.
- 🟡 Act 4 at Omega: Rest + true Boss (lone Ziggurat, `sts_trueBossHull`).
- 🟡 Cross-save Meta progress: Records, Ascension reached, starting bonuses; Hub Records page.

Independent review done and its findings fixed: spawned stations and the true Boss tagged the
shared stock variant (now a copy, also fixing an S3 leak), Primordial now gives every Boss pilots
(Gamma cores for Derelict/Remnant, officers for the Apex fleet), the Boss-reward free S-mod stays
claimable from the beacon menu until used, Act 4 texts. Known, low impact: a one-Act Run started
on an S1-era map continues into Act 2 with its Start beside the player rather than under it;
`repairFraction` (Seraph Rest) semantics to confirm in game.

## S5 — Polish and release ⬜

Balance pass, descriptions/flavor, settings exposure, `package_mod.py --strict`, release.

# ROADMAP — Slay the Sector

Each milestone is testable in game and ends with `./build.sh --strict` at 0 errors plus a
smoke-test list for the human tester. Design: `DESIGN.md`. Terms: `GLOSSARY.md`.

**Keep this file current**: when a feature lands, tick it and update the milestone status line.

Status legend: ⬜ planned · 🟡 code-complete, needs in-game test · ✅ verified in game.

**Current target: S1** — code-complete, awaiting the in-game smoke test.

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

## S1 — One Act, Combat + Boss 🟡

Code-complete on branch `s1/one-act`; in-game checklist: `S1_SMOKE_TEST.md`. Independent review
done and its findings fixed (campaign leaks, Payout exploit).
- 🟡 Hub station (idempotent placement, no market), Hub dialog, Archetype list (runtime faction filter).
- 🟡 Starting pick (card grid, 30 DP), free refit (weapon stock deferred).
- 🟡 Run system with Node entities, course choice as dialog options (map canvas in S2), Path lock.
- 🟡 Combat and Boss Nodes via the vanilla fleet encounter flow.
- 🟡 Reward pick (card grid), Supplies, attrition (CR to ~70 %, crew/fuel refill, logistics hullmod).
- 🟡 Fleet services (repair/sell), Run end + Payout, fleet-wipe intercept, Restore safety net.
- 🟡 Zero campaign impact enforced (Descent faction, stat snapshot, credits hand-back), XStream aliases.

Known, not fixed (review, low impact): debris/wrecks inside a Run still yield supplies via
scavenging; core's `xpAtLastStoryPointGain` has no API, so a max-level player who earns story
points in a Run re-earns that XP afterwards; an AI fleet already pursuing the player at the Hub
may target the parked Home fleet (untested); the Hub's fixed 2600-unit orbit may overlap a planet.

## S2 — Map and economy Nodes ⬜

Node map canvas + Intel overview, Shop, Rest, Treasure, Fleet DP cap.

## S3 — Special fights and Events ⬜

Elite (Escape), Ambush (marked target, enemy flees), Events: battle station, sabotage, side
battle with allies, convoy raid.

## S4 — Full Run and Meta progress ⬜

3 Acts with Act transitions, enemy faction draw per Act, Boss pool, cross-save Meta progress,
Ascension per Archetype, starting bonuses.

## S5 — Polish and release ⬜

Balance pass, descriptions/flavor, settings exposure, `package_mod.py --strict`, release.

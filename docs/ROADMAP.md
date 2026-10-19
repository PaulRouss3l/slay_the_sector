# ROADMAP — Slay the Sector

Each milestone is testable in game and ends with `./build.sh --strict` at 0 errors plus a
smoke-test list for the human tester. Design: `DESIGN.md`. Terms: `GLOSSARY.md`.

**Keep this file current**: when a feature lands, tick it and update the milestone status line.

Status legend: ⬜ todo · 🟡 code-complete, needs in-game test · ✅ verified in game.

**Current target: Spike.**

## Done before the redesign

- ✅ **Tooling + release pipeline**: `starsector-modding` skill adopted; build/validate/log-scan/
  package vendored in `tools/`; Version Checker file + CHANGELOG.
- 🗑️ **M0-M5 (old designs)**: isolated-system teleport, then dialog-only Run. Superseded; see
  `DESIGN.md` "History" and `adr/0001-run-in-contained-system.md`.

## Spike — prove the risky mechanisms ⬜

Throwaway-quality code allowed; each item gets a smoke-test line.
- ⬜ Jump transition from the Hub into a contained Run system and back, no camera bug; `fracture_jump` blocked.
- ⬜ Own `BattleCreationPlugin` picked for an enemy fleet tagged `$sts_scenario` (map size, no objectives, fight to the last).
- ⬜ Fleet wipe intercepted via `pickRespawnPlugin` → Run ends cleanly.
- ⬜ Custom canvas (`showCustomVisualDialog`) draws nodes/edges and receives clicks.
- ⬜ Home fleet + cargo detached into Run state survive save/load and restore intact (officers included).

## S1 — One Act, Combat + Boss ⬜

Hub station (idempotent), Hub dialog, Archetype list, Starting pick (card grid, 30 DP), free refit,
Run system with Node entities, text node choice (map canvas comes in S2), Combat and Boss Nodes
via the vanilla fleet encounter, Reward pick (card grid), Supplies, attrition (CR regen ~70 %,
crew refill), Fleet services (repair/sell), Run end + Payout, Restore safety net, XStream aliases.

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

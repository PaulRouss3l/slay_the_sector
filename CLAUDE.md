# CLAUDE.md — Slay the Sector

Context router for AI sessions. Keep this file lean; detail lives in `docs/`.

## What this is

A **Starsector 0.98a** total-conversion mod that turns the game into a **roguelite** in the
style of *Slay the Spire*. **Java only** (no Kotlin). Domain terms: `GLOSSARY.md`.

## Build / run

- Build: `./build.sh` → compiles `src/` into `jars/SlayTheSector.jar`, then runs the static
  validator. Must report **0 errors** after every change. `./build.sh --strict` before a release.
- Run (debug, attach on `:5005`): `./run.sh`. In VSCode: F5 "Debug Starsector".
- After an in-game test: `python3 tools/log_scan.py --mod .` triages `starsector.log`.
- Release: bump version in `mod_info.json` + `slay_the_sector.version` + `CHANGELOG.md`, then
  `python3 tools/package_mod.py --strict` → `dist/`.
- Game install: `/home/ubik/Downloads/starsector` (override with `STARSECTOR_DIR`). The repo is
  symlinked into `<install>/mods/slay-the-sector` (see README).
- Entry class: `com.slaythesector.SlayTheSectorModPlugin` (per `mod_info.json`).

## Tooling

- `tools/` is **vendored** from the `starsector-modding` skill (see `tools/UPSTREAM.md`). Do not
  edit those scripts; configure via `tools/validate_config.json`.
- The full skill is installed at `~/.claude/skills/starsector-modding`. Use its
  `scripts/api_lookup.py` (`class|method|const|uses|grep|rulecmd|javap`) to check any API call
  before using it, and its `references/pitfalls.md` as the review checklist.

## Architecture (one paragraph)

Design: `docs/DESIGN.md`, ADR `docs/adr/0001-*`; progress: `docs/ROADMAP.md`. A
**Run** lives in a **contained star system** inside a normal campaign, entered from the mod's
own **Hub** station by a jump transition. On start, the Home fleet and cargo are detached into
saved Run state and the player fleet becomes the Run fleet (Starting pick, 30 DP). The player
picks the next Node on a custom node-map canvas, flies to it, and fights through the vanilla
fleet encounter flow with a per-Node-type battle setup. Supplies are the Run's only currency.
Run end restores the Home fleet and grants a Payout; Meta progress is cross-save. `src/` holds
S1 (one Act, Combat + Boss, text course choice); later milestones add to it.

## Hard constraints

- **Java only.** Do not introduce Kotlin (RAT is Kotlin; we only borrow its patterns).
- **Never wipe / regenerate the vanilla sector.** The mod is purely additive.
- **Never relocate the player fleet from inside an open dialog** (camera gets stuck; this is why
  the system/teleport design was scrapped).
- **Zero campaign impact**: a Run must never change the player's campaign except the Payout.
  Every Run start/end swaps fleet and cargo exactly once.
- **State storage**: run state → sector memory key `$sts_run_state` (per-save, dies with the run).
  Meta progress → cross-save JSON in `saves/common/` (`writeJSONToCommon`), never in the save.
- **XStream-safe serialization**: saved state holds ids and plain `Serializable` POJOs, not
  references to live world objects (`SectorEntityToken`s, fleets in a location). The one deliberate
  exception is the detached Home fleet + cargo held during a Run (see ADR 0001). No static fields
  holding game objects. Alias saved classes in `configureXStream`; never rename them after release.

## Workflow

- **When a feature is done, update `docs/ROADMAP.md`** in the same change: tick the item
  (⬜ → 🟡 code-complete, ✅ verified in game) and the "Current target" line.
- New domain terms go in `GLOSSARY.md`; hard-to-reverse design choices get an ADR in `docs/adr/`.
- **Logging** (`STSLog`, ships to players' `starsector.log`): log events, never loops. Info lines for
  rare milestones only (Run start/end, jumps, special battle setups); `warn` for real anomalies.
  Nothing in `advance()`, rendering or per-tick code. Verbose tracing, if ever needed, goes behind
  an `sts_debugLog` setting that defaults to off.

## Pointers

- `GLOSSARY.md` — canonical domain terms.
- `docs/DESIGN.md` — settled design: step → screen map, Node types, rules, planned packages.
- `docs/adr/` — architecture decisions.
- `docs/ROADMAP.md` — milestones and status.
- `docs/API_NOTES.md` — verified Starsector API facts + pitfalls relevant to this mod.

## Reference mods

- **RAT** — `github.com/Lukas22041/Random-Assortment-of-Things`, package `abyss/` (original
  inspiration). API docs mirror: `https://jaghaimo.github.io/starsector-api/`.
- Nexerelin (`Histidine91/Nexerelin`), SecondInCommand (`Lukas22041/SecondInCommand`) — examples.

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

The whole **run** plays out inside **one interaction dialog** (`dialog/RunDialogPlugin`),
hijacked from the Jangala market menu via `dialog.setPlugin(...)`. No custom star system, no
fleet movement, no node entities. The player picks among the next nodes as text options (fog of
war: only immediate successors shown; picking one locks the path). Combat nodes call
`dialog.startBattle(...)`. The home fleet waits in the mothball bay while a temporary run fleet
fights. Win or loss ends the run and restores the home fleet; **meta progress persists** across
runs. A graphical node map comes later (M6). Full flow: `docs/DESIGN.md` "CURRENT ARCHITECTURE".

## Hard constraints

- **Java only.** Do not introduce Kotlin (RAT is Kotlin; we only borrow its patterns).
- **Never wipe / regenerate the vanilla sector.** The mod is purely additive.
- **Never relocate the player fleet from inside an open dialog** (camera gets stuck; this is why
  the system/teleport design was scrapped).
- **State storage**: run state → sector memory key `$sts_run_state` (per-save, dies with the run).
  Meta progress → persistent data key `sts_meta` (**survives runs** — ending a run must never
  touch it).
- **XStream-safe serialization**: saved state holds ids and plain `Serializable` POJOs only, never
  game objects (`SectorEntityToken`, `FleetMemberAPI`, …). No static fields holding game objects.
  Never rename a saved class after release.

## Pointers

- `GLOSSARY.md` — canonical domain terms.
- `docs/DESIGN.md` — current architecture (top), superseded system design (below).
- `docs/ROADMAP.md` — milestones and status.
- `docs/API_NOTES.md` — verified Starsector API facts + pitfalls relevant to this mod.

## Reference mods

- **RAT** — `github.com/Lukas22041/Random-Assortment-of-Things`, package `abyss/` (original
  inspiration). API docs mirror: `https://jaghaimo.github.io/starsector-api/`.
- Nexerelin (`Histidine91/Nexerelin`), SecondInCommand (`Lukas22041/SecondInCommand`) — examples.

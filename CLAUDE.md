# CLAUDE.md — Slay the Sector

Context router for AI sessions. Keep this file lean; detail lives in `docs/`.

## What this is

A **Starsector 0.98a** total-conversion mod that turns the game into a **roguelite** in the
style of *Slay the Spire*. **Java only** (no Kotlin), built with plain `javac`.

## Build / run

- Build: `./build.sh` → `jars/SlayTheSector.jar` (compiles `src/` against the game jars).
- Run (debug, attach on `:5005`): `./run.sh`. In VSCode: F5 "Debug Starsector".
- Game install: `/home/ubik/Downloads/starsector`. The repo must be symlinked into
  `<install>/mods/slay-the-sector` for the launcher to load it (see README).
- Entry class: `com.slaythesector.SlayTheSectorModPlugin` (per `mod_info.json`).

## Architecture (one paragraph)

The roguelike **run** happens inside a custom, isolated `StarSystemAPI` the player enters from a
**hub** — the RAT "Abyssal Depths" model. The base sector is **left intact** (no wipe). Inside a
run: FTL is locked, the player descends **node-to-node** seeing only ~1 node ahead (fog of war),
fighting Combat/Elite/Boss nodes and visiting Event/Shop/Recruit nodes. Win or death ends the run
and returns to the hub; **roguelite meta unlocks persist** across runs. Node selection is a **text
menu** (interaction dialog) for now; a graphical map comes later.

## Hard constraints

- **Java only.** Do not introduce Kotlin (RAT is Kotlin; we only borrow its patterns).
- **Never wipe / regenerate the vanilla sector.** The run is an additive isolated system.
- **State storage**: run state → sector memory key `$sts_run_state` (per-save, dies with the run).
  Meta progression → persistent data key `sts_meta` (**survives runs** — clearing a run must never
  touch it).
- **XStream-safe serialization**: store node **ids** in saved state, not `SectorEntityToken`
  references; resolve tokens lazily by scanning for entity memory `$sts_node_id`. Keep saved
  POJOs plain `Serializable`.
- The transverse-jump ability id is **`fracture_jump`** (not `transverse_jump`).

## Pointers

- `docs/DESIGN.md` — package/class layout, data model, core flows.
- `docs/ROADMAP.md` — milestones M0–M6 (current target: M0).
- `docs/API_NOTES.md` — verified Starsector API facts + "must re-verify" checklist.

## Reference mods

- **RAT** — `github.com/Lukas22041/Random-Assortment-of-Things`, package `abyss/` (the contained
  run-layer model we emulate). API docs mirror: `https://jaghaimo.github.io/starsector-api/`.
- Nexerelin (`Histidine91/Nexerelin`), SecondInCommand (`Lukas22041/SecondInCommand`) — examples.

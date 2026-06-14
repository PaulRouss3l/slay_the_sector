# ROADMAP — Slay the Sector

Incremental milestones. Each is independently testable in-game; early ones produce a playable loop
ASAP. **Status: M0 done; M1–M5 code-complete (pending in-game test); current target: M6.**
Class/package names: see `DESIGN.md`. API calls: see `API_NOTES.md`.

> **MAJOR PIVOT (post-M5): dialog-only run.** The custom-system + teleport + node-entity
> approach was scrapped — relocating the player fleet from inside the hub dialog left the
> camera stuck (teleport never completed visually). The entire run now plays inside **one
> interaction dialog** (`dialog/RunDialogPlugin`), hijacked from the Jangala menu via
> `dialog.setPlugin(...)`. Node map = text option menus; combat = `dialog.startBattle(...)`.
> No star system, fleet movement, node entities, or FTL lockout. See DESIGN.md
> "CURRENT ARCHITECTURE". M1–M5 features (run, fog/path-lock, node variety, meta, intel) are
> all preserved in the dialog form.

## M0 — Build / run sanity ✅
Restructure into the `com.slaythesector` package tree; add `util/STSConstants` + `util/STSLog`.
- **Files**: existing `SlayTheSectorModPlugin`, new `util/STSConstants`, `util/STSLog`.
- **Verify**: `./build.sh` produces `jars/SlayTheSector.jar`; enable mod; new game logs
  "application loaded" + "game loaded"; `./run.sh` debug attaches on `:5005`.

## M1 — Hub + enter empty run system 🟡 (code-complete, needs in-game test)
Hub entity → "Descend" teleports into a generated, hidden, FTL-locked star system; "Return" → back
to the core sector intact. Routing via rules.csv (`$tag:sts_hub` / `$tag:sts_run_exit`) → rulecmds
that add options programmatically.
- **Files**: `hub/HubManager`, `rulecmd/STS_EnterRun`, `rulecmd/STS_AbandonRun`,
  `gen/RunSystemGenerator` (system only, no nodes), `travel/RunEntryController`,
  `travel/RunExitController`, `ftl/FtlLockScript`, `run/RunState` + `RunStateManager`,
  `data/config/settings.json`, `data/campaign/rules.csv`.
- **Verify**: enter → camera in new isolated system; Transverse Jump greyed/cooldown-pinned;
  cannot FTL out; "Return" lands back in the core sector with the vanilla sector untouched.

## M2 — One combat node (vertical slice) 🟡 (code-complete, needs in-game test)
Enter → single COMBAT node → forced battle vs pirates → win/lose returns to hub; loop repeats with a
fresh system. The minimum-fun loop. Implemented as a linear START→COMBAT→BOSS chain.
- **Files**: `gen/NodeGraphBuilder` (trivial: START→COMBAT→BOSS or one combat),
  `gen/NodeEntityFactory`, `gen/EncounterFleetFactory`, `travel/RunTeleporter`,
  `nodes/NodeHandlerRegistry` + `CombatNodeHandler`, `dialog/NodeSelectDialogPlugin` +
  `CombatNodeDialogPlugin`, `rulecmd/STS_OpenNodeMenu`, `util/DepthScaling`.
- **Verify**: enter → fleet at START → interact → menu "Go to: Combat" → battle → win → run ends →
  back at hub; lose → also back at hub; re-enter → fresh system.

## M3 — Descent + fog of war + node variety 🟡 (code-complete, needs in-game test)
Multi-depth branching DAG (8 layers); reveal only next nodes; add EVENT, SHOP, RECRUIT, ELITE, BOSS.
- **Files**: `NodeGraphBuilder` (seeded layered DAG, connectivity-guaranteed); `travel/NodeProgress`
  (commit-path/despawn-siblings + reveal-successors); `RunState` (revealed set + seed);
  `NodeInteractionDialogPlugin` (per-type effects inlined — combat/event/shop/recruit). Effects use
  `cargo.getCredits()` (MutableValue), `addSupplies/addFuel`, `createFleetMember`→`addFleetMember`.
- **Deviation**: node effects are inlined in the dialog, not separate `*NodeHandler` classes (less
  boilerplate; refactor to handlers if it grows).
- **Verify**: from START, 2–3 node entities appear ahead; flying to one despawns its siblings (path
  locks); combat gates progress (successors appear only after a win); event gives credits; shop
  spends credits for supplies/fuel; recruit adds a frigate; boss at the bottom ends the run.

## M4 — Meta-progression 🟡 (code-complete, needs in-game test)
Wins unlock ships + start bonuses persisted in `sts_meta`; new runs read unlocks; hub shows progress.
- **Files**: `run/MetaProgress` + `MetaProgressManager` (persistent `sts_meta`, unlock pool);
  `RunExitController` win/lose/abandon record win + reached depth; `RunEntryController` counts run +
  grants start bonus (supplies/credits scaled by prior wins); `NodeInteractionDialogPlugin` recruit
  offers unlocked variants; `STS_HubMenu` shows runs-won / best-depth / ships-unlocked.
- **Verify**: win a run → boss win unlocks next pool ship (log `meta: unlocked …`); reload save →
  stats persist; next run's recruit node lists the unlocked ship + start bonus applied; abandon/lose
  still records best depth; `sts_meta` survives `RunStateManager.clear()`.

## M5 — Run intel / text-map polish 🟡 (code-complete, needs in-game test)
`intel/RunIntelPlugin` (`BaseIntelPlugin`) showing current depth, node, cleared count, the node(s)
immediately ahead (text "map"), and meta stats. Added on run start, removed on run end, self-ends
when no run is active. Wired in `RunEntryController` (add) / `RunExitController` (remove).
- **Deviation**: skipped `descriptions.csv` (dialog flavor) — node text is inline in the dialog; add
  the csv later only if flavor needs externalizing.
- **Verify**: intel entry "Sector Descent" appears on descend; opening it shows depth + "Ahead" node
  types; updates as you advance; gone after the run ends (win/lose/abandon).

## M6 — Graphical node map (post-MVP)
Replace/augment the text menu with a custom `CustomUIPanelPlugin` map overlay.
- **Files**: new `ui/` package (`RunMapPanelPlugin`, …).
- **Verify**: visual map renders nodes/edges, click selects, falls back to text menu.

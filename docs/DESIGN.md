# DESIGN — Slay the Sector

See `API_NOTES.md` for the verified Starsector API calls referenced below.

## CURRENT ARCHITECTURE (dialog-only) — supersedes the system/teleport design below

The whole run plays out inside **one interaction dialog** — no custom star system, no fleet
movement, no node entities, no FTL lockout. This replaced the RAT-style "enter an isolated system"
approach because relocating the player fleet from inside the hub dialog left the camera/current-
location stuck (teleport never visually completed).

Flow:
- `rules.csv`: `PopulateOptions` on Jangala (`$market.id == jangala`) adds the "Descend into the
  Sector" option (order 99, just above Leave). Selecting it → `DialogOptionSelected $option ==
  sts_enter` → `rulecmd/STS_EnterRun`.
- `STS_EnterRun` **hijacks the open hub dialog**: `dialog.setPlugin(new RunDialogPlugin()); plugin.init(dialog);`
- `dialog/RunDialogPlugin` runs the entire descent: builds the graph (`NodeGraphBuilder`), shows the
  current node's successors as **text options** (fog = only immediate successors shown; picking one
  locks the path since siblings are never reoffered). Per node type:
  - combat/elite/boss → `dialog.startBattle(new BattleCreationContext(player, ATTACK, enemy, ATTACK))`
    with `enemy` from `EncounterFleetFactory` (parked on the player's location, despawned after);
    result handled in `backFromEngagement`.
  - event/shop/recruit → sub-option menus mutating cargo/credits/fleet.
  - boss win → `endRun(won)`; any loss/abandon → `endRun`. `endRun` records meta + ends the dialog.
- Run state still mirrored into sector memory (`RunStateManager`) so `intel/RunIntelPlugin` (persistent,
  Important, points to Jangala) can describe an active run.

**Run fleet (temporary):** at run start the player picks a starting ship (Wolf always; meta-unlocked
hulls add more). The real fleet is stashed into the **mothball bay** (`player.getCargo().getMothballedShips()`)
— it travels safely, never fights, and keeps the player "alive" so a wiped run fleet can't trigger the
"fleet destroyed" game-over. The active fleet is replaced with the chosen ship (flagship, player as
captain). Each non-boss combat win offers **3 ships of the faction just fought**, sized to the fight's
depth (`DepthScaling.rewardSize` → `gen/RewardShipFactory` generates a throwaway faction fleet and
buckets members by `HullSize`); the picked hull is added to the run fleet. At run end (`endRun`,
including loss — runs before the dialog dismisses) the run fleet is dropped and the originals pulled
back out of the mothball bay. Combat nodes name the faction in the path label + intro; boss is always
Remnants. No Recruit node (rewards replace it).

Kept classes: `dialog/RunDialogPlugin`, `gen/NodeGraphBuilder`, `gen/EncounterFleetFactory`,
`run/{RunState,RunStateManager,RunNode,NodeType,MetaProgress,MetaProgressManager}`,
`util/{DepthScaling,STSConstants,STSLog}`, `hub/HubManager`, `intel/RunIntelPlugin`,
`rulecmd/STS_EnterRun`.
Removed: RunSystemGenerator, NodeEntityFactory, NodeProgress, STSCampaignPlugin, FtlLockScript,
DeferredEnter/Exit, RunEntryController/RunExitController, NodeInteractionDialogPlugin,
STS_AbandonRun, STS_RunExitMenu.

---

## Original (superseded) system/teleport design

Mirrors RAT's Abyssal-Depths patterns (`github.com/Lukas22041/Random-Assortment-of-Things`, `abyss/`).

## Package / class layout

Root package `com.slaythesector` (under `src/com/slaythesector/`):

| Package | Class | Role |
|---|---|---|
| `` (root) | `SlayTheSectorModPlugin` | Lifecycle. `onGameLoad`: re-register persistent EveryFrameScripts (FTL lock, run watcher), build/repair hub. |
| `run/` | `RunState` | Memory singleton: node graph, current node, revealed successors, depth, `hasGenerated`. |
| `run/` | `RunStateManager` | Static accessor on sector memory `$sts_run_state`: `getOrCreate / store / clear`. |
| `run/` | `RunNode` | One node: `id`, `NodeType`, `depth`, `successorIds`, `cleared`; transient resolved entity. |
| `run/` | `NodeType` | enum: START, COMBAT, ELITE, EVENT, SHOP, RECRUIT, BOSS. |
| `run/` | `MetaProgress` | Unlocks (ships/captains/bonuses) + counters. |
| `run/` | `MetaProgressManager` | Static accessor on persistent `sts_meta`: `get / store / unlock / recordRunEnd`. |
| `gen/` | `RunSystemGenerator` | createStarSystem, init star, tag hidden + cut-off, place one entity per node. |
| `gen/` | `NodeGraphBuilder` | Pure layered-DAG logic (no API calls → unit-testable). |
| `gen/` | `NodeEntityFactory` | Creates a `CustomCampaignEntityAPI` per node, sets `$sts_node_id`, positions it. |
| `gen/` | `EncounterFleetFactory` | Builds enemy `CampaignFleetAPI` via `FleetFactoryV3`, depth-scaled. |
| `nodes/` | `NodeHandler` (iface) + `Combat/Elite/Boss/Event/Shop/Recruit` handlers, `NodeHandlerRegistry` | Per-type arrival/resolution behavior. |
| `travel/` | `RunEntryController` | hub → run (generate, switch location, place fleet, lock FTL). |
| `travel/` | `RunExitController` | run → hub (award meta, restore fleet, cleanup/remove system). |
| `travel/` | `RunTeleporter` | Teleport fleet to a node entity; update current node; reveal successors. |
| `ftl/` | `FtlLockScript` | EveryFrameScript: pin `fracture_jump` cooldown + set `$cfai_noJump` while in run. |
| `dialog/` | `NodeSelectDialogPlugin` | Text "Go to A / B / C" menu. |
| `dialog/` | `Combat/Event/Shop/Recruit` dialog plugins, `DialogUtil` | Per-type interaction dialogs + helpers. |
| `rulecmd/` | `STS_EnterRun`, `STS_OpenNodeMenu`, `STS_AbandonRun` | RuleCmds (package registered via `ruleCommandPackages`). |
| `intel/` | `RunIntelPlugin` | `BaseIntelPlugin`: current depth + text map. |
| `hub/` | `HubManager` | Idempotent hub entity in a vanilla system; attaches enter-run interaction. |
| `util/` | `STSConstants` | All string keys (memory/persistent keys, entity type ids, system name). |
| `util/` | `STSLog`, `DepthScaling` | Logger wrapper; depth → {fleet points, quality, loot} scalars. |

## Data model

- **Run state** → sector memory `$sts_run_state` (`getMemoryWithoutUpdate()`). Per-save; vanishes
  when the run ends. Store node **ids**, not entity refs — resolve `SectorEntityToken`s lazily by
  scanning the run system for entity memory `$sts_node_id == id` (avoids XStream serialization
  issues). `hasGenerated` guard: `getOrCreate()` returns existing if the key is present, else builds
  fresh.
- **Meta** → persistent data `sts_meta` (`getPersistentData()`). Survives runs.
  `RunStateManager.clear()` only unsets `$sts_run_state` / `$sts_in_run`; it **never** touches
  `sts_meta`.
- **Fog of war**: on arrival at node N → mark N cleared, add `N.successorIds` to `revealedNodeIds`.
  The node-select dialog only offers revealed-and-uncleared successors of the current node.

## Core flows

**Enter run** (`RunEntryController.enter`):
get/create `MetaProgress` → `RunSystemGenerator.generate(seed, meta)` (createStarSystem, init star,
add tags `SYSTEM_CUT_OFF_FROM_HYPER` + `THEME_HIDDEN` to center, **no** jump points, place node
entities) → build + store `RunState` (current = start, reveal start successors) →
`setCurrentLocation(sys)` + place player fleet at START → `addScript(new FtlLockScript())`, set
`$sts_in_run` → open START node menu.

**Travel** (`NodeSelectDialogPlugin` + `RunTeleporter`):
node interaction fires `STS_OpenNodeMenu` → dialog lists revealed-uncleared successors of current
node → on pick, `RunTeleporter` resolves target entity, `playerFleet.setLocation(...)`, updates
`currentNodeId`, adds target successors to `revealedNodeIds`, dispatches to
`NodeHandlerRegistry.get(type).onArrive(...)`.

**Combat** (`CombatNodeHandler` + `CombatNodeDialogPlugin`):
`EncounterFleetFactory.create(depth, meta)` → `system.spawnFleet(...)` at the node → pre-battle
dialog → on "Engage": `dialog.startBattle(new BattleCreationContext(player, FleetGoal.ATTACK, enemy,
FleetGoal.DEFEND))` → `backFromEngagement(result)`: win → clear node + advance (if BOSS →
`RunExitController.win()`); loss → `RunExitController.lose()`.

**End run** (`RunExitController`):
award/skip meta unlocks (persist to `sts_meta`) → teleport player to hub (`setCurrentLocation(hub)`,
place fleet) → `RunStateManager.clear()` + `removeStarSystemNextFrame(sys)`. `FtlLockScript` sees
`$sts_in_run` gone → `isDone()` unwinds it. Next `enter()` regenerates fresh.

## Data files (created with their milestones, not upfront)

- `data/config/settings.json`: `{ "ruleCommandPackages": ["com.slaythesector.rulecmd"] }`
- `data/campaign/rules.csv`: route node-entity interaction (`$sts_node`) → `STS_OpenNodeMenu`;
  hub option `sts_enter` → `STS_EnterRun`.
- MVP enemies use vanilla `Factions.PIRATES`; node entities reuse the `"stable_location"` type.

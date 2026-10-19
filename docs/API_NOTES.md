# API_NOTES — Starsector 0.98a API facts

Verified against `/home/ubik/Downloads/starsector/starfarer.api.jar` (v0.98a-RC5; install is now RC8). The official
Javadoc is paywalled; use the mirror `https://jaghaimo.github.io/starsector-api/`. API source is
also bundled at `<install>/starsector-core/starfarer.api.zip`.

## Verified facts

| Concern | Fact |
|---|---|
| Create run system | `Global.getSector().createStarSystem(name)`; cleanup `removeStarSystemNextFrame(sys)` |
| Isolate system | add tags `Tags.SYSTEM_CUT_OFF_FROM_HYPER`, `Tags.THEME_HIDDEN` to the system **center** token; do **not** call `autogenerateHyperspaceJumpPoints` (no jump points → no hyperspace link) |
| Transverse jump | id is `Abilities.TRANSVERSE_JUMP = "fracture_jump"` (NOT `"transverse_jump"`). Lock via `playerFleet.getAbility("fracture_jump").setCooldownLeft(big)` / `forceDisable()` |
| No-jump flag | `MemFlags.MEMORY_KEY_NO_JUMP = "$cfai_noJump"` set on player fleet memory |
| Teleport | `SectorEntityToken.setLocation(x, y)` (player fleet); `SectorAPI.setCurrentLocation(LocationAPI)` to switch systems |
| State storage | run → `Global.getSector().getMemoryWithoutUpdate()` (`MemoryAPI`); meta → `Global.getSector().getPersistentData()` (`Map<String,Object>`) |
| Spawn fleets | `FleetFactoryV3.createFleet(FleetParamsV3)`; place via `system.spawnFleet(anchor, relX, relY, fleet)` or `system.addEntity` |
| Battle | `new BattleCreationContext(player, FleetGoal.ATTACK, enemy, FleetGoal.ATTACK)` (4-arg ctor confirmed; `FleetGoal` only has **ATTACK / ESCAPE** — there is NO `DEFEND`); `dialog.startBattle(bcc)`; result via `backFromEngagement(EngagementResultAPI).didPlayerWin()` |
| Route interactions | register a `BaseCampaignPlugin` (override `getId`, `isTransient`→true, `pickInteractionDialogPlugin(token)` returning `new PluginPick<>(plugin, PickPriority.MOD_SPECIFIC)`); `sector.registerPlugin(...)` / `unregisterPlugin(id)`. Cleaner than rules.csv for dynamic node ids. |
| Fleet for battle | `FleetFactoryV3.createFleet(new FleetParamsV3(Vector2f, faction, qualityOverride, fleetType, combat, freighter, tanker, personnel, liner, utility, qualityMod))`; then `loc.addEntity(fleet)`, `fleet.setLocation(x,y)`, `fleet.inflateIfNeeded()` |
| Hijack a dialog | `dialog.setPlugin(new MyPlugin()); plugin.init(dialog)` — take over the current interaction dialog (e.g. turn a market menu into a custom flow). Avoids opening a nested dialog and avoids relocating the player fleet. |
| Scripted battle from dialog | spawn enemy fleet, `dialog.startBattle(new BattleCreationContext(player, FleetGoal.ATTACK, enemy, FleetGoal.ATTACK))`, handle `backFromEngagement(result).didPlayerWin()`, despawn the enemy after. No system/teleport needed. |
| ⚠️ Relocating player fleet mid-dialog | Does NOT work: `setContainingLocation`/`setCurrentLocation` from inside an open dialog leaves the camera stuck (even deferred a frame). Prefer running content **in** the dialog (above), or `doHyperspaceTransition` if a real location change is required. |
| Dialog | `InteractionDialogPlugin`; open with `Global.getSector().getCampaignUI().showInteractionDialog(plugin, target)`; `dialog.getOptionPanel().addOption(text, Object id)` |
| RuleCmd | extend `com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin`; register package in `data/config/settings.json` `ruleCommandPackages` |
| Node entity | `LocationAPI.addCustomEntity(id, name, "stable_location", factionId)` → `CustomCampaignEntityAPI`; tag with entity memory `$sts_node_id` |
| Fleet edits | `Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId)` → `fleet.getFleetData().addFleetMember(member)`; `createPerson()` for captains |
| Lifecycle order | `onApplicationLoad` → `onNewGame` → `onNewGameAfterProcGen` → `onNewGameAfterEconomyLoad` → `onNewGameAfterTimePass` → `onGameLoad(boolean)` |
| Intel | extend `BaseIntelPlugin`; `Global.getSector().getIntelManager().addIntel(plugin)` |
| EveryFrameScript | iface `isDone() / runWhilePaused() / advance(float)`; register `Global.getSector().addScript(...)` |

## Imported from the starsector-modding skill (curated for a dialog-only mod)

Source: `~/.claude/skills/starsector-modding/references/{api-notes,pitfalls}.md` @ `ee66319`.
Ship/weapon/sprite/balance material is left there; read it if run rewards ever add custom hulls.

**Rule zero: never write an API call from memory.** Check it first:
`python3 ~/.claude/skills/starsector-modding/scripts/api_lookup.py class|method|const|uses|grep|rulecmd|javap ...`
(searches the API + vanilla impl source in `starfarer.api.zip`; `--libs` adds MagicLib/LunaLib/...).

| Concern | Fact |
|---|---|
| Toolchain | Game JRE is Java 17 → `javac --release 17`. Never compile against the obfuscated jars. Script classloader rejects `java.io.File`/streams and reflection: use `Global.getSettings().loadJSON/loadCSV/readTextFileFromCommon`. Loggers must be `static`. |
| Lifecycle order (new game) | `onNewGame` → `onNewGameAfterProcGen` → `onNewGameAfterEconomyLoad` → `onEnabled` → `onNewGameAfterTimePass` → `onGameLoad`. `onGameLoad` also runs on every load. |
| Save compatibility | Everything reachable from the sector (memory, persistent data, intel, `addScript` scripts, permanent listeners) is XStream-serialized. Removing/renaming/moving such a class breaks saves (`CannotResolveClassException`). Register short aliases in `configureXStream(x)` (`x.alias("STS_RunState", RunState.class)`) **before first release**; never rename saved classes after. Per-session things: `addTransientScript`, re-added in `onGameLoad`; caches in `transient` fields. |
| Static fields | Never hold game objects in static fields: they survive loading another save and leak it. |
| FireAll vs FireBest | `FireAll` collects **all** matching rules' options, sorts by order, clears the panel only if there are options, then runs every match's script/text. `FireBest` applies only the single best-scoring match. So our `PopulateOptions` entry sits alongside Jangala's vanilla options (by design: order 99). |
| Option handlers | Every option id needs a `DialogOptionSelected` rule with `$option == id`, or it is a dead button — **unless** a Java `InteractionDialogPlugin` owns `optionSelected` (our `RunDialogPlugin`). Prefer **object** option data (enums) over strings in Java-owned menus. |
| Dialog hijack pattern | Vanilla `PaginatedOptions`: save `dialog.getPlugin()`, `dialog.setPlugin(this)`, handle `optionSelected` yourself. Leave with `dialog.setPlugin(original); FireAll.fire(null, dialog, memoryMap, "PopulateOptions")` to return to the market menu, or dismiss. |
| Rule ids | Global namespace: prefix every rule id (`sts_`). Condition line suffix `score:N` raises FireBest score. |
| Fleets | `FleetFactoryV3.createFleet(...)` can return **null or an empty fleet**: check before `startBattle`. |
| MemFlags | Look up every flag's vanilla use (`api_lookup.py const MemFlags X` / `uses X`) before setting; give each a removal point (`Misc.setFlagWithReason`). |
| Set-piece battles | `FleetInteractionDialogPluginImpl.FIDConfigGen` under `MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN` (value misspelt `"$fidConifgGen"`: use the constant). `pullInAllies/Enemies/Stations = false` keeps Jangala's station and nearby fleets out. `BattleCreationContext` cannot restrict player deployment — remove members from the fleet instead (we do: mothball bay). |
| Fleet callbacks | Never `removeEventListener` / `despawn()` inside `reportBattleOccurred` / `reportFleetDespawnedToListener` (`ConcurrentModificationException` → `Fatal: null`). Queue it, act next tick. |
| Intel | `addIntel` only — the intel manager already advances it; also adding it via `addScript` makes it tick twice. `endImmediately()` to finish. |
| Testing | No headless runner. After a session: `python3 tools/log_scan.py --mod .`. Dev mode via the `devmode` console command. Smoke-test checklist template: `~/.claude/skills/starsector-modding/templates/SMOKE_TEST.md`. |

## Must re-verify before coding

Use `api_lookup.py` (above) or `javap` (`JAR=/home/ubik/Downloads/starsector/starfarer.api.jar`).
Items marked *(obsolete)* only mattered for the superseded system/teleport design:

- *(obsolete)* `setContainingLocation` on fleet — `javap -cp $JAR com.fs.starfarer.api.campaign.SectorEntityToken | grep -i ContainingLocation` (confirm whether needed in addition to `setCurrentLocation`).
- `BattleCreationContext` extra setters (`setEnemyDeployAll`, `setAllowRetreat`) — `javap -cp $JAR com.fs.starfarer.api.combat.BattleCreationContext`.
- `FleetParamsV3` ctor overload + arg order — `javap -cp $JAR com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3`.
- `FleetTypes` / `Factions` constant ids — `javap -cp $JAR -constants com.fs.starfarer.api.impl.campaign.ids.FleetTypes` (and `.Factions`).
- *(obsolete)* `"stable_location"` has an interaction dialog (so `OpenInteractionDialog` fires) — check `<install>/starsector-core/data/config/custom_entities.json`.
- Mod `settings.json` merge **appends** (not replaces) `ruleCommandPackages` — check another mod's `settings.json` in `mods/`.
- `EngagementResultAPI.didPlayerWin()` — `javap -cp $JAR com.fs.starfarer.api.combat.EngagementResultAPI | grep -i didPlayer`.
- *(obsolete)* `removeStarSystemNextFrame` timing — move the player to the hub **before** scheduling removal.

Keep `RunState` / `MetaProgress` as plain `Serializable` POJOs storing **ids** only (resolve tokens
lazily) to avoid XStream save issues.

## Reference source (RAT Abyssal Depths)

`github.com/Lukas22041/Random-Assortment-of-Things`, package `src/assortment_of_things/abyss/`:
- `procgen/AbyssGenerator` — creates the isolated `StarSystemAPI` + terrain stack.
- `AbyssData` / `AbyssUtils` — run-state singleton in sector memory (`$rat_abyss_data`) + `hasGenerated` guard.
- `scripts/DisableTransverseScript` — FTL lockout via cooldown pin + no-jump flag.
- `campaign/rulecmd/AbyssGateCMD` — entry transition (`doHyperspaceTransition`).
- `procgen/AbyssBiomeManager` — cell-grid spatial structure + depth scaling.
- `AbyssBattleCreationPlugin` — custom battle context (darkening, speed penalties).

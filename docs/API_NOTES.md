# API_NOTES — Starsector 0.98a API facts

Verified against `/home/ubik/Downloads/starsector/starfarer.api.jar` (v0.98a-RC5). The official
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

## Must re-verify before coding

Grep with `javap` (`JAR=/home/ubik/Downloads/starsector/starfarer.api.jar`):

- `setContainingLocation` on fleet — `javap -cp $JAR com.fs.starfarer.api.campaign.SectorEntityToken | grep -i ContainingLocation` (confirm whether needed in addition to `setCurrentLocation`).
- `BattleCreationContext` extra setters (`setEnemyDeployAll`, `setAllowRetreat`) — `javap -cp $JAR com.fs.starfarer.api.combat.BattleCreationContext`.
- `FleetParamsV3` ctor overload + arg order — `javap -cp $JAR com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3`.
- `FleetTypes` / `Factions` constant ids — `javap -cp $JAR -constants com.fs.starfarer.api.impl.campaign.ids.FleetTypes` (and `.Factions`).
- `"stable_location"` has an interaction dialog (so `OpenInteractionDialog` fires) — check `<install>/starsector-core/data/config/custom_entities.json`.
- Mod `settings.json` merge **appends** (not replaces) `ruleCommandPackages` — check another mod's `settings.json` in `mods/`.
- `EngagementResultAPI.didPlayerWin()` — `javap -cp $JAR com.fs.starfarer.api.combat.EngagementResultAPI | grep -i didPlayer`.
- `removeStarSystemNextFrame` timing — move the player to the hub **before** scheduling removal.

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

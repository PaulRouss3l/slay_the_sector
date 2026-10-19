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

## Verified in game (Spike, 2026-10-19, RC8)

| Concern | Fact |
|---|---|
| Fleet swap | `sector.setPlayerFleet(newFleet)` while a dialog is open works; camera follows; the detached old fleet held in sector memory survives save/load with ships, officers, cargo, credits. |
| Stale location | After `loc.removeEntity(fleet)`, `fleet.getContainingLocation()` still returns the old location. Test membership with `loc.getFleets().contains(fleet)`. |
| Contained system | `dialog.dismiss()` → `setPaused(false)` → `doHyperspaceTransition(pf, from, new JumpDestination(to, null), 1f)` into a system with no jump points: no camera bug; save/load inside works. |
| Transverse jump lock | Holding `getAbility(Abilities.TRANSVERSE_JUMP).setCooldownLeft(1f)` from a transient `EveryFrameScript` keeps it unusable. |
| Battle setup | `pickBattleCreationPlugin` returning a `BattleCreationPluginImpl` subclass is honoured for a fleet engaged through the normal vanilla encounter. |
| Fleet wipe | `pickRespawnPlugin` at `MOD_SPECIFIC` replaces the vanilla defeat flow; restoring + transition from it works. |
| Canvas | `showCustomVisualDialog` + GL `render` + `processInput` (`isLMBDownEvent`, `isMouseMoveEvent`, Esc) works inside an interaction dialog. |

## Verified in game (S1, 2026-10-19, RC8)

| Concern | Fact |
|---|---|
| FID handoff | `FIDConfig.dismissOnLeave=false` + `delegate.notifyLeave` → `dialog.setPlugin(self)`; `FleetEncounterContext.isEngagedInHostilities()` / `didPlayerWinEncounterOutright()` tell retreat from win. Vanilla loot/recovery screens run first. |
| Salvage edit | `postPlayerSalvageGeneration(dialog, ctx, salvage)`: `salvage.removeSupplies/removeFuel` strips them before the loot screen. |
| Over capacity | Costs supplies daily even with `SuppliesPerMonth ×0`: settings `suppliesPer{Cargo,Fuel,Personnel}UnitOverCapacity` 0.1/unit/day (cap 50/day each). Per-ship `stats.getCargoMod().modifyFlat` in a hullmod adds cargo. |
| Refit CR cost | Refitting away from a spaceport costs CR (`crLossMultForRefit*`, hullmods ×4). |
| Intel | `BaseIntelPlugin` with `setImportant(true)` shows in Important; `getMapLocation` + `IntelUIAPI.showOnMap(token)`; `Global.getSector().layInCourseFor(token)`; icon via a `graphics.intel` key in our `settings.json`. |
| Custom entity | `"baseId"` inherits another entity's fields. Our beacon (sprite, `useLightColor:false`, no `baseId`) renders in the no-star Run system; `setCircularOrbitPointingDown` spins the sprite, `setCircularOrbit` does not. |
| Cards | `CardGridDelegate` (rebuild in the delegate's `advance`, not in the click) works for Starting and Reward picks. |

## Verified in game (S2, 2026-10-19, RC8)

| Concern | Fact |
|---|---|
| GL map | A `CustomUIPanelPlugin` drawing GL lines + `SpriteAPI.renderAtCenter` works in `showCustomVisualDialog` and in an intel `createLargeDescription` panel. Wrap `render` in `glPushAttrib/glPopAttrib`. The visual dialog is see-through: draw an opaque quad in the root plugin's `renderBelow`. |
| Sprites | Sprites listed under a `graphics.<category>` key in our `settings.json` load at startup; `getSprite(category, key)` / `getSpriteName(...)` (also for `beginImageWithText`). White glyphs tint with `setColor`. |
| Hidden intel | Overriding `isHidden()` (true outside a Run) hides a permanent intel entry. |
| S-mods | `variant.addPermaMod(id, true)` + `member.updateStats()`; limit `Misc.getMaxPermanentMods(member, playerStats)`; skip `Tags.HULLMOD_NO_BUILD_IN`/`dmod`. |
| Officer training | `OfficerDataAPI.addXP(xpForNextLevel - xp)` gives a pending level-up; the player picks the skill in the vanilla Fleet screen. |
| Boss pilots | `FleetParamsV3.withOfficers=false` (no officers/cores) or `aiCores = OfficerQuality.AI_GAMMA`. |

## Verified in game (S3, 2026-10-19, RC8)

| Concern | Fact |
|---|---|
| Escape / pursuit fights | `pickBattleCreationPlugin(opponent)` gets the encounter's other fleet; a `BattleCreationPluginImpl` subclass that builds a new `BattleCreationContext(player, goal, other, goal)` and calls `super.initBattle` with it yields a real escape (player `ESCAPE`) or pursuit (enemy `ESCAPE`) battle through the vanilla encounter. Copy the computed layout back to the original context: combat runs on that one. |
| Pursuit scoring | Vanilla counts "the enemy got away" (`ESCAPE_ENEMY_SUCCESS`) as a player win: score pursuits yourself. |
| Retreated survivors | A won battle with retreated enemies is `BATTLE_PLAYER_WIN`: `didPlayerWinEncounterOutright()` is false, `didPlayerWinMostRecentBattleOfEncounter()` is true. |

## UI text

The game's UI fonts have no em dash, middle dot or multiplication sign: "—", "·" and "×" render
as "?". Keep UI strings ASCII.

## Redesign research (2026-10, from source reading — not yet run in game)

| Concern | Fact |
|---|---|
| Custom canvas | `InteractionDialogAPI.showCustomVisualDialog(w, h, CustomVisualDialogDelegate)`; plugin gets `render/renderBelow` (GL), `processInput`, `advance`, `buttonPressed`. Vanilla precedent: `impl/campaign/eventide/DuelDialogDelegate` (1024×700). LunaLib `OpenCustomPanelFromDialog` hides visual/text panels for ~80 % screen. |
| Intel panel | `IntelInfoPlugin.hasLargeDescription()` + `createLargeDescription(CustomPanelAPI, w, h)`; open with `showCoreUITab(INTEL, intel)`. Usable outside dialogs. |
| Card-grid dialog | `dialog.showCustomDialog(w, h, CustomDialogDelegate)` — Nexerelin `Nex_NGCCustomStartFleet` (ship grid with +/-; no budget enforcement — add our own). Simpler: `showFleetMemberPickerDialog(...)` (no live counter). |
| Command tab | `CoreUITabId.OUTPOSTS`. AotD extends it via AshLib reflection on obfuscated classes (not public API). Not used. |
| Core screens from a dialog | `dialog.getVisualPanel().showCore(CoreUITabId, target, CoreUITradeMode.NONE, CoreInteractionListener)` (`OpenCoreTab` rulecmd). Refit cannot be filtered per member — the Run fleet must *be* the player fleet. |
| Battle setup override | `CampaignPlugin.pickBattleCreationPlugin(opponent)` → own `BattleCreationPlugin.initBattle(ctx, MissionDefinitionAPI loader)` (map size, terrain, objectives, `loader.addPlugin`). `BattleCreationPluginImpl` overwrites standoff/escape/burn fields set elsewhere. Examples: RAT `AbyssBattleCreationPlugin`, MagicLib `MagicBountyBattleCreationPlugin`. |
| Vanilla battle flow | `FleetInteractionDialogPluginImpl(FIDConfig)` gives CR/crew loss, XP, recovery, loot. `FIDDelegate.battleContextCreated` (boss: `aiRetreatAllowed=false; fightToTheLast=true; objectivesAllowed=false; enemyDeployAll=true`), `postPlayerSalvageGeneration`, `notifyLeave`. Raw `dialog.startBattle` applies **nothing**. |
| Flee / escape fights | `BattleCreationContext(pf, ATTACK, enemy, FleetGoal.ESCAPE)` = vanilla pursuit; player `ESCAPE` = escape battle (escaped ships in `getRetreated()`). Combat: `CombatFleetManagerAPI.getRetreatedCopy/getDestroyedCopy`, `endCombat(delay, side)`, `maintainStatusForPlayerShip`, `setMaxStrength` (DP cap). |
| Fleet wipe | `CampaignPlugin.pickRespawnPlugin()` — return own dialog at `MOD_SPECIFIC`. No `reportPlayerDefeated`. No vanilla ironman. |
| Cross-save data | `SettingsAPI.writeJSONToCommon/readJSONFromCommon`, `writeTextFileToCommon` (≤ 1 MB) → `<install>/saves/common/<name>.data`. Vanilla: `SharedUnlockData`. |
| Contained system | RAT `AbyssGenerator`: tags `THEME_HIDDEN`, `SYSTEM_CUT_OFF_FROM_HYPER`, `DO_NOT_SHOW_STRANDED_DIALOG`; entry via `doHyperspaceTransition(fleet, from, JumpDestination, 0.01f)` from a campaign-plugin-picked interaction. Cut-off tag does **not** obviously block `fracture_jump` — keep our own lock. |
| New game | `newGameCreationEntryPoint` / `newGameSectorProcGen` overridable in settings.json (Nexerelin). No API to start a new game from inside a save. Not used. |

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

# DESIGN — Slay the Sector

Terms are defined in `GLOSSARY.md`. API facts in `API_NOTES.md`. Why the Run lives in a contained
star system: `adr/0001-run-in-contained-system.md`. Milestones: `ROADMAP.md`.

Status: **design settled, not implemented** (redesign of 2026-10). Mechanisms marked 🧪 must be
proven by the Spike milestone before content is built on them.

## Goals

- A Slay-the-Spire-style roguelite **inside a normal campaign**: works with any modded ships and
  factions, never touches the vanilla sector, and has **zero impact on the player's campaign**
  except the Payout.
- Every fight is a real vanilla battle. Saving is possible between Nodes, so a Run can be long.

## Player journey: step → screen

| # | Step | Screen | Built with |
|---|---|---|---|
| 1 | Entry | Hub Intel entry (Important; Show on map, Lay in course) → Hub station dialog: Start Run / Meta progress / Restore Home fleet | `BaseIntelPlugin` + interaction dialog |
| 2 | Setup | Archetype list → Starting pick grid (30 DP cap) → free refit | dialog options → card grid → vanilla Refit (`CoreUITradeMode.NONE`) |
| 3 | Map | Node map canvas; read-only copy in the Intel tab | `showCustomVisualDialog` 🧪 + `IntelInfoPlugin.createLargeDescription` |
| 4 | Choose Node | click on the map → fly to the Node entity in the Run system | canvas → campaign map |
| 5a | Fight Nodes | Node dialog → vanilla fleet battle with a per-type battle setup | `FleetInteractionDialogPluginImpl` + `FIDConfig` + own `BattleCreationPlugin` 🧪 + combat plugin |
| 5b | Event | Node dialog text + choices, may chain into a fight | interaction dialog |
| 5c | Shop / Treasure | card grid priced in Supplies | card grid |
| 5d | Rest | choose one: repair all / S-mod / officer level | dialog options + ship picker |
| 6 | Reward pick | 3 cards with tooltips | card grid |
| 7 | Build | vanilla Refit + Fleet tabs, usable anytime in the Run system | vanilla, no code |
| 8 | Repair / sell | Fleet services panel after each Node | custom dialog |
| 9 | Act transition | Boss win → jump to the next Act | `doHyperspaceTransition` 🧪 |
| 10 | Run end | summary dialog (Depth, Payout, Unlocks) → back at the Hub, Home fleet restored | interaction dialog |
| 11 | Meta | Hub dialog page; data in `saves/common/` | `writeJSONToCommon` |

Only **two custom UI components** exist: the **card grid** (Starting pick, Shop, Treasure, Reward
pick) and the **node map** (canvas and Intel). Everything else is vanilla dialogs and screens.

## Entering and leaving a Run

- **Hub station**: a custom entity the mod places once (idempotent, also in old saves) in a core
  system; no market. Works in Nexerelin random sectors.
- **Start**: Archetype → Starting pick → the Home fleet and all cargo (credits included) are
  detached into saved Run state (a fleet object not placed in any location + a cargo object);
  the player fleet becomes the Run fleet with empty cargo plus starting Supplies → refit →
  jump transition into the Run system 🧪.
- **Contained**: the Run system has no jump points; our script blocks `fracture_jump`.
- **End** (win or loss): Run fleet and Run cargo are discarded, Home fleet and cargo restored,
  Payout added, Meta progress recorded, player returned to the Hub.
- **Safety net**: the Hub always offers "Restore Home fleet". On game load, stored Home assets
  with no active Run are restored automatically.

## Map

- 3 Acts × 10 rows + Boss, generated like Slay the Spire: on a 5-column grid, 4 paths climb row
  by row to one of the 3 nearest cells, never crossing (the first two start apart); only touched
  cells become Nodes. Rows 1-2 Combat, row 5 Treasure, row 10 Rest (then the Boss). Other Nodes
  are drawn by weight (Combat 5, Event 2, Shop 1.5, Rest 1.5, Elite 1, Ambush 1) and re-drawn
  while they break a rule: no Rest or Elite before row 4, no Ambush before row 3, no Rest on row
  9, no Elite→Elite / Shop→Shop / Rest→Rest, Nodes reached from one parent differ (Combat may
  repeat as the fallback); ≥ 1 Shop per Act. Winning Act 3 wins
  the Run. All counts in `settings.json` (`sts_actRows`, `sts_mapColumns`, `sts_mapPaths`,
  `sts_nodeWeight*`).
- Path lock: only successors of the current Node are selectable.
- Each Act draws 2-3 enemy factions (eligible list, minus the Archetype); Combat Nodes name their
  faction. Bosses come from the Boss pool (Remnant, Omega, Derelict) or the strongest eligible
  faction.
- Difficulty follows a fixed curve by Depth (not the player's fleet strength); Ascension adds to
  it later.

## Node types

| Type | Content |
|---|---|
| Combat | standard battle; Reward pick + Supplies |
| Elite (Escape) | encircled by a stronger fleet, player `FleetGoal.ESCAPE`; win = reach the edge with ≥ N% of DP; reward scales with ships saved |
| Ambush | enemy `FleetGoal.ESCAPE`, player spawns around it, one marked target 🧪; target destroyed = Reward pick, escaped = Supplies only |
| Boss | no retreat, fight to the last, enemy deploys all; pilots per `sts_bossOfficers` (default none: no officers or AI cores); strength ×`sts_bossFPMult` (1.3); Reward pick + bonus S-mod; retreat = Run lost |
| Event | text choice: battle station (fight or flee free; win = 3 Reward picks), sabotage (choice to inspect or risk −20 % Supplies), side battle (fight with allies, reward from the helped faction, no reputation change), convoy raid (convoy flees, reward by DP destroyed) |
| Shop | buy ships, weapons, officers, repairs with Supplies |
| Rest | choose one: full repair of the fleet / S-mod on one ship / +1 officer level |
| Treasure | choose 1 of 3 rare items (`rare_bp` hulls/weapons, Boss-pool ships, S-mod), scaled by Depth |

## Fleet, resources and rules

- **Archetypes**: `getAllFactions()` filtered by `isShowInIntelTab()`, ≥ ~8 known ships, not
  player/neutral/independent; blacklist/whitelist `sts_factionBlacklist` in `settings.json`.
  Boss-pool factions are never Archetypes.
- **Starting pick**: Archetype's known ships (stock variants), hard 30 DP cap, then free refit
  with the Archetype's known weapons; 1 starting officer.
- **Reward pick**: 1 of 3 among two ships (one of the faction just fought, one of the Archetype) /
  officer or weapon bundle. Never an
  S-mod; S-mods come only from Rest, Treasure and as a Boss bonus.
- **Supplies**: the only currency. Earned from fights (Combat < Elite < Boss), Events, selling
  ships. Spent on repairs and Shops.
- **Attrition**: hull/armor damage carries over; CR regenerates for free up to ~70 % after each
  fight, Supplies top it up; crew refilled free after each fight; fuel unlimited. Ships destroyed
  in a fight are lost unless recovered right after (vanilla recovery flow).
- **Fleet DP cap** grows with Act: soft cap 80 / 110 / 140. Each DP over the soft cap cuts every
  fight reward by 5 Supplies; the hard cap (soft + 30) blocks setting a course until ships are sold.
  Gaining ships is never blocked.
- **Character skills** of the player carry into the Run (accepted).
- **Run loss**: fleet wiped (intercepted via `CampaignPlugin.pickRespawnPlugin` 🧪) or no
  deployable ship left. Retreating from a non-Boss fight = that fight lost (no reward), Run goes on.
- **Save-scumming**: accepted in v1 (ironman-lite later).

## Persistence

- **Run state** → sector memory `$sts_run_state`: plain `Serializable` POJOs holding ids, plus the
  detached Home fleet and cargo. Aliased in `configureXStream`.
- **Meta progress** → cross-save JSON in `saves/common/` (`writeJSONToCommon`), never in the
  save. v1 content: Ascension per Archetype + small permanent starting bonuses.
- **Payout**: 0 credits for a loss before Node 3, linear with Depth after (e.g. Depth 3 = 5k up to
  the final Boss = 25k), 50k for a win. Values in `settings.json`.

## Code layout

Root package `com.slaythesector` (S2 state; later packages added as their milestones land).

| Package / class | Responsibility |
|---|---|
| `SlayTheSectorModPlugin` | lifecycle, XStream aliases, registers transient hooks, places the Hub |
| `RunCampaignPlugin` | routes Hub/Node interactions, fleet-wipe intercept (`pickRespawnPlugin`) |
| `hub/HubDialog` | Begin a Run (Archetype → Starting pick → refit → jump), recover Home fleet, abandon |
| `run/` | `RunState`/`RunNode`/`NodeType` (saved), `RunManager` (start/end/Payout), `HomeFleet` (swap), `RunFleet` (build, attrition, repair/sell), `Archetypes` (faction filter, ship pools), `SafetyNetScript` |
| `map/NodeGraphBuilder`, `map/NodeText` | pure-logic Act layout with Node-type placement rules; Node descriptions |
| `intel/HubIntel`, `intel/RunIntel` | Important Intel entry locating the Hub; Run overview with the read-only node map (hidden outside a Run) |
| `world/` | `HubPlacer`, `RunSystem` (contained system, Node entities, transitions), `FtlLockScript` |
| `combat/` | `EncounterFactory` (all Run fleets), `RunBattleCreationPlugin` + `MarkedTargetPlugin` (escape/pursuit setups, Ambush mark), `Rewards` (Reward pick), `RunRespawnDialog` |
| `nodes/NodeDialog`, `nodes/Events` | encounter → vanilla fleet encounter handoff → Reward pick; Shop/Rest/Treasure services; Fleet services; node map → next course |
| `ui/` | `CardGridDelegate` + `Card` (the shared picker), `NodeMapPanel` (GL map) + `NodeMapDelegate` (course picker) |
| `economy/` | `Shop` (stock, prices), `Treasure` (prizes), `Upgrades` (S-mods, officer training) |
| `hullmods/RunLogistics` | hidden built-in hullmod: no maintenance, no passive repair |
| `util/` | `STSConstants`, `STSSettings` (`sts_*` keys in settings.json), `DepthScaling`, `STSLog` |

**Implementation notes (S1)**
- Fights hand the Node dialog to `FleetInteractionDialogPluginImpl` with `dismissOnLeave=false`;
  `FIDDelegate.notifyLeave` hands it back (vanilla `SalvageDefenderInteraction` pattern). Boss:
  no retreat, fight to the last, enemy deploys all (`battleContextCreated`).
- Supplies only move by player choice: every Run ship carries the hidden `sts_run_logistics`
  hullmod (supply use ×0, repair/CR recovery ×0). Free CR to ~70 % and crew/fuel refill after
  each fight (`RunFleet.afterFight`).
- Only the chosen next Node gets a world entity; a set course is final (Path lock). The Run
  system is emptied (entities, wrecks, debris) between Runs.
- **Zero campaign impact, enforced**: Run enemies fly under the mod faction `sts_descent` (hostile
  to the player only) so commissions/bounties ignore Run kills; reputation impact is off; player
  XP/level/skill points/story points and veteran marines are snapshotted at start and restored at
  end; credits that land on the Run fleet (monthly income/payroll) are handed back to the Home
  fleet; salvaged supplies are stripped (Supplies come only from designed rewards).
- Payout scales on the **deepest Node won**, so retreating can't farm Depth.

**Implementation notes (S3)**
- Escape and pursuit fights: the enemy fleet carries `$sts_battle` (`escape` / `pursuit`);
  `RunCampaignPlugin.pickBattleCreationPlugin` returns `RunBattleCreationPlugin`, which rebuilds the
  `BattleCreationContext` with the goals (they have no setters) and hands it to the vanilla setup.
  Combat runs on the original context, so the layout the setup computed is copied back. A fleeing
  enemy deploys everything (reserves would count as escaped).
- Elite: enemy ×`sts_eliteFPMult` (1.5); a win is beating it, or bringing out ≥ `sts_eliteEscapeFraction`
  (60 %) of the **deployed** DP, measured against at least half the fleet (reserves don't count as
  saved); supplies 60 × share saved + a Reward pick.
- Pursuits: vanilla scores "the enemy got away" as a player win, so Ambush and convoy ignore it:
  only the mark (Ambush) or the share destroyed (convoy) decides.
- Ambush: the enemy's largest ship is marked (`$sts_target`); `MarkedTargetPlugin` rings it in
  combat and names it. Mark destroyed = Supplies + Reward pick, else 15 supplies.
- Events (drawn on first arrival, kept in `RunNode.eventId`): battle station (vanilla tier-1 station
  hull by Depth, unboardable, flee for free, win = 3 Reward picks), sabotage (inspect for 15 supplies or 50 % risk of
  −20 %), side battle (`createBattle(ally, enemy)` before the encounter so vanilla's join-an-ongoing-
  battle flow puts the player with the `sts_ally` side; reward from the helped faction; the joined battle is finished before despawn), convoy raid
  (fleeing freighters; 3 supplies per DP destroyed, Reward pick at ≥ 50 %). Event fights score as won
  only on success; declining resolves the Node with nothing.

**Implementation notes (S2)**
- Shop, Rest and Treasure resolve on arrival but never count as won (Payout stays on fights).
  Their service is claimed once (`RunNode.claimed`): Rest and Treasure on use; an opened Shop or
  Treasure when the player leaves the beacon (no re-roll); any unused service when a course is
  set. Each Shop item sells once; the Shop can be visited repeatedly until the player leaves.
- The Fleet DP cap is checked whenever a course is set or resumed.
- The node map's course is applied on the dialog's next frame, after the map has closed.
- Salaries: vanilla charges pay each economy tick for whatever fleet is the player fleet.
  `RunPayrollListener` adds up the Run fleet's pay minus the Home fleet's during a Run; Run end
  hands back the Run fleet's credits plus that total, so the campaign's credits come out as if
  no Run happened (Run pay refunded, Home pay still charged).
- Officer training gives the officer exactly the XP for its next level; the player picks the
  skill in the vanilla Fleet screen. S-mods use `addPermaMod(id, true)` within
  `Misc.getMaxPermanentMods`.
- Ship pools skip automated hulls (they need a skill to crew).
- Deferred from S1: free starting weapons from the Archetype's known weapons (refit uses what the
  ships carry plus salvage), Fleet DP cap (S2), Meta progress (S4).

## History

1. **Isolated system + teleport** (RAT-style, M1-M5): moving the player fleet from inside an open
   dialog left the camera stuck.
2. **Dialog-only Run** (post-M5): worked, but leaked Run gains into the campaign (cargo, weapons,
   Supplies), gave no vanilla post-battle flow, and blocked saving during a Run. Replaced by this
   design; see the ADR.

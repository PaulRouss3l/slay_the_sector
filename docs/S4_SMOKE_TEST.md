# S4 smoke test (~2 h for a full Run): Acts, Bosses, Ascension, Meta progress

**Setup**: `./build.sh`, mod enabled, start a **new Run**. A Run started on an older build stays a
one-Act Run. Afterwards I run `log_scan` myself (mod lines: `Run started ... ascension=`,
`Act N begins`, `Meta progress: starting bonus unlocked`). Tunables: `settings.json`
(`sts_actFactor`, `sts_actBossFactions`, `sts_trueBossHull`, `sts_ascension*`, `sts_bossRewardDP`,
`sts_bonusStart*`). Meta progress lives in `saves/common/slay_the_sector_meta.json`: delete it to
start fresh. Tip: save before each Boss.

**Log evidence (2026-10-20, no mod errors):** a Gamma Run went Act 1 -> Act 2 -> Act 3 (`Act 2
begins`, `Act 3 begins`, new enemy factions each time; the Derelict Act 2 Boss generated with no
fallback warning); escape and pursuit setups still apply; an earlier S4 loss at Depth 9 paid 9,000
(new 3-Act Payout scale); Meta progress was written (`tritachyon: 1 Run, best Depth 10`). Not seen
yet: the Act 3 Remnant Boss, a won Run's Records and unlocks, any Ascension above Gamma, Act 4.

## 1. Hub
- [x] Main menu: **Records** page (empty at first; shows the starting bonuses as locked).
- [x] First Run: no Ascension or bonus screen (only Gamma, nothing unlocked); straight to the pick.

## 2. Acts and Bosses
- [x] Act 1 Boss: "The Act 1 Boss: a <enemy faction> fleet", strength ~78.
- [x] Win it: Boss supplies, then the **Boss reward** choice (no Reward pick):
  - [x] Refit: pick a ship -> it can take one more S-mod (check in Refit), then the S-mod picker for any
        ship. Press Back instead: "Choose your free S-mod (Boss reward)" stays on the beacon menu until used.
  - [x] Bigger fleet: beacon text shows the cap 20 higher (`Fleet DP x / 100`).
  - [x] Remnant warship: Brilliant with a Gamma core captain, **CR normal** (not 0).
- [x] Then **Descend to Act 2**: dialog closes, a message, a new Entry Beacon right where you are;
      new map, new enemy factions, "The Descent: Act 2" on the map.
- [x] Act 2 is harder (×1.6), its Boss is **Derelict** (~125); Act 3 ×2.2, Boss **Remnant** (~172).
      (Log: all three Bosses beaten in one Run.)
- [x] Leaving the Boss beacon before choosing the reward, then coming back: the choice is still there.
- [x] Beat Act 3's Boss at Gamma: "The Run is won", Payout 50,000. (Log 2026-10-20: `won=true,
      depth=33, payout=50000`; the Remnant Boss generated with no fallback warning.)
- [x] The Run's end posts an intel report with a notification: result, Payout, Act/Depth/Ascension
      and what was unlocked (replaces the old one-line message).

## 3. Meta progress
- [x] After that win, Records shows the Run; the Archetype list says "(won at Gamma)". (Meta file:
      Tri-Tachyon 2 Runs, 1 win, best Depth 33, best Ascension won Gamma, Beta unlocked.)
- [x] Next Run with that faction: Ascension choice Gamma / Beta, each with its handicap text.
- [x] Starting bonuses screen: "+5 starting DP" unlocked by the first win ([x]/[ ] toggles);
      the Starting pick budget shows 35 DP when on.
- [x] Reaching Act 3 (win or lose) unlocks "+1 level for the starting officer" (log: `starting bonus
      unlocked: officerLevel`; the pending level-up at the next start still to see).
- [x] A Run's end is recorded in `saves/common/slay_the_sector_meta.json.data` (log: Depth 9 loss
      -> `runs 1`), and the loss Payout follows the 3-Act scale (Depth 9 -> 9,000).

## 4. Ascension effects (any order, as unlocked)
- [x] Beta: enemy strength +15 % (encounter numbers). Chronos: "N supplies withheld" after fights.
- [x] Alpha: noticeably more Elites. Seraph: Rest repairs only **half** of the damage (check a
      lightly damaged ship isn't fully repaired). Primordial: Boss ships have pilots (Gamma cores on
      Derelict/Remnant, officers on the Act 1 Apex fleet).
- [x] Payout: +50,000 per level on a win (e.g. Beta win = 100,000).

## 4b. Ship fit tooltip
- [x] On ship cards (Starting pick, Reward, Shop, Treasure), hovering the ship's **name** shows hull,
      DP, system and OP; F1 expands to weapons, fighter wings and hullmods. The icon keeps vanilla's
      tooltip. A grey header line tells the player to hover the name.

## 4b. Ship fit tooltip
- [x] On ship cards (Starting pick, Reward, Shop, Treasure), hovering the ship's **name** shows hull,
      DP, system and OP; F1 expands to weapons, fighter wings and hullmods. The icon keeps vanilla's
      tooltip. A grey header line tells the player to hover the name.

## 5. Battle station Event
- [x] "Trade with the station" replaces "Slip past": the Node turns into a Shop (Browse, dockyard
      repair and D-mod removal); the stock closes when you leave the beacon. Assault still works.

## 6. Act 4 (Ascension Omega)

Log 2026-10-20: an Omega Run, `sts act 4`, true Boss beaten: `won=true, depth=35, payout=450000`
(50,000 + 6 x 50,000 for Omega + 100,000 true Boss).

Shortcut with the Console Commands mod: start any Run, jump into the Descent, then `sts act 4` (or
`sts unlock` then start an Omega Run). `sts supplies 500` helps survive the Ziggurat test.
- [x] After Act 3's Boss, "Descend to Act 4: the true Boss": Start -> Rest -> Boss.
- [x] The Boss is a lone **Ziggurat**, no escort; win = "True victory", Payout + 100,000, Records "Omega cleared".
- [x] Losing to it (or abandoning: "The Run stays won") still ends a **won** Run.
- [x] After any Run with a station or the Ziggurat, a Ziggurat/station met elsewhere in the campaign
      is still recoverable as normal (no tag leaked onto the stock variant).

## 7. Compatibility fixes (0.5.3)
- [ ] With Nexerelin and a commission: run a fight, then check the Intel faction relations: no
      reputation change, no "warmonger" event; Run enemies still hostile, side-battle allies still help.
- [ ] Hyperspace map: nothing new shows for "The Descent" (it now sits inside the Sector's bounds);
      jumping into a Run still works. Log on load of an older save: `Run system placed at x, y`.
- [ ] A Run that crosses a month end: the Run end hands back colony income as before, and no
      `credits paid to the Run fleet outside the monthly report` warning appears in a plain game.
- [ ] Dustkeepers (SotF; reveal with `runcode Global.getSector().getFaction("sotf_dustkeepers").setShowInIntelTab(true)`):
      pick only drones. The Run starts (no "fleet destroyed"); the biggest drone is the flagship with
      **Command Uplink** and you command it; every drone has normal CR without the Automated Ships skill.
- [ ] Same Run: lose the flagship in a fight while other drones survive. The Run goes on; log
      `Command Uplink moved to <hull>`; the biggest surviving drone is the new flagship.
- [ ] Shop / Rewards of a Dustkeeper Run offer Dustkeeper drones; bought ones have normal CR.

## 8. Removing the mod (ADR 0002)
- [x] During a Run, `sts remove`: refused ("a Run is in progress"), nothing changes.
- [x] No Run, `"sts_prepareRemoval": true`, load: message "Slay the Sector removed from this save";
      no Relay, no Slay the Sector intel; log `prepared for removal: [...]`. Save.
- [x] `python3 scripts/save_residue.py`: "OK: nothing known to break loading without the mod"
      (only `sts_descent` / `sts_ally` listed under other residue).
- [x] Disable the mod (keep Nexerelin etc. as they were), load that save: it loads and plays.
      **This answers the faction question in ADR 0002**; note the result in ROADMAP.
- [x] Re-enable the mod with the setting back to false, load: the Relay and its intel come back.


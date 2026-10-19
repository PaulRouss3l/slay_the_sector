# S4 smoke test (~2 h for a full Run): Acts, Bosses, Ascension, Meta progress

**Setup**: `./build.sh`, mod enabled, start a **new Run**. A Run started on an older build stays a
one-Act Run. Afterwards I run `log_scan` myself (mod lines: `Run started ... ascension=`,
`Act N begins`, `Meta progress: starting bonus unlocked`). Tunables: `settings.json`
(`sts_actFactor`, `sts_actBossFactions`, `sts_trueBossHull`, `sts_ascension*`, `sts_bossRewardDP`,
`sts_bonusStart*`). Meta progress lives in `saves/common/slay_the_sector_meta.json`: delete it to
start fresh. Tip: save before each Boss.

## 1. Hub
- [ ] Main menu: **Records** page (empty at first; shows the starting bonuses as locked).
- [ ] First Run: no Ascension or bonus screen (only Gamma, nothing unlocked); straight to the pick.

## 2. Acts and Bosses
- [ ] Act 1 Boss: "The Act 1 Boss: a <enemy faction> fleet", strength ~78.
- [ ] Win it: Boss supplies, then the **Boss reward** choice (no Reward pick):
  - [ ] Refit: pick a ship -> it can take one more S-mod (check in Refit), then the S-mod picker for any
        ship. Press Back instead: "Choose your free S-mod (Boss reward)" stays on the beacon menu until used.
  - [ ] Bigger fleet: beacon text shows the cap 20 higher (`Fleet DP x / 100`).
  - [ ] Remnant warship: Brilliant with a Gamma core captain, **CR normal** (not 0).
- [ ] Then **Descend to Act 2**: dialog closes, a message, a new Entry Beacon right where you are;
      new map, new enemy factions, "The Descent: Act 2" on the map.
- [ ] Act 2 is harder (×1.6), its Boss is **Derelict** (~125); Act 3 ×2.2, Boss **Remnant** (~172).
- [ ] Leaving the Boss beacon before choosing the reward, then coming back: the choice is still there.
- [ ] Beat Act 3's Boss at Gamma: "The Run is won", Payout 50,000.

## 3. Meta progress
- [ ] After that win, Records shows the Run; the Archetype list says "(won at Gamma)".
- [ ] Next Run with that faction: Ascension choice Gamma / Beta, each with its handicap text.
- [ ] Starting bonuses screen: "+5 starting DP" unlocked by the first win ([x]/[ ] toggles);
      the Starting pick budget shows 35 DP when on.
- [ ] Reaching Act 3 (win or lose) unlocks "+1 level for the starting officer" (level-up pending
      in the Fleet screen at the start).

## 4. Ascension effects (any order, as unlocked)
- [ ] Beta: enemy strength +15 % (encounter numbers). Chronos: "N supplies withheld" after fights.
- [ ] Alpha: noticeably more Elites. Seraph: Rest repairs only **half** of the damage (check a
      lightly damaged ship isn't fully repaired). Primordial: Boss ships have pilots (Gamma cores on
      Derelict/Remnant, officers on the Act 1 Apex fleet).
- [ ] Payout: +50,000 per level on a win (e.g. Beta win = 100,000).

## 5. Battle station Event
- [ ] "Trade with the station" replaces "Slip past": the Node turns into a Shop (Browse, dockyard
      repair and D-mod removal); the stock closes when you leave the beacon. Assault still works.

## 6. Act 4 (Ascension Omega; or set a test: temporarily give yourself the level by editing the meta file)
- [ ] After Act 3's Boss, "Descend to Act 4: the true Boss": Start -> Rest -> Boss.
- [ ] The Boss is a lone **Ziggurat**, no escort; win = "True victory", Payout + 100,000, Records "Omega cleared".
- [ ] Losing to it (or abandoning: "The Run stays won") still ends a **won** Run.
- [ ] After any Run with a station or the Ziggurat, a Ziggurat/station met elsewhere in the campaign
      is still recoverable as normal (no tag leaked onto the stock variant).

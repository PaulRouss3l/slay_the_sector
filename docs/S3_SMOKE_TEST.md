# S3 smoke test (~45-60 min): Elite, Ambush, Events

**Setup**: `./build.sh`, mod enabled, start a **new Run** (older Runs have no Elite/Ambush/Event
Nodes). Afterwards I run `log_scan` myself. Mod log lines start with `[SlayTheSector]`; every special
fight logs `battle setup: escape|pursuit ...`. Tunables: `data/config/settings.json` (`sts_elite*`,
`sts_ambush*`, `sts_sabotage*`, `sts_station*`, `sts_convoy*`, `sts_sideAllyFPMult`, `sts_nodeWeight*`).
Tip: save before each new Node type so you can retry.

**Status (2026-10-19)**: sections 1-4 tested in game (tester report + log: `battle setup: escape`
and `pursuit` applied, Boss won with Payout 50,000, no mod errors). Still to test: section 5, the
first item of section 6 and section 7 (added after that test), and the reputation check.

## 1. Map
- [x] New icons: Elite (hollow red diamond), Ambush (orange triangle), Event (orange "!?"); legend lists them.
- [x] No Elite before row 4, no Ambush before row 3, never Elite -> Elite on a path.

## 2. Elite
- [x] Encounter text: "An elite ... fleet", strength ×1.5, the 60 % rule explained.
- [x] Engage -> the battle starts as an **escape**: your ships must reach the map edge; the enemy
      flanks. (If it starts as a normal battle, note it: the setup didn't apply.)
- [x] Escape with ≥ 60 % of your **deployed** DP: "You saved N%", supplies ~60 × share, Reward pick.
- [x] Escape with less: "You saved N%", no reward, the Run goes on.
- [x] Deploying one small ship and retreating it at once is **not** a win (scored against half the fleet).

## 3. Ambush
- [x] Encounter text explains the marked flagship.
- [x] Battle starts as a **pursuit** (enemy runs, all its ships deployed); a pulsing orange ring on the target, a status line
      "Marked target: ..." and messages when it dies or escapes.
- [x] Target destroyed: supplies + Reward pick. Target escaped: "The marked target got away", 15 supplies.

## 4. Events (one per Event Node; reload to see others)
- [x] **Battle station**: Assault -> a fixed orbital station battle (not offered for recovery after); win = supplies + 3 Reward picks in a row
      ("N picks left"). Slip past -> nothing, Node done.
- [x] **Sabotage**: Inspect (−15 supplies, greyed if you can't pay) or Press on (50 %: lose 20 %).
- [x] **Side battle**: Join A / Join B / Stay out. Joining: the helped side fights **with** you
      (allied ships in the deployment screen / battle); win = supplies + Reward pick of the helped
      faction's ships. Note anything odd (allies hostile, you can't join, no allies in battle).
- [x] **Convoy raid**: battle is a pursuit of freighters; "You destroyed N DP": 3 supplies per DP,
      Reward pick if ≥ 50 % destroyed. Let it pass -> nothing.
- [x] Leaving an Event and coming back shows the same Event (no re-roll).

## 5. D-mod removal
- [ ] **Rest**: "Restore a ship (remove all its D-mods)" (greyed when no ship has D-mods) -> pick a
      ship -> all D-mods gone, a "(D)" hull becomes the clean hull. Back returns to the Rest menu.
- [ ] **Treasure**: a "Restoration" prize appears when a ship has D-mods; taking it runs the same picker.
- [ ] **Shop**: "Dockyard: remove a D-mod" -> ship (price per D-mod shown, greyed if unaffordable)
      -> D-mod -> paid, removed. Works after buying too; gone when you leave the beacon.

## 6. Regression
- [ ] Win a normal fight where some enemies retreat, or use **Claim victory**: no Re-engage prompt;
      straight to the loot/recovery screen (survivors' hulls count as wrecks), then Continue ->
      supplies + Reward pick. Elite/Ambush/convoy still behave as escapes/pursuits.
- [x] Combat, Boss (now beaten: Payout 50,000, Home fleet back), Shop, Rest, Treasure, DP cap,
      save/load mid-Run, Abandon as in S2.
- [ ] Reputation with the factions involved is unchanged after a side battle.

## 7. Beacon menu
- [ ] "Refit the Run fleet" now reads **Refit**.
- [ ] **Repair the fleet (N supplies)** appears when a ship needs repair (hidden otherwise, greyed
      if you can't pay); one click repairs every ship (hull + CR) and stays on the beacon menu. Same
      cost as "Repair all" in Fleet services.

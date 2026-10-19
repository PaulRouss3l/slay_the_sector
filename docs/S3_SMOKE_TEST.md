# S3 smoke test (~45-60 min): Elite, Ambush, Events

**Setup**: `./build.sh`, mod enabled, start a **new Run** (older Runs have no Elite/Ambush/Event
Nodes). Afterwards I run `log_scan` myself. Mod log lines start with `[SlayTheSector]`; every special
fight logs `battle setup: escape|pursuit ...`. Tunables: `data/config/settings.json` (`sts_elite*`,
`sts_ambush*`, `sts_sabotage*`, `sts_station*`, `sts_convoy*`, `sts_sideAllyFPMult`, `sts_nodeWeight*`).
Tip: save before each new Node type so you can retry.

## 1. Map
- [ ] New icons: Elite (hollow red diamond), Ambush (orange triangle), Event (orange "!?"); legend lists them.
- [ ] No Elite before row 4, no Ambush before row 3, never Elite -> Elite on a path.

## 2. Elite
- [ ] Encounter text: "An elite ... fleet", strength ×1.5, the 60 % rule explained.
- [ ] Engage -> the battle starts as an **escape**: your ships must reach the map edge; the enemy
      flanks. (If it starts as a normal battle, note it: the setup didn't apply.)
- [ ] Escape with ≥ 60 % of your **deployed** DP: "You saved N%", supplies ~60 × share, Reward pick.
- [ ] Escape with less: "You saved N%", no reward, the Run goes on.
- [ ] Deploying one small ship and retreating it at once is **not** a win (scored against half the fleet).

## 3. Ambush
- [ ] Encounter text explains the marked flagship.
- [ ] Battle starts as a **pursuit** (enemy runs, all its ships deployed); a pulsing orange ring on the target, a status line
      "Marked target: ..." and messages when it dies or escapes.
- [ ] Target destroyed: supplies + Reward pick. Target escaped: "The marked target got away", 15 supplies.

## 4. Events (one per Event Node; reload to see others)
- [ ] **Battle station**: Assault -> a fixed orbital station battle (not offered for recovery after); win = supplies + 3 Reward picks in a row
      ("N picks left"). Slip past -> nothing, Node done.
- [ ] **Sabotage**: Inspect (−15 supplies, greyed if you can't pay) or Press on (50 %: lose 20 %).
- [ ] **Side battle**: Join A / Join B / Stay out. Joining: the helped side fights **with** you
      (allied ships in the deployment screen / battle); win = supplies + Reward pick of the helped
      faction's ships. Note anything odd (allies hostile, you can't join, no allies in battle).
- [ ] **Convoy raid**: battle is a pursuit of freighters; "You destroyed N DP": 3 supplies per DP,
      Reward pick if ≥ 50 % destroyed. Let it pass -> nothing.
- [ ] Leaving an Event and coming back shows the same Event (no re-roll).

## 5. Regression
- [ ] Combat, Boss, Shop, Rest, Treasure, DP cap, save/load mid-Run, Abandon as in S2.
- [ ] Reputation with the factions involved is unchanged after a side battle.

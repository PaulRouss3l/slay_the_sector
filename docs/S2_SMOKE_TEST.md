# S2 smoke test (~30-45 min): map and economy Nodes

**Setup**: `./build.sh`, mod enabled. No new game needed, but a Run started on an S1 build keeps
its all-Combat map: start a new Run to see Shop/Rest/Treasure. Afterwards I run `log_scan` myself.
Tunables: `data/config/settings.json` (`sts_nodeWeight*`, `sts_fleetDPCap`, `sts_shop*`).

## 1. Node map
- [ ] At the Entry Beacon, **Open the node map**: Start at the bottom, Boss at the top, 7 rows of
      2-3 Nodes, one icon per Node type (Boss larger). Rows 1-2 are Combat, the middle row Treasure, the row before the Boss Rest.
- [ ] Hover shows the Node (type, faction, Depth, strength) and what it offers.
- [ ] Only the current Node's successors pulse; clicking another says it is not reachable.
- [ ] Click a pulsing Node → **Set course** → dialog closes, course laid in. Esc/Close cancels.
- [ ] Later in the Run: travelled path drawn gold, visited Nodes dimmed.
- [ ] Intel screen: **The Descent: current Run** entry (Important) with summary; its large view
      shows the same map, read-only. Hidden when no Run is active.

## 2. Shop
- [ ] Arrive: trader text, **Browse the shop**: ships (DP + price), weapons, an officer; budget =
      your Supplies; +/- per card; Confirm buys, supplies deducted, items in fleet/cargo.
- [ ] **Dockyard** repair-all costs half the Fleet services price.
- [ ] Each item can be taken once. Buy, close, sell a ship in Fleet services, reopen: the rest of
      the stock is still there. Leave the beacon (or set a course) and come back: shop gone.

## 3. Rest
- [ ] **Rest (choose one)**: free full repair / S-mod / train an officer; impossible ones greyed.
- [ ] S-mod: pick ship → pick hullmod → it shows as an S-mod in Refit. Back returns to the menu.
- [ ] Train: officer gets a level-up; choose the skill in the Fleet screen.
- [ ] After one choice the Rest option is gone. Setting a course without choosing forfeits it.

## 4. Treasure
- [ ] **Open the cache**: up to 3 prizes (a bigger ship, rare weapons, S-mod). Take one; it's
      applied (S-mod goes through the ship/hullmod picker). "Not now" keeps it until you leave.

## 5. Fleet DP cap
- [ ] Beacon text shows `Fleet DP x / 80 (hard cap 110)`; the Reward/Shop/Treasure grid titles too.
- [ ] Above 80: red warning on the beacon, the encounter and the map ("N DP over the soft cap:
      every fight reward is cut by 5×N supplies"); courses still work. Win a fight: reward reduced,
      with a line saying how much.
- [ ] Above 110: the map opens locked with the reason; "Resume course" refuses too. Sell ships,
      then courses work again.

## 6. Regression
- [ ] Combat, Reward pick, retreat, Boss, save/load mid-Run and Abandon still work as in S1.
- [ ] No Rest/Shop/Treasure counts toward the Payout (Payout = deepest **fight** won).
- [ ] Salaries: note your credits before a Run that crosses a month end (buy officers in the
      Shop). After the Run, credits = before + Payout + colony income − your Home fleet's usual
      pay; the Run's officers/crew cost you nothing.

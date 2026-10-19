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
- [ ] Close the shop without buying, reopen: same stock. Leave the beacon and come back: shop gone.

## 3. Rest
- [ ] **Rest (choose one)**: free full repair / S-mod / train an officer; impossible ones greyed.
- [ ] S-mod: pick ship → pick hullmod → it shows as an S-mod in Refit. Back returns to the menu.
- [ ] Train: officer gets a level-up; choose the skill in the Fleet screen.
- [ ] After one choice the Rest option is gone.

## 4. Treasure
- [ ] **Open the cache**: up to 3 prizes (a bigger ship, rare weapons, S-mod). Take one; it's
      applied (S-mod goes through the ship/hullmod picker). "Not now" keeps it until you leave.

## 5. Fleet DP cap
- [ ] Waypoint shows `Fleet DP: x / 60`. Buy or win ships past 60: the map opens read-only with
      a note; sell ships in Fleet services, then courses work again.

## 6. Regression
- [ ] Combat, Reward pick, retreat, Boss, save/load mid-Run and Abandon still work as in S1.
- [ ] No Rest/Shop/Treasure counts toward the Payout (Payout = deepest **fight** won).

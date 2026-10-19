# Spike smoke test (~15 min)

Proves the 5 risky mechanisms from `ROADMAP.md` "Spike" before any content is built on them.
The spike code lives in `src/com/slaythesector/spike/` and is throwaway.

**Setup**: `./build.sh`, enable **Slay the Sector** in the launcher (it is currently disabled),
start the game (`./run.sh`). Use a **throwaway save** (a new game is fine). After the session,
run `python3 tools/log_scan.py --mod .` and send the output plus your answers below.
Spike log lines start with `[SlayTheSector] spike:`.

## 0. Hub appears
Load the save. A **"Descent Relay (spike)"** station sits ~400 units to the right of your fleet.
- [x] Station visible and clickable → test menu opens.

## 1. Fleet swap + save/load (item 5)
1. At the relay: **Stash Home fleet, take a Run fleet**.
   - [x] Your fleet is now a Wolf + Hammerhead; credits/cargo empty except crew, 100 supplies, 100 fuel.
   - [x] Camera still follows your fleet; you can fly normally.
2. **Save**, quit to menu, **load** that save.
   - [x] Load works, you still have the Run fleet.
3. At the relay: **Restore Home fleet**.
   - [x] All original ships, officers (assigned to the same ships?), cargo and credits are back.
   - Note anything missing or duplicated (e.g. the player captain on two ships).

## 2. Jump into the contained system (item 1)
1. At the relay: Stash again, then **Jump into the Run system**.
   - [x] Jump animation, you arrive in "STS Spike Depths" next to the Descent Beacon. **No stuck camera.**
   - [x] Transverse Jump is greyed / on cooldown and stays so.
   - [x] No jump points in the system; the sector map does not show a way out.
2. Save + load inside the system.
   - [x] Load works, you're still in the system with the Run fleet.

## 3. Custom battle setup (item 2)
1. At the beacon: **Spawn a test enemy** (pirates). Let it reach you, **Engage**.
   - [x] Combat message "[STS spike] custom battle setup active" appears after ~2 s.
   - [x] No capturable objectives on the map.
   - [x] Enemies don't retreat.
2. After the fight: vanilla post-battle screens (loot, recovery) appear normally.

## 4. Node map canvas (item 4)
1. At the beacon (or relay): **Open the node map canvas**.
   - [x] 5 nodes + lines are drawn; hovering highlights a node; clicking turns it gold and the text updates.
   - [x] Close button and Esc both close it; the dialog is still usable after.

## 5. Fleet wipe intercept (item 3)
1. At the beacon: **Spawn an overwhelming enemy** (Remnants). Engage and **fight until your Run fleet is destroyed** (don't retreat).
   - [x] Instead of the vanilla "fleet destroyed" screen: "[STS spike] Your Run fleet was destroyed…"
   - [x] **Return to the Hub** → jump back to the relay, **Home fleet restored** intact.
2. Save + load.
   - [x] Everything still correct.

## 6. Back to the hub normally
At the beacon: **Return to the Hub** (with Run fleet still alive).
- [x] Jump back to the relay; then **Restore Home fleet** works.

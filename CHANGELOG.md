# Changelog

## 0.5.0 (beta)

First public beta. A full rewrite of the earlier prototype: the Run now lives in its own star
system inside your normal campaign.

**The Run**
- The **Descent Relay**, a station placed in the most populated core system, starts a Run. An
  Important Intel entry shows where it is, with Show on map and Lay in course.
- Pick a faction (vanilla or modded) and build a **30 DP starting fleet** from its ships (or press
  Random). Your real fleet, cargo and officers stay docked at the Relay, untouched, until the Run ends.
- **Three Acts**, each a branching node map in the style of Slay the Spire (10 rows, paths that never
  cross), ending in a Boss. Choose your course on the map, then fly to the beacon.
- **Zero campaign impact**: XP, skills, story points, marines, reputation, bounties, cargo and
  salaries are restored; the only thing a Run gives your campaign is a credit **Payout**.

**Nodes**
- **Combat**, **Elite** (a stronger fleet surrounds you: break out with most of your fleet),
  **Ambush** (the enemy flees: destroy its marked flagship), **Boss**.
- **Events**: battle station (assault it, or trade with it), sabotage, side battle with an allied
  fleet, convoy raid.
- **Shop** (ships, weapons, officers, repairs, D-mod removal), **Rest** (repair, S-mod, officer
  training or D-mod removal), **Treasure** (rare ship, rare weapons, S-mod or restoration).

**Economy**
- **Supplies** are the Run's only currency: earned from fights, spent on repairs and in Shops.
- Reward pick after each won fight: a ship of the defeated faction, one of your own, or an officer /
  weapons. Boss rewards: an S-mod slot and an S-mod, a bigger fleet cap, or a Remnant warship.
- **Fleet DP cap** (80 / 110 / 140 by Act): above it, fight rewards shrink; 30 above it, you must sell
  before moving on.
- A won battle is a total victory: no chasing retreated survivors.

**Progression**
- **Ascension** per faction, from Gamma to Omega (named after vanilla and RAT AI cores), each level
  adding a handicap and +50,000 to the Payout. At Omega, a hidden Act 4 waits.
- Cross-save **Records** and three **starting bonuses** to unlock.

**Settings**
- Nearly every number (difficulty, rewards, caps, prices, map shape) is in
  `data/config/settings.json` (`sts_*` keys).

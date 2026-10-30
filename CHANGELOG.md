# Changelog

## 0.5.4 (beta)

- The mod can be removed from a save: finish your Run, set `"sts_prepareRemoval": true` in
  `data/config/settings.json` (or run `sts remove` with Console Commands), load, save, then disable
  the mod. Steps in the README.
- Version Checker links to the forum thread.

## 0.5.3 (beta)

**Compatibility**
- With [Crew Stations](https://github.com/PaulRouss3l/crew_stations) installed, your first won Run also
  unlocks its Simulator Bay; the Run-end report says so.
- Nexerelin: Run fights no longer touch your real reputation, and the Descent's factions keep their
  relations (no warmonger event, no neutral Descent after a commission).
- Credits other mods pay into the Run fleet during a Run (e.g. Nexerelin insurance for lost Run
  ships) no longer leak into your campaign. Only the Payout comes home.
- The Descent's star system now sits inside the Sector's bounds, so mods that size the Sector from
  its systems (MagicLib bounties) are no longer skewed. Older saves are moved on load.

**Factions**
- Automated factions (e.g. Secrets of the Frontier's Dustkeepers, once met) are playable: drones
  need no Automated Ships skill, and an all-drone fleet gets a Command Uplink on its biggest drone,
  which becomes your flagship.

## 0.5.2 (beta)

**Fixes**
- Fixed a crash when confirming the Starting pick with Combat Chatter installed.
- Fixed a related error with Nexerelin's salvage scavenger dialog near the Run fleet.

## 0.5.1 (beta)

**Balance**
- Act 2 and Act 3 no longer start easier than the end of the previous Act: fights climb from the
  previous Act's last fight (Act 1 15 -> 56, Act 2 56 -> 89, Act 3 89 -> 122).
- Supply rewards grow with the Act (x1.0 / x1.5 / x2.0), since the fleet gets bigger and costs more
  to repair.
- The true Boss and the battle station now start at full combat readiness.

**Shops**
- One page of stock per Act (Act 2 = 2 pages, Act 3 = 3 pages).
- Buy +10 Fleet DP cap for 100 supplies (first card of every Shop).

**Interface**
- Ship cards: hover a ship's name to see its fit, press F1 for its weapons, fighters and hullmods.
- The end of a Run posts an intel report with a notification: result, Payout, how deep you went,
  and what you unlocked.

**Other**
- Debug commands for the Console Commands mod: `sts status`, `sts unlock`, `sts act <2-4>`,
  `sts supplies <n>` (handy to test Act 4).
- The mod is now under the MIT license (LICENSE file included).
- Bug reports and balance feedback: friendly forms on the GitHub Issues page.

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

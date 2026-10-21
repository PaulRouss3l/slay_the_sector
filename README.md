# Slay the Sector

**A Slay the Spire style roguelite, played inside your Starsector campaign.**

*Beta 0.5.2, for Starsector 0.98a.*

[![Install with TriOS](https://trilink.wispborne.com/badges/install-badge-forthebadge.svg)](https://trilink.wispborne.com/open.html?mod=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FPaulRouss3l%2Fslay_the_sector%2Fmain%2Fslay_the_sector.version%22%2C%22id%22%3A%22slay_the_sector%22%7D)

**[Download the latest release](https://github.com/PaulRouss3l/slay_the_sector/releases)** (zip, extract into `mods/`). No dependencies.

The Descent Relay has opened somewhere in the core worlds. Dock with it, plug into its combat
simulation, and fight your way down **The Descent**: three Acts of branching battles, shops,
events and Bosses, with a fleet you build as you go. Your real fleet waits safely at the Relay. When
the Run ends, win or lose, you get it back exactly as you left it, plus a credit Payout for how deep
you went.

## How it plays

1. **Find the Relay.** Open the Intel screen: *The Descent awaits (Slay the Sector)* is in the
   Important tab, with **Show on map** and **Lay in course**.
2. **Pick a faction and a fleet.** Any faction with enough ships, modded ones included. Spend
   **30 DP** on its ships (or press Random), then refit.
3. **Descend.** Each Act is a node map: choose the next beacon, fly there, resolve it.
   - **Combat**, **Elite** (break out of an encirclement), **Ambush** (hunt a fleeing flagship)
   - **Events**: battle station, sabotage, a side battle with allies, a convoy raid
   - **Shop**, **Rest**, **Treasure**, and a **Boss** at the bottom of every Act
4. **Grow the fleet.** After each win, pick a ship, an officer or weapons. Supplies are the only
   currency: repairs, shops and fleet size all compete for them. Watch the **Fleet DP cap**.
5. **Climb the Ascensions.** Winning unlocks harder levels per faction (Gamma to Omega), each worth a
   bigger Payout. Omega hides one last fight.

Every number (difficulty, rewards, caps, prices, map shape) can be tuned in
`data/config/settings.json` (`sts_*` keys).

## Compatibility

- **Other mods:** works with them. Factions and ships are read from the game at runtime, so modded
  factions show up as playable factions and as enemies. Tested alongside Nexerelin, Random
  Assortment of Things, Combat Chatter, MagicLib, LazyLib, LunaLib and GraphicsLib.
- **No dependencies.**
- **Existing saves:** safe to add to a game in progress. The Relay appears the next time you load.
- **Your campaign:** a Run changes nothing in it except the Payout. XP, skills, story points,
  marines, reputation, bounties, cargo and salaries are all restored when the Run ends.
- **Removing the mod:** not supported yet. A save that has used the mod needs it to load. Finish
  your Run first and keep a backup if you plan to remove it.

## Install

With [TriOS](https://github.com/wispborne/TriOS): click the **Install with TriOS** button above.

Manually:

1. Download the zip from the [Releases](https://github.com/PaulRouss3l/slay_the_sector/releases) page.
2. Extract it into `Starsector/mods/` (you get `mods/slay-the-sector/`).
3. Enable **Slay the Sector** in the launcher.

Version Checker (bundled with Nexerelin and LunaLib) will tell you when an update is out.

## Beta notes

This is a beta: balance is rough and bugs are expected. Reports are welcome on the forum thread or as
GitHub issues. Please attach your `starsector.log`; the mod's lines start with `[SlayTheSector]`.

## Credits

Thanks to the people whose work and open source code taught me how Starsector mods are made, and
whose mods I love playing:

- **wispborne**, for the Starsector IntelliJ Template this project started from, and for
  everything they do for the modding community. I learned to make my first mod with their tutorials.
- **Lukas22041**, for Second In Command and Random Assortment of Things, the inspiration for this
  mod and a great example of what a Starsector mod can be. I never play Starsector without Lukas's
  mods.
- **Histidine91**, for Nexerelin, a reference for nearly every campaign question. Histidine showed
  me how deep modding can go, and I've spent many evenings reading Nexerelin's code to learn how the
  game loop works.
- **tracesfx**, for the starsector-modding skill: the build, validation and log tools, and the API
  notes this project relies on. This toolkit made developing this mod much more efficient.

And to Alex and the Fractal Softworks team for Starsector <3.

## AI use disclaimer

Some or all assets and code in this mod were created using AI tools:

- **Code:** Claude Opus 5.5 (Anthropic), through Claude Code. It wrote most of the Java code, the
  build and test tooling configuration, and the design and API documentation.
- **Code review:** Claude was also used to review each milestone for bugs before release. No line of
  code reaches a release without a human review.
- **Art:** no AI image model. Map and node icons are vanilla Starsector sprites.

How it was made: I designed the mod (the Run, the Node types, the rules and the balance) and
directed every change. Each milestone was then played and tested in game by me, against a
checklist, before it was merged. The full source and the design notes are public in this
repository.

## License

[MIT](LICENSE): use, change, fork or redistribute the mod freely, as long as the copyright notice and
the license text stay with it. No warranty. The scripts in `tools/` are third-party (see
`tools/UPSTREAM.md`) and are not covered by this license.

## For developers

Building from source, project layout and release steps: [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).
Design and roadmap: [`docs/DESIGN.md`](docs/DESIGN.md), [`docs/ROADMAP.md`](docs/ROADMAP.md).

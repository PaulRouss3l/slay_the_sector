# 0001 — The Run lives in a contained star system inside a normal campaign

Date: 2026-10-19. Status: accepted.

## Context

A Run must (1) keep working with other mods' ships and factions, (2) never affect the player's
campaign beyond a designed Payout, (3) allow saving between fights, and (4) use real vanilla
battles. Two earlier attempts failed: relocating the fleet from inside a dialog froze the camera,
and running everything inside one dialog leaked items into the campaign, skipped vanilla
post-battle handling and made saving mid-Run impossible.

## Options

- **New game = Run** (replace the sector generator, as Nexerelin does). Cleanest isolation, but
  conflicts with Nexerelin and any mod touching sector generation, and there is no API to start a
  new game from inside a save.
- **Separate mode from the main menu** (mission-style). Not doable with a mod.
- **Dialog-only Run** inside a campaign. Tried; see Context.
- **Contained star system** entered by a jump transition (`doHyperspaceTransition`, RAT's method),
  with the Home fleet and cargo detached into saved Run state and the player fleet replaced by the
  Run fleet.

## Decision

Contained star system. The player flies between Node entities, fights through the vanilla fleet
encounter flow with our own battle setup, and can save whenever no dialog is open. A custom
canvas provides the StS-style node map. The Home fleet is held as a detached fleet in Run state
(no market), with an always-available restore at the Hub.

## Consequences

- Vanilla Refit, Fleet, recovery and loot screens work unchanged; no UI rebuild for them.
- Isolation is our responsibility: every Run start/end must swap fleet and cargo exactly once,
  and a load-time check restores stranded Home assets.
- Saved classes (Run state, Home fleet holder) are aliased in `configureXStream` and must never be
  renamed after release.
- The jump transition, battle-setup override, respawn intercept and canvas input are unproven in
  game: the Spike milestone verifies them before content is built.

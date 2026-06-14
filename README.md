# Slay the Sector — Starsector mod (VSCode)

A VSCode port of the [Starsector IntelliJ template](https://github.com/wispborne/Starsector-IntelliJ-Template),
adapted for **Linux** and Starsector **0.98a** (bundled Zulu JDK 17).

> **Project context & design**: see [`CLAUDE.md`](CLAUDE.md) and [`docs/`](docs/)
> ([DESIGN](docs/DESIGN.md), [ROADMAP](docs/ROADMAP.md), [API_NOTES](docs/API_NOTES.md)).

## Prerequisites (one-time)

1. **JDK 17** (the game ships a JRE only — no compiler):
   ```sh
   sudo apt install openjdk-17-jdk
   ```
   Confirm the path matches `.vscode/settings.json`:
   ```sh
   readlink -f $(which javac)   # expect /usr/lib/jvm/java-17-openjdk-amd64/...
   ```
2. **VSCode extensions**: `redhat.java` + `vscjava.vscode-java-debug`
   (VSCode prompts to install them from `.vscode/extensions.json`).
3. **Symlink the project into the game's `mods/`** so the launcher loads it:
   ```sh
   ln -s /home/ubik/localhost/perso/slay-the-sector \
         /home/ubik/Downloads/starsector/mods/slay-the-sector
   ```

## Layout

| Path | Purpose |
|------|---------|
| `mod_info.json` | Mod metadata; `modPlugin` points at the entry class |
| `src/com/slaythesector/` | Java sources |
| `jars/SlayTheSector.jar` | Build output (referenced by `mod_info.json`) |
| `build.sh` | Compile `src/` against the game jars → `jars/SlayTheSector.jar` |
| `run.sh` | Launch the game from the install dir with a debug agent on `:5005` |
| `.vscode/settings.json` | Wires game jars for IntelliSense (API autocomplete + javadoc) |
| `.vscode/tasks.json` | "Build mod" and "Run Starsector" tasks |
| `.vscode/launch.json` | "Debug Starsector" — build, launch, attach debugger |

## Workflow

- **Build only:** `Ctrl+Shift+B`, or `./build.sh`.
- **Run with debugging:** Run panel → **Debug Starsector** (F5). Builds, launches
  the game, attaches to `:5005`. Set breakpoints in `src/` and they hit.
- **Run without debugging:** `./run.sh`.

## Adjusting paths

All paths assume the install at `/home/ubik/Downloads/starsector`. If it moves, edit
the `STARSECTOR` var in `build.sh`, the `cd` in `run.sh`, and the jar globs in
`.vscode/settings.json`.

`gameVersion` in `mod_info.json` is set to `0.98a-RC5`. If the launcher shows a
"version mismatch" warning, set it to the exact version printed on the launcher
screen — the warning is cosmetic, not fatal.

# MODS SOURCES / EXAMPLES
https://github.com/wispborne/Starsector-IntelliJ-Template
https://github.com/Lukas22041/SecondInCommand
https://github.com/Histidine91/Nexerelin
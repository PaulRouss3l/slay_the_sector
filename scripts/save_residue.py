#!/usr/bin/env python3
"""List what Slay the Sector left in a save, to check it can be removed (docs/adr/0002).

    python3 scripts/save_residue.py              # newest save in $STARSECTOR_DIR/saves
    python3 scripts/save_residue.py <save dir>   # a given save folder (holding campaign.xml)

Exit code 1 when anything that breaks loading without the mod is left; 0 otherwise.
The mod's own entry in the save's mod list (modPluginClassName) is ignored: the game only warns.
"""
import os
import re
import sys

# Load fails without the mod: our classes, our custom entity types, our hullmods.
BLOCKERS = {
    "saved class (STS_ alias)": r"<STS__\w+",
    "saved class (package)": r"com\.slaythesector\.[\w.$]+",
    "custom entity type": r"\bsts_(?:hub_station|node_marker|spike_station)\b",
    "hullmod": r"\bsts_(?:run_logistics|command_uplink)\b",
}
# Harmless or not known yet: memory keys and the two factions (no API to remove a faction).
OTHERS = {
    "memory key": r"\$sts_\w+",
    "faction (needs the load test)": r"\bsts_(?:descent|ally)\b",
}
MOD_LIST_ENTRY = re.compile(r"<modPluginClassName>com\.slaythesector\.[\w.]+</modPluginClassName>")


def newest_save():
    root = os.path.join(os.environ.get("STARSECTOR_DIR", os.path.expanduser("~/Downloads/starsector")), "saves")
    saves = [os.path.join(root, d) for d in os.listdir(root) if d.startswith("save_")]
    if not saves:
        sys.exit("no save in " + root)
    return max(saves, key=lambda d: os.path.getmtime(os.path.join(d, "campaign.xml")))


def report(text, groups):
    found = False
    for label, pattern in groups.items():
        hits = list(re.finditer(pattern, text))
        if not hits:
            continue
        found = True
        names = sorted({h.group(0) for h in hits})
        print("  %-30s %5d  %s" % (label, len(hits), ", ".join(names[:6]) + (" ..." if len(names) > 6 else "")))
    if not found:
        print("  none")
    return found


def main():
    save = sys.argv[1] if len(sys.argv) > 1 else newest_save()
    with open(os.path.join(save, "campaign.xml"), encoding="utf-8", errors="replace") as f:
        text = MOD_LIST_ENTRY.sub("", f.read())
    print("save:", save)
    print("Breaks loading without the mod:")
    blocked = report(text, BLOCKERS)
    print("Other residue:")
    report(text, OTHERS)
    print("NOT removable" if blocked else "OK: nothing known to break loading without the mod")
    return 1 if blocked else 0


if __name__ == "__main__":
    sys.exit(main())

#!/usr/bin/env python3
"""Extract a single version's section from RELEASE_NOTES.md.

RELEASE_NOTES.md accumulates every past version's notes in one file (newest
first), each starting with a "## ClaudeMod vX.Y.Z" heading. Previously,
release.yml passed the *entire* file as the GitHub Release body, so every
release page showed the full history of every past version instead of just
the one being released (repo owner feedback, 2026-08-26: "リリースノートが
バカ長い。そのバージョンの情報だけを乗せるようにできないですか?").

This script pulls out only the section for one specific version, so it can
be used as the release body instead of the whole file.

Usage:
    python3 extract_release_notes.py RELEASE_NOTES.md 0.22.0 > CURRENT_RELEASE_NOTES.md

The version argument may be given with or without a leading "v" (so it works
directly with a git tag like "v0.22.0" as well as gradle.properties' bare
"0.22.0").
"""
import re
import sys


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: extract_release_notes.py <RELEASE_NOTES.md> <version>", file=sys.stderr)
        return 2

    notes_path, version = sys.argv[1], sys.argv[2]
    version = version.lstrip("vV")

    with open(notes_path, encoding="utf-8") as f:
        content = f.read()

    # Each version section starts with a line like "## ClaudeMod v0.22.0".
    heading_re = re.compile(r"^## ClaudeMod v(\S+)\s*$", re.MULTILINE)
    matches = list(heading_re.finditer(content))

    if not matches:
        print(f"::warning::No '## ClaudeMod vX.Y.Z' headings found in {notes_path}; "
              f"falling back to the whole file.", file=sys.stderr)
        sys.stdout.write(content)
        return 0

    target = None
    for i, m in enumerate(matches):
        if m.group(1) == version:
            target = i
            break

    if target is None:
        print(f"::warning::No section for version {version!r} found in {notes_path}; "
              f"falling back to the whole file.", file=sys.stderr)
        sys.stdout.write(content)
        return 0

    start = matches[target].end()
    end = matches[target + 1].start() if target + 1 < len(matches) else len(content)
    section = content[start:end].strip("\n")

    sys.stdout.write(section + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

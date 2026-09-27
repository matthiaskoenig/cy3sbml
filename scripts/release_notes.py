"""Generate `docs/release-notes.md` from the per-version release notes.

Every release writes its notes to `release-notes/<version>.md` (see
`docs/development/release.md`). This script concatenates all of them, newest version
first, into the single page the documentation site links to. Each file's own
Markdown headings are demoted by one level so they nest under the page's
`# Release notes` heading.

Run it before `zensical build`, the generated file is gitignored:

```bash
uv run --no-project --python 3.14 python scripts/release_notes.py
```

Self-check the helper functions without touching the filesystem:

```bash
python scripts/release_notes.py --check
```
"""

import re
import sys
from pathlib import Path

REPO_DIR: Path = Path(__file__).parent.parent
RELEASE_NOTES_DIR: Path = REPO_DIR / "release-notes"
OUTPUT_PATH: Path = REPO_DIR / "docs" / "release-notes.md"

# an ATX heading, i.e. one or more '#' followed by whitespace, at the start
# of a line; issue references such as "#395" have no space and do not match
_HEADING_RE = re.compile(r"^(#+)(\s)", re.MULTILINE)


def version_key(stem: str) -> tuple[int, ...]:
    """Parse a release notes filename stem into a numeric sort key.

    Args:
        stem: filename stem, e.g. "0.5.10".

    Returns:
        The dot separated parts as integers, e.g. (0, 5, 10), so that
        versions sort numerically instead of lexicographically
        (0.5.10 sorts after 0.5.9).
    """
    return tuple(int(part) for part in stem.split("."))


def demote_headings(markdown: str) -> str:
    """Shift every Markdown ATX heading in `markdown` down by one level.

    Args:
        markdown: content of one release notes file.

    Returns:
        The content with every heading level increased by one, e.g. a `#`
        heading becomes a `##` heading. Content without headings, such as
        the older release notes files, is returned unchanged.
    """
    return _HEADING_RE.sub(r"#\1\2", markdown)


def release_note_files() -> list[Path]:
    """Return the release notes files, newest version first.

    Returns:
        The files of `release-notes/*.md`, sorted by their filename stem
        parsed as a version, newest first.
    """
    files = RELEASE_NOTES_DIR.glob("*.md")
    return sorted(files, key=lambda path: version_key(path.stem), reverse=True)


def release_notes_markdown(files: list[Path]) -> str:
    """Build the combined `docs/release-notes.md` content.

    Args:
        files: release notes files, in the order they should appear.

    Returns:
        The full Markdown content of the combined page.
    """
    sections = [demote_headings(path.read_text()).strip() for path in files]
    return "# Release notes\n\n" + "\n\n".join(sections) + "\n"


def main() -> None:
    """Write `docs/release-notes.md` from `release-notes/*.md`."""
    files = release_note_files()
    OUTPUT_PATH.write_text(release_notes_markdown(files))
    print(f"Wrote '{OUTPUT_PATH}' from {len(files)} release notes files.")


def check() -> None:
    """Run a self-check of the helper functions, without touching the disk."""
    assert version_key("0.5.9") < version_key("0.5.10"), "expected numeric order"
    assert version_key("0.4.0") < version_key("0.5.0")
    assert demote_headings("# Title\n\ntext\n\n## Sub") == "## Title\n\ntext\n\n### Sub"
    assert demote_headings("no heading here") == "no heading here"
    assert demote_headings("fixed issue #395") == "fixed issue #395"
    print("self-check passed")


if __name__ == "__main__":
    if "--check" in sys.argv[1:]:
        check()
    else:
        main()

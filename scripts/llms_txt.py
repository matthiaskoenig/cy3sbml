"""Generate the agent facing files of the documentation site.

Static site generators render the documentation for browsers; agents and large
language models are better served by markdown. This script writes the files of
the [llms.txt convention](https://llmstxt.org/) into the built site:

- `llms.txt`, an index of the documentation with one annotated link per page,
- `llms-full.txt`, the complete documentation as a single markdown file,
- a markdown copy of every page next to its html, so that the links resolve.

Run it after `zensical build`:

```bash
uvx --python 3.14 --with-requirements docs/requirements.txt zensical build --clean
uv run --no-project --python 3.14 python scripts/llms_txt.py
```
"""

import re
import tomllib
from pathlib import Path
from typing import Any, NamedTuple

REPO_DIR: Path = Path(__file__).parent.parent
DOCS_DIR: Path = REPO_DIR / "docs"
SITE_DIR: Path = REPO_DIR / "site"
CONFIG_PATH: Path = REPO_DIR / "zensical.toml"


class Page(NamedTuple):
    """A page of the documentation.

    Attributes:
        section: title of the nav section the page belongs to.
        title: title of the page in the nav.
        path: path of the markdown source relative to `docs/`.
    """

    section: str
    title: str
    path: str


class SiteConfig(NamedTuple):
    """The settings of the zensical configuration used by this script.

    Attributes:
        site_name: name of the documentation site.
        site_description: one line description of the site.
        site_url: URL of the published site, without a trailing slash.
        repo_url: URL of the source repository.
        pages: pages of the nav, in the order of the nav.
    """

    site_name: str
    site_description: str
    site_url: str
    repo_url: str
    pages: list[Page]


def _config_str(project: dict[str, Any], key: str) -> str:
    """Return a string setting of the `project` table.

    Args:
        project: `project` table of the zensical configuration.
        key: name of the setting.

    Returns:
        The value of the setting.

    Raises:
        SystemExit: if the setting is missing or not a string.
    """
    value = project.get(key)
    if not isinstance(value, str):
        raise SystemExit(f"'{CONFIG_PATH}': `project.{key}` must be a string.")
    return value


def read_config() -> SiteConfig:
    """Read the zensical configuration.

    Returns:
        The settings of the `project` table of `zensical.toml`.
    """
    with CONFIG_PATH.open("rb") as f_config:
        project: dict[str, Any] = tomllib.load(f_config)["project"]
    return SiteConfig(
        site_name=_config_str(project, "site_name"),
        site_description=_config_str(project, "site_description"),
        site_url=_config_str(project, "site_url").rstrip("/"),
        repo_url=_config_str(project, "repo_url"),
        pages=parse_nav(project["nav"]),
    )


def parse_nav(nav: list[Any], section: str = "Documentation") -> list[Page]:
    """Flatten the nav of the configuration into a list of pages.

    Args:
        nav: nav entries, i.e., single element dictionaries mapping a title to
            either a path or a list of nav entries.
        section: title of the section the entries belong to.

    Returns:
        The pages in the order of the nav.
    """
    pages: list[Page] = []
    for entry in nav:
        for title, value in entry.items():
            if isinstance(value, list):
                # a subsection keeps the title of its top level section, the
                # sections of `llms.txt` are flat
                sub_section = section if section != "Documentation" else title
                pages.extend(parse_nav(value, section=sub_section))
            elif isinstance(value, str):
                pages.append(Page(section=section, title=title, path=value))
            else:
                raise SystemExit(f"'{CONFIG_PATH}': invalid nav entry '{title}'.")
    return pages


def summary(markdown: str) -> str:
    """Extract a one line summary from the markdown of a page.

    The first paragraph which is neither a heading, a badge, an image nor a
    code block is used, reduced to its first sentence.

    Args:
        markdown: content of the page.

    Returns:
        The summary, empty if no paragraph was found.
    """
    for block in markdown.split("\n\n"):
        line = block.strip()
        if not line or line.startswith(("#", "!", "[", "```", "|", ">", ":::")):
            continue
        sentence = line.split(". ")[0].replace("\n", " ").strip()
        return re.sub(r"\[([^]]+)]\([^)]+\)", r"\1", sentence).rstrip(":.") + "."
    return ""


def page_markdown(page: Page) -> str:
    """Return the markdown of a page.

    Args:
        page: page of the documentation.

    Returns:
        The source markdown.
    """
    return (DOCS_DIR / page.path).read_text()


def sections(pages: dict[Page, str]) -> dict[str, list[Page]]:
    """Group the pages by their section, in the order of the nav.

    Args:
        pages: markdown of every page of the documentation.

    Returns:
        The pages of every section, keyed by the section title.
    """
    grouped: dict[str, list[Page]] = {}
    for page in pages:
        grouped.setdefault(page.section, []).append(page)
    return grouped


def write_llms_txt(pages: dict[Page, str], config: SiteConfig) -> None:
    """Write the `llms.txt` index of the documentation.

    Args:
        pages: markdown of every page of the documentation.
        config: settings of the zensical configuration.
    """
    site_url = config.site_url
    lines: list[str] = [
        f"# {config.site_name}",
        "",
        f"> {config.site_description}",
        "",
        "The links below point to the markdown source of the documentation. "
        f"[llms-full.txt]({site_url}/llms-full.txt) contains all of it in a "
        "single file.",
    ]

    for section, section_pages in sections(pages).items():
        lines += ["", f"## {section}", ""]
        for page in section_pages:
            url = f"{site_url}/{page.path}"
            summary_text = summary(pages[page])
            lines.append(f"- [{page.title}]({url}): {summary_text}".rstrip())

    lines += [
        "",
        "## Optional",
        "",
        f"- [Repository]({config.repo_url}): source code, issues and releases.",
        f"- [Sitemap]({site_url}/sitemap.xml): all pages of the rendered site.",
        "",
    ]
    (SITE_DIR / "llms.txt").write_text("\n".join(lines))


def write_llms_full_txt(pages: dict[Page, str], config: SiteConfig) -> None:
    """Write the complete documentation as a single markdown file.

    Args:
        pages: markdown of every page of the documentation.
        config: settings of the zensical configuration.
    """
    site_url = config.site_url
    lines: list[str] = [
        f"# {config.site_name}",
        "",
        f"> {config.site_description}",
        "",
        f"The complete documentation from {site_url}, one section per page.",
        "",
    ]
    for page, markdown in pages.items():
        lines += [
            "---",
            "",
            f"<!-- {site_url}/{page.path} -->",
            "",
            markdown.strip(),
            "",
        ]
    (SITE_DIR / "llms-full.txt").write_text("\n".join(lines))


def write_markdown(pages: dict[Page, str]) -> None:
    """Write the markdown of every page next to its rendered html.

    Args:
        pages: markdown of every page of the documentation.
    """
    for page, markdown in pages.items():
        path = SITE_DIR / page.path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(markdown)


def main() -> None:
    """Generate the agent facing files of the built site."""
    if not SITE_DIR.exists():
        raise SystemExit(f"'{SITE_DIR}' does not exist, run `zensical build` first.")

    config = read_config()
    pages = {page: page_markdown(page) for page in config.pages}

    write_markdown(pages)
    write_llms_txt(pages, config)
    write_llms_full_txt(pages, config)
    print(f"llms.txt, llms-full.txt and {len(pages)} markdown pages in '{SITE_DIR}'")


if __name__ == "__main__":
    main()

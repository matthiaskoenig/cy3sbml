# Contributing

Bug reports, questions and code contributions are welcome. Use the latest releases of
Cytoscape and cy3sbml before you report a problem, only these are supported.

## Report an issue

Check the [issue tracker](https://github.com/matthiaskoenig/cy3sbml/issues) for an
existing issue first. If there is none, open a new issue with:

- a clear description, and for a bug the steps to reproduce it,
- the SBML file, or a link to it, if the problem is about a model,
- the versions of cy3sbml, Cytoscape, Java and the operating system. Cytoscape shows them
  in **Help → About...**, for example:

  ```
  Version: 3.10.4
  Java: 17.0.15 by Eclipse Adoptium
  OS: Linux 6.8.0 - amd64
  ```

- the cy3sbml log file `~/CytoscapeConfiguration/cy3sbml/cy3sbml-v<version>.log`.

## Contribute code

1. Open an issue, or comment on an existing one, so that the change can be discussed
   first.
2. Fork the repository and create a branch from `develop`, for example
   `fix-fbc-bounds`. Do not work on `develop` or `main` directly.
3. Set up the build as described in [Building](building.md), and install the
   [pre-commit hook](quality.md#formatting).
4. Make the change in commits of logical units. Add tests for it
   ([Testing](testing.md)), and run `./mvnw verify` and the checks in
   [Code quality](quality.md).
5. Check the change in Cytoscape if it affects the import or the GUI.
6. Open a pull request against `develop`. Describe the change and how you tested it.

The pull request needs the checks `tests`, `format`, `lint` and `docs` to pass, and is
merged with squash or rebase, so `develop` has a linear history. See
[Release process](release.md#branches).

For changes of the documentation only, an issue is not needed.

## Documentation

This documentation is built with [Zensical](https://zensical.org) from the Markdown files
in `docs/`. The navigation is in `zensical.toml`. Build it locally with
[uv](https://docs.astral.sh/uv/):

```bash
uv run --no-project --python 3.14 python scripts/release_notes.py
uvx --python 3.14 --with-requirements docs/requirements.txt zensical build --clean
uv run --no-project --python 3.14 python scripts/llms_txt.py
```

The first command generates `docs/release-notes.md` from `release-notes/`; do not edit
that file. The site is written to `site/`. Use `zensical serve` instead of
`zensical build` for a local preview that reloads on changes. The build must not show
warnings; it reports broken links.

## Resources

- [GitHub documentation](https://docs.github.com)
- [Creating a pull request from a fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork)

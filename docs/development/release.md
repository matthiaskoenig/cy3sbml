# Release process

## Branches

| Branch | Role |
|---|---|
| `develop` | Default branch. All pull requests go here. |
| `main` | The latest release. Only the release workflow moves it, by a fast-forward to the released commit. |
| feature branches | One per change, deleted after the merge. |

The branch and tag rules are GitHub rulesets, stored as JSON in `.github/rulesets/`:

- `develop.json`: no deletion and no force push, linear history, changes only by pull
  request with resolved review threads, merge by squash or rebase, and the required
  status checks `tests`, `format`, `lint` and `docs`.
- `main.json`: no deletion, no force push, linear history.
- `tags.json`: tags cannot be deleted or moved.

`.github/rulesets/apply.sh` applies the rulesets and the merge settings of the
repository (squash and rebase merges, no merge commits, auto-merge, deletion of merged
branches) with the GitHub CLI. It needs admin rights on the repository and updates
existing rulesets, so run it again after every change of the JSON files:

```bash
gh auth login
.github/rulesets/apply.sh matthiaskoenig/cy3sbml
```

## Release a version

1. Open a pull request that sets the release version in `pom.xml`, for example `0.5.1`.
2. In the same pull request, write the release notes in
   `release-notes/<version>.md`, for example `release-notes/0.5.1.md`. They are the text
   of the GitHub release and appear on the [Release notes](../release-notes.md) page.
3. Merge the pull request into `develop`.
4. Tag the merged commit on `develop` and push the tag:

    ```bash
    git checkout develop
    git pull
    git tag v<version>
    git push origin v<version>
    ```

5. The workflow `.github/workflows/release.yml` runs for the tag. It builds and tests
   the app with `./mvnw verify`, checks that `release-notes/<version>.md` and
   `target/cy3sbml-<version>.jar` exist, creates the GitHub release with the jar and its
   MD5 and SHA-1 checksums, and fast-forwards `main` to the tag.
6. Upload the jar of the GitHub release to the
   [Cytoscape App Store](https://apps.cytoscape.org/apps/cy3sbml).
7. Open a pull request that sets the next development version in `pom.xml`.

The documentation is published from `develop` by the workflow
`.github/workflows/docs.yml` to <https://matthiaskoenig.github.io/cy3sbml/>.

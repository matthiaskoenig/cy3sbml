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
  status checks `tests`, `format`, `lint`, `python` and `docs`.
- `main.json`: no deletion, no force push, linear history.
- `tags.json`: tags cannot be deleted or moved.

`.github/rulesets/apply.sh` applies the rulesets and the merge settings of the
repository (squash and rebase merges, no merge commits, auto-merge, updating pull request
branches, deletion of merged branches) with the GitHub CLI. It needs admin rights on the repository and updates
existing rulesets. A change of the JSON files takes effect only after `apply.sh` is run
again:

```bash
gh auth login
.github/rulesets/apply.sh matthiaskoenig/cy3sbml
```

## Versions and release notes

The version in `pom.xml` on `develop` is always the next release, without a `-SNAPSHOT`
suffix. After a release, a pull request starts the development of the next version: it
sets the version in `pom.xml`, for example `0.7.0`, and adds the release notes
`release-notes/<version>.md`, for example `release-notes/0.7.0.md`. Every pull request
with a user visible change adds its note to this file. The release notes are the text of
the GitHub release and appear on the [Release notes](../release-notes.md) page, the notes
of the version in development included.

## Release a version

1. Open a pull request that completes the release notes `release-notes/<version>.md` and
   updates the documentation, and merge it into `develop`.
2. Tag the merged commit on `develop` and push the tag:

    ```bash
    git checkout develop
    git pull
    git tag v<version>
    git push origin v<version>
    ```

3. The workflow `.github/workflows/release.yml` runs for the tag. It checks that
   `release-notes/<version>.md` exists, builds and tests the app and its javadoc with
   `./mvnw -Pjavadoc verify`, checks that `target/cy3sbml-<version>.jar` and
   `target/cy3sbml-<version>-javadoc.jar` exist, creates the GitHub release with the app
   jar, the javadoc jar and the `pom.xml` of the release, each with its MD5 and SHA-1
   checksum, and fast-forwards `main` to the tag.
4. Upload the jar of the GitHub release to the
   [Cytoscape App Store](https://apps.cytoscape.org/apps/cy3sbml).
5. Open the pull request that starts the development of the next version.

The documentation is published from `develop` by the workflow
`.github/workflows/docs.yml` to <https://matthiaskoenig.github.io/cy3sbml/>.

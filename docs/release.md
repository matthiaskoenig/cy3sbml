# cy3sbml Release Instructions

1. Open a PR to bump the version in `pom.xml`
2. Write release notes in `release-notes/<version>.md` (e.g., `release-notes/0.5.1.md`)
3. Merge the PR to `develop`
4. Tag the release on `develop`: `git tag v<version>` (e.g., `git tag v0.5.1`)
5. Push the tag: `git push origin v<version>`
6. The GitHub Actions workflow runs automatically, builds the app, creates the GitHub release, and fast-forwards `main` to the latest release
7. Upload the jar from the GitHub release to the App Store (https://apps.cytoscape.org/apps/cy3sbml)
8. Open a PR to bump the version in `pom.xml` for the next development cycle

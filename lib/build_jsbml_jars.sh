#!/bin/bash
########################################################
# Build JSBML (core & packages) from source and install
# the jars as an in-project Maven dependency under
# lib/cy3sbml-dep, pinned to one JSBML commit.
#
# Usage:
#   ./build_jsbml_jars.sh <jsbml-commit>
#
# Requires ant on the PATH (JSBML's own build tool). This
# machine does not have ant installed and cannot install it
# (no sudo); see "Re-versioning without a rebuild" below for
# what to do in that case.
#
# What it does:
#   1. Clones JSBML into $JSBMLCODE (default $HOME/git/jsbml)
#      if it is not already there, and checks out the given
#      commit.
#   2. Derives a version string from that commit:
#        1.7-<commit date, YYYYMMDD>-<8-char short sha>
#      so the installed jars record exactly which JSBML
#      commit they came from.
#   3. Runs `ant jar` to build the core jar (core/build/) and
#      the package jars (build/).
#   4. Installs core and every package jar into
#      lib/cy3sbml-dep via `mvn install:install-file`, all
#      under the same derived version.
#
# After running this script:
#   - update the `jsbml.version` property in pom.xml to the
#     version this script prints,
#   - remove the previous version's directories under
#     lib/cy3sbml-dep/jsbml*/ (the old jars are superseded,
#     not needed side by side),
#   - run `./mvnw -B -q clean verify` to confirm the build
#     resolves the new jars.
#
# jtidy (the third-party HTML Tidy library used by
# jsbml-tidy) is not part of JSBML and is not touched here.
########################################################
set -euo pipefail

JSBML_COMMIT="${1:?usage: build_jsbml_jars.sh <jsbml commit>}"

# JSBML source checkout (cloned on demand)
JSBMLCODE="${JSBMLCODE:-$HOME/git/jsbml}"

# cy3sbml checkout (this repository)
CY3SBMLCODE="${CY3SBMLCODE:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"

# lib/cy3sbml-dep is a Maven repository rooted at lib/ (see the
# <repository> with url file:${project.basedir}/lib in pom.xml);
# groupId cy3sbml-dep is itself the first path segment under it.
LOCAL_REPO="$CY3SBMLCODE/lib"

if [ ! -d "$JSBMLCODE" ]; then
    echo "Cloning JSBML into $JSBMLCODE"
    git clone https://github.com/sbmlteam/jsbml.git "$JSBMLCODE"
fi

cd "$JSBMLCODE"
git fetch origin
git checkout "$JSBML_COMMIT"

JSBML_VERSION="1.7-$(git -C "$JSBMLCODE" show -s --format=%cd --date=format:%Y%m%d "$JSBML_COMMIT")-$(git -C "$JSBMLCODE" rev-parse --short=8 "$JSBML_COMMIT")"
echo "JSBML commit $JSBML_COMMIT -> version $JSBML_VERSION"

# clean old build files
rm -rf "$JSBMLCODE/build"

# build core and extensions
# core available from $JSBMLCODE/core/build/
# extensions from $JSBMLCODE/build/
ant jar

cd "$CY3SBMLCODE"

########################################################
# install in the local repository
echo "Installing JSBML $JSBML_VERSION into $LOCAL_REPO"

# Maven-format checksum sidecar: the hash only, lower case, no
# filename and no trailing newline (matches the .md5/.sha1 files
# already checked in for e.g. lib/cy3sbml-dep/jtidy/r938/).
write_checksums() {
    local target="$1"
    printf '%s' "$(md5sum "$target" | awk '{print $1}')" > "$target.md5"
    printf '%s' "$(sha1sum "$target" | awk '{print $1}')" > "$target.sha1"
}

install_jar() {
    local artifact_id="$1"
    local jar_file="$2"
    local artifact_dir="$LOCAL_REPO/cy3sbml-dep/$artifact_id/$JSBML_VERSION"
    local installed_jar="$artifact_dir/$artifact_id-$JSBML_VERSION.jar"
    local installed_pom="$artifact_dir/$artifact_id-$JSBML_VERSION.pom"
    local metadata="$LOCAL_REPO/cy3sbml-dep/$artifact_id/maven-metadata-local.xml"

    ./mvnw -B -q install:install-file \
        -DlocalRepositoryPath="$LOCAL_REPO" \
        -DgroupId=cy3sbml-dep \
        -DartifactId="$artifact_id" \
        -Dversion="$JSBML_VERSION" \
        -Dfile="$jar_file" \
        -Dpackaging=jar \
        -DgeneratePom=true \
        -DcreateChecksum=true

    # install:install-file 3.x ignores -DcreateChecksum, so write the
    # sidecars ourselves.
    write_checksums "$installed_jar"
    write_checksums "$installed_pom"
    write_checksums "$metadata"
}

install_jar jsbml         "$JSBMLCODE/core/build/jsbml-$JSBML_VERSION.jar"
install_jar jsbml-qual    "$JSBMLCODE/build/jsbml-qual-$JSBML_VERSION.jar"
install_jar jsbml-layout  "$JSBMLCODE/build/jsbml-layout-$JSBML_VERSION.jar"
install_jar jsbml-comp    "$JSBMLCODE/build/jsbml-comp-$JSBML_VERSION.jar"
install_jar jsbml-fbc     "$JSBMLCODE/build/jsbml-fbc-$JSBML_VERSION.jar"
install_jar jsbml-groups  "$JSBMLCODE/build/jsbml-groups-$JSBML_VERSION.jar"
install_jar jsbml-distrib "$JSBMLCODE/build/jsbml-distrib-$JSBML_VERSION.jar"
install_jar jsbml-tidy    "$JSBMLCODE/build/jsbml-tidy-$JSBML_VERSION.jar"

echo "Done. Set <jsbml.version>$JSBML_VERSION</jsbml.version> in pom.xml,"
echo "remove the old version directories under lib/cy3sbml-dep/jsbml*/,"
echo "and run ./mvnw -B -q clean verify."

########################################################
# Re-versioning without a rebuild (how the jars currently in
# lib/cy3sbml-dep got their pinned version)
#
# ant is not installed on this machine, and agents working in
# this repo cannot sudo to install it. Rather than rebuild
# JSBML from source, the jars already committed under
# lib/cy3sbml-dep (built 2025-05-20, commit 2837954af "JSBML
# updates") were re-installed in place, unchanged, under a
# pinned version derived from their own build provenance:
#
#   - Each core/package jar's META-INF/MANIFEST.MF was read
#     with `unzip -p <jar> META-INF/MANIFEST.MF`.
#   - The core and jsbml-tidy jars carry
#     `Bundle-Revision: 22659a76927b24e23175563e2224bc56c05a0e74`
#     and `Built-Date: May 20 2025` (Build 20250520-1354),
#     i.e. the exact JSBML source commit used for that build.
#   - `git log --diff-filter=A -- lib/cy3sbml-dep` confirms all
#     eight jars (core, qual, layout, comp, fbc, groups,
#     distrib, tidy) were added together in commit 2837954af,
#     so they came from that same checkout/build.
#   - That commit's committer date (via the GitHub API, since
#     no local JSBML checkout was available) is 2023-01-03,
#     giving JSBML_VERSION=1.7-20230103-22659a76.
#
# The re-versioning itself was a plain re-install of the
# existing jars under the new version, with no source changes:
#
#   JSBML_VERSION=1.7-20230103-22659a76
#   for artifact in jsbml jsbml-qual jsbml-layout jsbml-comp \
#                    jsbml-fbc jsbml-groups jsbml-distrib jsbml-tidy; do
#       ./mvnw -B -q install:install-file \
#           -DlocalRepositoryPath=lib \
#           -DgroupId=cy3sbml-dep \
#           -DartifactId="$artifact" \
#           -Dversion="$JSBML_VERSION" \
#           -Dfile=<path to the existing jar for $artifact> \
#           -Dpackaging=jar -DgeneratePom=true -DcreateChecksum=true
#   done
#
# -DcreateChecksum=true is a no-op on this maven-install-plugin
# version, so the .jar.md5/.jar.sha1/.pom.md5/.pom.sha1 and
# maven-metadata-local.xml.md5/.sha1 sidecars for each artifact
# were written by hand afterwards with the same write_checksums()
# helper used above (hash only, no filename, no trailing newline),
# matching the format of the pre-existing lib/cy3sbml-dep/jtidy/
# checksums.
#
# The next real JSBML upgrade should go through the normal
# path above (build from source with ant on a machine that has
# it, or in CI) rather than repeating this fallback.
########################################################

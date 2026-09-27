#!/bin/bash
########################################################
# Build JSBML (core & packages) from source and install
# the jars as an in-project Maven dependency under
# lib/cy3sbml-dep, pinned to one JSBML commit.
#
# Usage:
#   ./build_jsbml_jars.sh <jsbml-commit>
#
# Requires ant (JSBML's own build tool) and a JDK 17 on the
# PATH (JAVA_HOME pointing to it). JSBML compiles with
# -source/-target 1.7 (jsbml-build.properties), so the class
# files (major version 51) load on Cytoscape's Java 17
# runtime; JDK 17 is the newest JDK that still accepts
# -source 1.7.
#
# What it does:
#   1. Clones JSBML into $JSBMLCODE (default $HOME/git/jsbml)
#      if it is not already there, and checks out the given
#      commit.
#   2. Derives a version string from that commit:
#        1.7-<commit date, YYYYMMDD>-<8-char short sha>
#      so the installed jars record exactly which JSBML
#      commit they came from.
#   3. Runs `ant -Dversion=<that version> jar` to build the
#      core jar (core/build/) and the package jars (build/).
#      The command line property overrides the per-module
#      version (1.7-SNAPSHOT, 2.1-b1, ...), so every jar is
#      named after and carries (Bundle-Version) the pinned
#      version.
#   4. Installs core and every package jar into
#      lib/cy3sbml-dep via `mvn install:install-file`, all
#      under the same derived version, with md5/sha1
#      checksum sidecars, and removes the previous version
#      (its directory and its maven-metadata-local.xml entry):
#      the old jars are superseded, not kept side by side.
#
# After running this script:
#   - update the `jsbml.version` and `jsbml.osgi.version`
#     properties in pom.xml to the versions this script prints,
#   - run `./mvnw -B -q clean verify` to confirm the build
#     resolves the new jars.
#
# jtidy (the third-party HTML Tidy library used by
# jsbml-tidy) is not part of JSBML and is not touched here.
#
# How the jars currently in lib/cy3sbml-dep were built
# (JSBML commit 8192a8a7010676202eb666413213879c5a6a555e,
# 2026-09-07, version 1.7-20260907-8192a8a7), on Ubuntu without
# root access, so ant was not installed system-wide:
#
#   # Apache Ant 1.10.18 binary distribution, checked against
#   # its published sha512
#   curl -LO https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.18-bin.tar.gz
#   curl -LO https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.18-bin.tar.gz.sha512
#   echo "$(cut -d' ' -f1 apache-ant-1.10.18-bin.tar.gz.sha512)  apache-ant-1.10.18-bin.tar.gz" | sha512sum -c
#   tar xzf apache-ant-1.10.18-bin.tar.gz
#
#   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # OpenJDK 17.0.20
#   export PATH="$PWD/apache-ant-1.10.18/bin:$PATH"
#   JSBMLCODE=/path/to/jsbml-clone lib/build_jsbml_jars.sh 8192a8a7
#
# The svn "Execute failed" messages in the ant output are
# harmless: the build probes for svn, then falls back to git
# for the revision recorded in the manifests (Bundle-Revision).
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
ant -Dversion="$JSBML_VERSION" jar

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

    # The old version is superseded, not kept side by side: drop its
    # directory and its <version> entry in the metadata.
    local version_dir
    for version_dir in "$LOCAL_REPO/cy3sbml-dep/$artifact_id"/*/; do
        if [ "$(basename "$version_dir")" != "$JSBML_VERSION" ]; then
            rm -rf "$version_dir"
        fi
    done
    sed -i "\\|<version>|{\\|<version>$JSBML_VERSION</version>|!d}" "$metadata"

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

JSBML_OSGI_VERSION="$(echo "$JSBML_VERSION" | sed -E 's/^([0-9]+)\.([0-9]+)-/\1.\2.0./')"
echo "Done. Set <jsbml.version>$JSBML_VERSION</jsbml.version> and"
echo "<jsbml.osgi.version>$JSBML_OSGI_VERSION</jsbml.osgi.version> in pom.xml"
echo "and run ./mvnw -B -q clean verify."


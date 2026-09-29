"""Update the JSBML jars in `lib/cy3sbml-dep` to a JSBML commit.

JSBML is not taken from Maven Central: cy3sbml needs fixes that are not
released there. The jars are built from the JSBML sources instead and stored in
`lib/cy3sbml-dep`, a Maven repository inside the project, pinned to one commit
of https://github.com/sbmlteam/jsbml by the `jsbml.version` property in
`pom.xml`. See `docs/development/dependencies.md`.

Update to the latest commit of the JSBML `master` branch, or to a given commit,
branch or tag:

```bash
uv run --no-project --python 3.14 python scripts/update_jsbml.py
uv run --no-project --python 3.14 python scripts/update_jsbml.py <jsbml-ref>
```

`--repository <url or path>` builds from another JSBML repository, for example
a fork with fixes that are not merged into JSBML yet:

```bash
uv run --no-project --python 3.14 python scripts/update_jsbml.py \
    --repository https://github.com/matthiaskoenig/jsbml <commit>
```

The script needs git and a JDK 17 (JSBML compiles for Java 7, which JDK 20 and
newer no longer support). It

1. clones JSBML into a temporary directory and checks out the commit,
2. builds the core and package jars with JSBML's own Ant build, with the version
   `<JSBML version>-<commit date>-<short sha>`, e.g. `1.7-20260907-8192a8a7`
   (Ant is downloaded once, checked against its published SHA-512, and cached),
3. removes the JUnit test classes and test data, which JSBML's Ant build puts
   into the jars next to the production classes,
4. writes the jars with a minimal POM and SHA-1 checksums into
   `lib/cy3sbml-dep` and removes the previous version (and a copy of the new
   version in `~/.m2/repository`, which a rebuild would otherwise not replace),
5. sets the properties `jsbml.version` and `jsbml.osgi.version` in `pom.xml`.

Nothing changes when the commit is already the pinned one; `--force` rebuilds
it anyway. `--summary <file>` writes a Markdown summary of the update (the JSBML
commits it brings in), used as the pull request description by the
`update-jsbml` workflow.

Self-check the helper functions without touching the filesystem:

```bash
python scripts/update_jsbml.py --check
```
"""

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path

REPO_DIR: Path = Path(__file__).parent.parent
POM_PATH: Path = REPO_DIR / "pom.xml"
# lib/ is the Maven repository (see the in-project <repository> in pom.xml), the
# groupId cy3sbml-dep is the first path segment under it
DEP_DIR: Path = REPO_DIR / "lib" / "cy3sbml-dep"
GROUP_ID: str = "cy3sbml-dep"
# Maven copies the jars into the local repository on first use and never looks at
# lib/ again for that version, so a rebuild of the pinned version (--force) must
# remove that copy
M2_GROUP_DIR: Path = Path.home() / ".m2" / "repository" / GROUP_ID

JSBML_URL: str = "https://github.com/sbmlteam/jsbml"
JSBML_DEFAULT_REF: str = "master"

# Apache Ant, the build tool of JSBML; the checksum is the published SHA-512 of
# https://archive.apache.org/dist/ant/binaries/apache-ant-<version>-bin.tar.gz.sha512
ANT_VERSION: str = "1.10.18"
ANT_SHA512: str = (
    "c510d744876d8da48dabc9495b023b6ec5284a0f18b40ee50d98ce099e791ca5"
    "c73e3cd4c80d3c90d3efc03f9620be43aad0a373ed47198284f7e3373e0db2c3"
)
ANT_URL: str = (
    f"https://archive.apache.org/dist/ant/binaries/apache-ant-{ANT_VERSION}-bin.tar.gz"
)

# JDK versions that compile JSBML's -source/-target 1.7 (jsbml-build.properties)
JDK_MIN: int = 17
JDK_MAX: int = 19


@dataclass(frozen=True)
class Module:
    """One JSBML jar that cy3sbml depends on."""

    artifact_id: str
    # module directory in the JSBML sources, holding src/ and test/
    source_dir: str
    # directory of the jar built by `ant jar`, relative to the JSBML sources
    build_dir: str


MODULES: tuple[Module, ...] = (
    Module("jsbml", "core", "core/build"),
    Module("jsbml-qual", "extensions/qual", "build"),
    Module("jsbml-layout", "extensions/layout", "build"),
    Module("jsbml-comp", "extensions/comp", "build"),
    Module("jsbml-fbc", "extensions/fbc", "build"),
    Module("jsbml-groups", "extensions/groups", "build"),
    Module("jsbml-distrib", "extensions/distrib", "build"),
    Module("jsbml-tidy", "modules/tidy", "build"),
)

# the jar index lists the packages of the jar, including those of the removed
# test classes; it is optional, so it is dropped instead of being rewritten
DROPPED_ENTRIES: frozenset[str] = frozenset({"META-INF/INDEX.LIST"})

PACKAGE_RE: re.Pattern[str] = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)


def jsbml_version(base_version: str, commit_date: str, sha: str) -> str:
    """Build the Maven version of the jars of a JSBML commit.

    Args:
        base_version: version of the JSBML sources without "-SNAPSHOT", e.g. "1.7".
        commit_date: commit date as YYYYMMDD, e.g. "20260907".
        sha: full or abbreviated commit hash.

    Returns:
        The version, e.g. "1.7-20260907-8192a8a7". It sorts by commit date and
        records the commit the jars are built from.
    """
    return f"{base_version}-{commit_date}-{sha[:8]}"


def osgi_version(version: str) -> str:
    """Convert a version from `jsbml_version` to an OSGi version.

    Args:
        version: e.g. "1.7-20260907-8192a8a7".

    Returns:
        The OSGi version (major.minor.micro.qualifier), e.g.
        "1.7.0.20260907-8192a8a7", used for the exported org.sbml.jsbml packages.
    """
    match = re.fullmatch(r"(\d+)\.(\d+)-(\d{8}-[0-9a-f]+)", version)
    if match is None:
        raise ValueError(f"not a JSBML version: '{version}'")
    major, minor, qualifier = match.groups()
    return f"{major}.{minor}.0.{qualifier}"


def version_sha(version: str) -> str:
    """Return the abbreviated commit hash of a version from `jsbml_version`."""
    return version.rsplit("-", 1)[1]


def pom_property(pom: str, name: str) -> str:
    """Return the value of the property `name` in the POM content `pom`."""
    match = re.search(rf"<{re.escape(name)}>([^<]*)</{re.escape(name)}>", pom)
    if match is None:
        raise ValueError(f"property '{name}' not found in the POM")
    return str(match.group(1))


def set_pom_property(pom: str, name: str, value: str) -> str:
    """Return the POM content `pom` with the property `name` set to `value`."""
    pattern = rf"<{re.escape(name)}>[^<]*</{re.escape(name)}>"
    updated, count = re.subn(pattern, f"<{name}>{value}</{name}>", pom)
    if count != 1:
        raise ValueError(f"expected one property '{name}' in the POM, found {count}")
    return updated


def test_class_path(java_source: str, file_name: str) -> str:
    """Return the class path of the top-level class of a Java source file.

    The package is read from the `package` declaration, not from the directory:
    JSBML has test sources in a directory that does not match their package
    (`core/test/org/sbml/jsbml/xml/prasers/` holds package `...xml.parsers`).

    Args:
        java_source: content of the Java source file.
        file_name: name of the file, e.g. "FooTest.java".

    Returns:
        The path of the class in a jar without ".class", e.g.
        "org/sbml/jsbml/test/FooTest".
    """
    match = PACKAGE_RE.search(java_source)
    package = str(match.group(1)).replace(".", "/") + "/" if match else ""
    return package + file_name.removesuffix(".java")


def is_test_data(resource: str) -> bool:
    """Decide whether a resource of the test sources is test data.

    Only resources in a `test` or `testdata` package count as test data. The
    other resources of the test sources stay in the jar, since the production
    classes load some of them (`ASTFactory.parseMathML` reads
    `org/sbml/jsbml/math/compiler/resources/` from `core/test`).
    """
    return bool({"test", "testdata"} & set(resource.split("/")[:-1]))


def is_test_entry(
    entry: str, test_classes: frozenset[str], test_data: frozenset[str]
) -> bool:
    """Decide whether a jar entry is built from JSBML's test sources.

    Args:
        entry: path of the jar entry, e.g. "org/sbml/jsbml/test/Foo$1.class".
        test_classes: class paths from `test_class_path` of the test sources.
        test_data: test data paths, relative to the module's test/ directory.

    Returns:
        True for a class compiled from a test source (including its nested and
        anonymous classes), and for test data.
    """
    if entry.endswith(".class"):
        return entry.removesuffix(".class").split("$", 1)[0] in test_classes
    return entry in test_data


def jar_entries_to_keep(
    entries: list[str], test_classes: frozenset[str], test_data: frozenset[str]
) -> list[str]:
    """Return the jar entries without test classes, test data and dropped entries."""
    return [
        entry
        for entry in entries
        if entry not in DROPPED_ENTRIES
        and not is_test_entry(entry, test_classes, test_data)
    ]


def minimal_pom(artifact_id: str, version: str, commit: str, repository: str) -> str:
    """Return a POM without dependencies for a jar in `lib/cy3sbml-dep`.

    The JSBML dependencies (woodstox, staxmate, xstream, ...) are declared in
    cy3sbml's own pom.xml, so the POM declares none.
    """
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>{GROUP_ID}</groupId>
  <artifactId>{artifact_id}</artifactId>
  <version>{version}</version>
  <!-- built from {repository}/commit/{commit}
    by scripts/update_jsbml.py, without the test classes -->
</project>
"""


def run(args: list[str], cwd: Path | None = None) -> str:
    """Run a command, fail on a non-zero exit code, and return its stdout."""
    result = subprocess.run(args, cwd=cwd, check=True, capture_output=True, text=True)
    return result.stdout.strip()


def check_jdk() -> None:
    """Fail unless `javac` (from JAVA_HOME, else the PATH) is a JDK 17 to 19."""
    java_home = os.environ.get("JAVA_HOME")
    javac = str(Path(java_home) / "bin" / "javac") if java_home else "javac"
    try:
        output = subprocess.run(
            [javac, "-version"], check=True, capture_output=True, text=True
        )
    except (OSError, subprocess.CalledProcessError) as error:
        sys.exit(f"JDK {JDK_MIN} needed, '{javac} -version' failed: {error}")
    version = (output.stdout + output.stderr).strip()
    match = re.search(r"javac (\d+)", version)
    if match is None or not JDK_MIN <= int(match.group(1)) <= JDK_MAX:
        sys.exit(
            f"JDK {JDK_MIN} to {JDK_MAX} needed to build JSBML (Java 7 bytecode), "
            f"found '{version}'. Set JAVA_HOME to a JDK {JDK_MIN}."
        )


def ant_executable() -> Path:
    """Return the Ant launcher, downloading and verifying Ant on first use.

    Ant is cached in `$XDG_CACHE_HOME/cy3sbml` (default `~/.cache/cy3sbml`).
    """
    cache_dir = Path(os.environ.get("XDG_CACHE_HOME", Path.home() / ".cache"))
    ant_home = cache_dir / "cy3sbml" / f"apache-ant-{ANT_VERSION}"
    launcher = ant_home / "bin" / ("ant.bat" if os.name == "nt" else "ant")
    if launcher.exists():
        return launcher

    print(f"Downloading Apache Ant {ANT_VERSION}")
    with urllib.request.urlopen(ANT_URL, timeout=120) as response:
        archive = response.read()
    digest = hashlib.sha512(archive).hexdigest()
    if digest != ANT_SHA512:
        sys.exit(f"SHA-512 mismatch for {ANT_URL}: {digest}")
    ant_home.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=ant_home.parent) as extract_dir:
        archive_path = Path(extract_dir) / "ant.tar.gz"
        archive_path.write_bytes(archive)
        with tarfile.open(archive_path) as tar:
            tar.extractall(extract_dir, filter="data")
        # move into place last, so that an interrupted download leaves no cache
        (Path(extract_dir) / ant_home.name).rename(ant_home)
    return launcher


def checkout(ref: str, jsbml_dir: Path, repository: str) -> str:
    """Clone `repository` into `jsbml_dir`, check out `ref`, return its commit hash."""
    print(f"Cloning {repository} and checking out '{ref}'")
    run(["git", "clone", "--quiet", "--filter=blob:none", repository, str(jsbml_dir)])
    for candidate in (f"origin/{ref}", ref):
        try:
            commit = run(
                ["git", "rev-parse", "--verify", "--quiet", f"{candidate}^{{commit}}"],
                cwd=jsbml_dir,
            )
        except subprocess.CalledProcessError:
            continue
        run(["git", "checkout", "--quiet", "--detach", commit], cwd=jsbml_dir)
        return commit
    sys.exit(f"'{ref}' is no branch, tag or commit of {repository}")


def build(jsbml_dir: Path, version: str) -> None:
    """Build the JSBML core and package jars with Ant, all with `version`."""
    ant = ant_executable()
    print(f"Building JSBML {version} with Ant {ANT_VERSION}")
    # the command line property overrides the version of every module
    # (1.7-SNAPSHOT, 2.1-b1, ...), so every jar is named after and carries
    # (Bundle-Version) the pinned version. The svn "Execute failed" messages in
    # the output are harmless: the build probes svn, then falls back to git.
    result = subprocess.run(
        [str(ant), f"-Dversion={version}", "jar"],
        cwd=jsbml_dir,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        print(result.stdout[-5000:], result.stderr[-5000:], sep="\n")
        sys.exit("The JSBML Ant build failed.")


def strip_tests(source: Path, target: Path, test_dir: Path) -> int:
    """Copy the jar `source` to `target` without test classes and test data.

    Returns:
        The number of removed entries.
    """
    files = [path for path in test_dir.rglob("*") if path.is_file()]
    test_classes = frozenset(
        test_class_path(path.read_text(encoding="utf-8", errors="replace"), path.name)
        for path in files
        if path.suffix == ".java"
    )
    test_data = frozenset(
        resource
        for resource in (path.relative_to(test_dir).as_posix() for path in files)
        if is_test_data(resource)
    )
    with zipfile.ZipFile(source) as jar_in:
        entries = jar_in.namelist()
        kept = jar_entries_to_keep(entries, test_classes, test_data)
        with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as jar_out:
            for entry in kept:
                jar_out.writestr(jar_in.getinfo(entry), jar_in.read(entry))
    return len(entries) - len(kept)


def write_sha1(path: Path) -> None:
    """Write the Maven SHA-1 checksum file of `path` (the bare hash)."""
    digest = hashlib.sha1(path.read_bytes()).hexdigest()
    path.with_name(f"{path.name}.sha1").write_text(digest)


def install(
    module: Module, jsbml_dir: Path, version: str, commit: str, repository: str
) -> tuple[int, int]:
    """Write the stripped jar of `module` with its POM into `lib/cy3sbml-dep`.

    Everything else in the artifact directory, i.e. the previous version, is
    removed: the new jars replace the old ones. A copy of the version in the
    local Maven repository is removed, too.

    Returns:
        The number of entries in the written jar, and the number removed as tests.
    """
    artifact_dir = DEP_DIR / module.artifact_id
    if artifact_dir.exists():
        shutil.rmtree(artifact_dir)
    version_dir = artifact_dir / version
    version_dir.mkdir(parents=True)
    shutil.rmtree(M2_GROUP_DIR / module.artifact_id / version, ignore_errors=True)

    name = f"{module.artifact_id}-{version}"
    built_jar = jsbml_dir / module.build_dir / f"{module.artifact_id}-{version}.jar"
    jar = version_dir / f"{name}.jar"
    removed = strip_tests(built_jar, jar, jsbml_dir / module.source_dir / "test")
    pom = version_dir / f"{name}.pom"
    pom.write_text(minimal_pom(module.artifact_id, version, commit, repository))
    write_sha1(jar)
    write_sha1(pom)
    with zipfile.ZipFile(jar) as written:
        return len(written.namelist()), removed


def summary(
    old_version: str, new_version: str, commit: str, jsbml_dir: Path, repository: str
) -> str:
    """Return a Markdown summary of the update with the JSBML commits it adds."""
    old_sha = version_sha(old_version)
    log = run(
        ["git", "log", "--first-parent", "--format=%h %s", f"{old_sha}..{commit}"],
        cwd=jsbml_dir,
    )
    commits = "\n".join(
        f"- [`{line[:9]}`]({repository}/commit/{line.split(' ', 1)[0]}) "
        f"{line.split(' ', 1)[1]}"
        for line in log.splitlines()
    )
    return (
        f"Update JSBML from `{old_version}` to `{new_version}` "
        f"({repository}/compare/{old_sha}...{commit[:8]}).\n\n"
        f"JSBML commits (first parent):\n\n{commits or '- none'}\n"
    )


def main() -> None:
    """Update `lib/cy3sbml-dep` and `pom.xml` to a JSBML commit."""
    parser = argparse.ArgumentParser(
        description="Update the JSBML jars in lib/cy3sbml-dep to a JSBML commit."
    )
    parser.add_argument(
        "ref",
        nargs="?",
        default=JSBML_DEFAULT_REF,
        help=f"JSBML branch, tag or commit (default: {JSBML_DEFAULT_REF})",
    )
    parser.add_argument(
        "--repository",
        default=JSBML_URL,
        help=f"JSBML repository URL or path (default: {JSBML_URL})",
    )
    parser.add_argument(
        "--force", action="store_true", help="rebuild the pinned commit, too"
    )
    parser.add_argument(
        "--summary", type=Path, help="write a Markdown summary of the update"
    )
    args = parser.parse_args()

    check_jdk()
    pom = POM_PATH.read_text()
    old_version = pom_property(pom, "jsbml.version")

    with tempfile.TemporaryDirectory(prefix="jsbml-") as tmp:
        jsbml_dir = Path(tmp) / "jsbml"
        commit = checkout(args.ref, jsbml_dir, args.repository)
        commit_date = run(
            ["git", "show", "-s", "--format=%cd", "--date=format:%Y%m%d", commit],
            cwd=jsbml_dir,
        )
        base_version = pom_property(
            (jsbml_dir / "pom.xml").read_text().split("</parent>")[-1], "version"
        ).removesuffix("-SNAPSHOT")
        version = jsbml_version(base_version, commit_date, commit)
        print(f"JSBML {args.ref} is commit {commit}, version {version}")

        if version == old_version and not args.force:
            print(f"JSBML is already at {version}, nothing to do.")
            return

        build(jsbml_dir, version)
        for module in MODULES:
            entries, removed = install(
                module, jsbml_dir, version, commit, args.repository
            )
            print(f"  {module.artifact_id}: {entries} entries, {removed} test removed")

        pom = set_pom_property(pom, "jsbml.version", version)
        pom = set_pom_property(pom, "jsbml.osgi.version", osgi_version(version))
        POM_PATH.write_text(pom)

        text = summary(old_version, version, commit, jsbml_dir, args.repository)
        if args.summary:
            args.summary.write_text(text)

    print(f"\n{text}")
    print(
        f"Updated lib/cy3sbml-dep and pom.xml from {old_version} to {version}.\n"
        "Next: ./mvnw -B -q clean verify, and review the GoldenModelsTest result."
    )


def check() -> None:
    """Run a self-check of the helper functions, without touching the disk."""
    version = jsbml_version("1.7", "20260907", "8192a8a7010676202eb6")
    assert version == "1.7-20260907-8192a8a7", version
    assert osgi_version(version) == "1.7.0.20260907-8192a8a7"
    assert version_sha(version) == "8192a8a7"
    try:
        osgi_version("1.7-SNAPSHOT")
    except ValueError:
        pass
    else:
        raise AssertionError("expected a ValueError for a SNAPSHOT version")

    pom = "<p><a.b>1</a.b><a.bc>2</a.bc></p>"
    assert pom_property(pom, "a.b") == "1"
    assert set_pom_property(pom, "a.b", "3") == "<p><a.b>3</a.b><a.bc>2</a.bc></p>"

    source = "/* header */\npackage org.x.test;\n\npublic class FooTest {}\n"
    assert test_class_path(source, "FooTest.java") == "org/x/test/FooTest"
    assert test_class_path("class Bar {}", "Bar.java") == "Bar"
    assert is_test_data("org/x/testdata/model.xml")
    assert not is_test_data("org/x/resources/abs.xml")

    classes = frozenset({"org/x/test/FooTest", "org/x/BarTest"})
    data = frozenset({"org/x/testdata/model.xml"})
    entries = [
        "META-INF/MANIFEST.MF",
        "META-INF/INDEX.LIST",
        "org/x/Foo.class",
        "org/x/Foo$1.class",
        "org/x/BarTest.class",
        "org/x/test/FooTest.class",
        "org/x/test/FooTest$Inner.class",
        "org/x/testdata/model.xml",
        "org/x/resources/abs.xml",
    ]
    assert jar_entries_to_keep(entries, classes, data) == [
        "META-INF/MANIFEST.MF",
        "org/x/Foo.class",
        "org/x/Foo$1.class",
        "org/x/resources/abs.xml",
    ]
    print("self-check passed")


if __name__ == "__main__":
    if "--check" in sys.argv[1:]:
        check()
    else:
        main()

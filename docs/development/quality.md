# Code quality

Four checks keep the code consistent. The CI runs them on every pull request, and they
are required for merging into `develop`.

| Check | Tool | Command |
|---|---|---|
| `format` | Spotless with palantir-java-format | `./mvnw spotless:check` |
| `lint` | Error Prone and `javac` warnings | `./mvnw -Plint -DskipTests test-compile` (JDK 21) |
| `tests` | JUnit tests and the packaged jar test | `./mvnw verify` |
| `python` | ruff and ty on the Python helpers | see [Python](#python) |

## Formatting

The Java code is formatted with [Spotless](https://github.com/diffplug/spotless) and
[palantir-java-format](https://github.com/palantir/palantir-java-format). Spotless also
removes unused imports, orders the imports, trims trailing whitespace, and sorts
`pom.xml`.

```bash
./mvnw -B -q spotless:apply    # format the code
./mvnw -B -q spotless:check    # check the formatting, as the CI does
```

IDE formatters produce different results, so run `spotless:apply` before every commit.
A git pre-commit hook does this for you. It formats the staged Java files and stages the
result. It stops if a staged file also has unstaged changes, because staging the formatted
file would stage those changes too. Save it as `.git/hooks/pre-commit` and make it
executable with `chmod +x .git/hooks/pre-commit`:

```bash
#!/bin/sh
# format the staged Java files with spotless and stage the result
FILES=$(git diff --cached --name-only --diff-filter=ACM -- '*.java')
[ -z "$FILES" ] && exit 0
# re-staging a file that also has unstaged changes would stage those too
PARTIAL=$(git diff --name-only -- $FILES)
if [ -n "$PARTIAL" ]; then
    echo "pre-commit: stage or stash the unstaged changes first:" >&2
    echo "$PARTIAL" >&2
    exit 1
fi
PATTERNS=$(printf '.*%s,' $FILES)
./mvnw -q spotless:apply -DspotlessFiles="${PATTERNS%,}" || exit 1
git add -- $FILES
```

## Error Prone

The Maven profile `lint` compiles the code with [Error Prone](https://errorprone.info),
with all `javac` lint warnings except `processing` and `serial`, and fails on every
warning (`-Werror`). Error Prone needs JDK 21 or newer to run. The code still compiles
for Java 17. The JVM options that Error Prone needs are in `.mvn/jvm.config`.

```bash
./mvnw -B -q -Plint -DskipTests test-compile
```

Fix the warning instead of suppressing it. If a suppression is needed, use
`@SuppressWarnings` on the smallest possible scope, with a comment that gives the reason.

## Python

The Python helpers, the documentation scripts in `scripts/` and the test model tools in
`tools/pycysbml`, are linted and formatted with [ruff](https://docs.astral.sh/ruff/) and
type checked with [ty](https://docs.astral.sh/ty/), configured in `ruff.toml` and
`ty.toml`. Both tools, and the dependencies of the helpers, come from the uv project in
`tools/`, so run them through it from the repository root:

```bash
uv run --project tools ruff check           # lint
uv run --project tools ruff format          # format the code
uv run --project tools ruff format --check  # check the formatting, as the CI does
uv run --project tools ty check             # type check
```

ty reports every diagnostic as an error, including a value of an untyped library or of
`Any` that flows into an annotated variable or return. Narrow such a value with a check
at the boundary, instead of suppressing the diagnostic.

## Tests

Every change needs tests that cover it. Tests that need the network carry
`@Tag("network")`, long running model suites carry `@Tag("models")`; both are excluded
by default. A change of the import changes the golden snapshots; regenerate them with

```bash
./mvnw -B -q test -Dtest=GoldenModelsTest -Dgolden.update=true
```

and review the diff. See [Testing](testing.md).

## Conventions

- Use the constants in `org.cy3sbml.SBML` for node types, edge types and column names,
  never string literals.
- Access the SBML document of a network only through `SBMLManager`.
- Create new objects in `CyActivator` and pass them to the classes that need them. The
  code has no static singletons.

# Shell tests (`tools/cli/shell-tests`)

End-to-end tests that run the installed `jmltk` CLI launcher and compare stdout against a fixture.

## Prerequisites
Tests invoke `tools/cli/build/install/jmltk/bin/jmltk`, so install the distribution first:
```bash
./gradlew :tools:cli:installDist
```

## Commands
| Task | Command |
|------|---------|
| All tests | `scripts/run-shelltests.sh` (runs `installDist`, then the tester) |
| One file | `python3 scripts/shelltester.py tools/cli/shell-tests/stat/help.txt` |
| One subdir | `python3 scripts/shelltester.py tools/cli/shell-tests/stat` |
| Everything | `python3 scripts/shelltester.py tools/cli/shell-tests` |

Pass the **directory**, not a `**` glob: Windows (pwsh) does not expand globs, and `scripts/run-shelltests.sh` is POSIX-only.

## File format
One `.txt` file per case with YAML-like frontmatter, then the exact expected stdout:
```text
---
run: tools/cli/build/install/jmltk/bin/jmltk stat --help
ignoreStderr: true
---
<expected output>
```
- `run:` is executed through the shell (`/bin/sh` on POSIX, `cmd.exe` on Windows) — keep commands portable.
- `ignoreStderr: true` compares stdout only; otherwise stderr is appended to the compared output.
- Comparison is exact (after trimming). A trailing newline or spacing change fails the test.

## Conventions
- Directory = `jmltk` subcommand (`stat`, `jmlstub`, …); file = scenario (`help.txt`, …).
- Tests are discovered recursively by `scripts/shelltester.py`; CI runs them from `.github/workflows/gradle_tests.yml`.
- On failure the runner writes `<file>.txt.actual` beside the test. Inspect it, then either fix the CLI or update the fixture — never commit `.actual` files.
- Update fixtures only for intentional output changes (e.g. new help lines).


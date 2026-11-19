# jml-tools
Tools for the processing of JML based on the jml-parser

## Parse benchmark

`tools/bench` measures `JavaParser` throughput and separates grammar cost from validation cost.

```bash
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --rounds 5"
./gradlew :tools:bench:run --args="--corpus jml --mode full --rounds 5 --json /tmp/jml-full.json"
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --baseline tools/bench/baseline/plain-grammar.json --verify"
```

Corpora: `plain`, `jml`, or `path:<directory>`. Modes: `grammar` (processors/validators disabled) and `full` (default processors at language level `JAVA_21`).
`--verify` exits nonzero when files, bytes, ok/fail, the failing-file set, or any per-file AST fingerprint differs from the baseline; `--json` is written only after a successful verify and must not name the same file as `--baseline`.

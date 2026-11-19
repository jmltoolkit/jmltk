# jml-tools
Tools for the processing of JML based on the jml-parser

## Parse benchmark

`tools/bench` measures `JavaParser` throughput and separates grammar cost from validation cost.

```bash
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --rounds 5"
./gradlew :tools:bench:run --args="--corpus jml --mode full --rounds 5 --json /tmp/jml-full.json"
./gradlew :tools:bench:run --args="--corpus plain --mode grammar --baseline tools/bench/baseline/plain-grammar.json --verify"
```

Corpora: `plain`, `jml`, or `path:<directory>`. Modes: `grammar` (validators disabled) and `full`.
`--verify` exits nonzero when the ok/fail file set differs from the baseline.

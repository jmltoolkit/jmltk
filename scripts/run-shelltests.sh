#!/usr/bin/env bash

gradle --continue --parallel :tools:cli:installDist
python3 scripts/shelltester.py tools/cli/shell-tests/**
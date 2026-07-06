#!/usr/bin/env sh

gradle --parallel :jmlparser-core-metamodel-generator:run
gradle --parallel :jmlparser-core-generators:run
gradle --parallel spotlessApply
git diff --exit-code
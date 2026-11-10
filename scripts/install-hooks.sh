#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
git config --local core.hooksPath .githooks
echo 'Installed local hooks. Run ./mvnw spotless:apply, review, then stage.'

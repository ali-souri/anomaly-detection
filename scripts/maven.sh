#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) exec ./mvnw.cmd "$@" ;;
  *) exec ./mvnw "$@" ;;
esac

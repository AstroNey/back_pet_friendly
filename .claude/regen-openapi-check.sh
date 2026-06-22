#!/usr/bin/env bash
# Stop hook : signale si openapi.json est out-of-date par rapport au web layer.
# N'execute pas la regen (lente, ~1 min) — juste un rappel pour que Claude la lance.
# Regen manuelle : ./mvnw verify -Pgenerate-openapi -DskipTests

set -e
cd "$(dirname "$0")/.."

if [ ! -f openapi.json ]; then
  echo "[openapi-check] openapi.json missing. Run: ./mvnw verify -Pgenerate-openapi -DskipTests" >&2
  exit 0
fi

newer=$(find src/main/java/lns/back/backend_pet_friendly/web -name "*.java" -newer openapi.json 2>/dev/null | head -5)
if [ -n "$newer" ]; then
  echo "[openapi-check] openapi.json stale — these web files are newer:" >&2
  echo "$newer" | sed 's/^/  - /' >&2
  echo "  Regen: ./mvnw verify -Pgenerate-openapi -DskipTests" >&2
fi
exit 0

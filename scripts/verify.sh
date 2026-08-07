#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
source "$ROOT/scripts/check-env.sh"
if ! cmp -s "$ROOT/sql/schema.sql" "$ROOT/backend/src/main/resources/schema.sql"; then
  echo 'ERROR: sql/schema.sql 与 backend/src/main/resources/schema.sql 不一致'
  diff -u "$ROOT/backend/src/main/resources/schema.sql" "$ROOT/sql/schema.sql" || true
  exit 1
fi
(cd "$ROOT/frontend" && npm run check && npm run build)
(cd "$ROOT/backend" && mvn test)
echo '全部静态检查、前端测试/构建和后端测试已通过。'

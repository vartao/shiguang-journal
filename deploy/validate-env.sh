#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
ENV_FILE="${ENV_FILE:-$ROOT/.env}"
[[ -f "$ENV_FILE" ]] || { echo "ERROR: 缺少 $ENV_FILE"; exit 1; }
set -a; source "$ENV_FILE"; set +a
fail=0
for name in MYSQL_ROOT_PASSWORD JWT_SECRET CORS_ORIGINS; do
  value="${!name:-}"
  if [[ -z "$value" || "$value" == *CHANGE_ME* ]]; then echo "ERROR: $name 未安全配置"; fail=1; fi
done
[[ "${JWT_SECRET:-}" == ????????????????????????????????* ]] || { echo 'ERROR: JWT_SECRET 至少需要 32 个字符'; fail=1; }
[[ "${CORS_ORIGINS:-}" == *localhost* ]] && { echo 'ERROR: 生产 CORS_ORIGINS 不应包含 localhost'; fail=1; }
(( fail == 0 )) || exit 1
echo '生产环境变量检查通过。'

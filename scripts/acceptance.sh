#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080/api}"
PASSED=0

usage() {
  cat <<USAGE
Usage: bash scripts/acceptance.sh [--base-url URL]

Environment:
  BASE_URL  API base URL (default: http://127.0.0.1:8080/api)
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      [[ $# -ge 2 ]] || { echo 'ERROR: --base-url requires a value' >&2; exit 2; }
      BASE_URL=$2
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

for command_name in curl node; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "ERROR: required command not found: $command_name" >&2
    exit 1
  }
done

pass() {
  PASSED=$((PASSED + 1))
  printf '[PASS] %s\n' "$1"
}

fail() {
  printf '[FAIL] %s\n' "$1" >&2
  if [[ -n "${2:-}" ]]; then
    printf 'Response: %s\n' "$2" >&2
  fi
  exit 1
}

api() {
  local method=$1
  local path=$2
  local token=${3:-}
  local body=${4:-}
  local -a args=(--silent --show-error --connect-timeout 10 --max-time 30 -X "$method" -H 'Content-Type: application/json')
  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    args+=(--data "$body")
  fi
  curl "${args[@]}" "$BASE_URL$path"
}

json_get() {
  local path=$1
  node -e '
    const path = process.argv[1].split(".").filter(Boolean);
    let input = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", chunk => input += chunk);
    process.stdin.on("end", () => {
      let value = JSON.parse(input);
      for (const key of path) value = value == null ? undefined : value[key];
      if (value == null) return;
      process.stdout.write(typeof value === "object" ? JSON.stringify(value) : String(value));
    });
  ' "$path"
}

biz_ok() {
  local response=$1
  local name=$2
  local code
  code=$(printf '%s' "$response" | json_get code)
  if [[ "$code" != 200 && "$code" != 0 ]]; then
    fail "$name returned business code ${code:-missing}" "$response"
  fi
}

require_value() {
  local value=$1
  local name=$2
  local response=${3:-}
  [[ -n "$value" ]] || fail "$name is missing" "$response"
}

suffix="$(date +%H%M%S)$((RANDOM % 10000))"
user_a="acc_a_${suffix}"
user_b="acc_b_${suffix}"
password='Test1234'

echo "Qinglv API acceptance: $BASE_URL"

health=$(curl --silent --show-error --fail --connect-timeout 10 --max-time 30 "${BASE_URL%/api}/actuator/health")
health_status=$(printf '%s' "$health" | json_get status)
[[ "$health_status" == UP ]] || fail 'actuator health is not UP' "$health"
pass 'actuator health'

reg_a=$(api POST /auth/register '' "{\"username\":\"$user_a\",\"password\":\"$password\",\"nickname\":\"Acceptance A\",\"togetherDate\":\"2024-05-20\",\"role\":\"boy\"}")
biz_ok "$reg_a" 'register A'
token_a=$(printf '%s' "$reg_a" | json_get data.token)
couple_a=$(printf '%s' "$reg_a" | json_get data.coupleId)
require_value "$token_a" 'register A token' "$reg_a"
require_value "$couple_a" 'register A coupleId' "$reg_a"
pass 'register A'

couple=$(api GET /couple/info "$token_a")
biz_ok "$couple" 'couple info'
invite=$(printf '%s' "$couple" | json_get data.inviteCode)
require_value "$invite" 'invite code' "$couple"
pass 'couple invite code'

reg_b=$(api POST /auth/register '' "{\"username\":\"$user_b\",\"password\":\"$password\",\"nickname\":\"Acceptance B\",\"inviteCode\":\"$invite\",\"role\":\"girl\"}")
biz_ok "$reg_b" 'register B'
token_b=$(printf '%s' "$reg_b" | json_get data.token)
couple_b=$(printf '%s' "$reg_b" | json_get data.coupleId)
require_value "$token_b" 'register B token' "$reg_b"
[[ "$couple_b" == "$couple_a" ]] || fail "couple binding mismatch: A=$couple_a B=$couple_b" "$reg_b"
pass 'register B and bind couple'

login_a=$(api POST /auth/login '' "{\"username\":\"$user_a\",\"password\":\"$password\"}")
biz_ok "$login_a" 'login A'
token_a=$(printf '%s' "$login_a" | json_get data.token)
require_value "$token_a" 'login token' "$login_a"
pass 'login A'

note=$(api POST /notes "$token_a" "{\"title\":\"Acceptance note $suffix\",\"content\":\"Docker end-to-end acceptance.\",\"mood\":\"happy\",\"weather\":\"sunny\",\"isTop\":0}")
biz_ok "$note" 'create note'
note_id=$(printf '%s' "$note" | json_get data.id)
require_value "$note_id" 'created note id' "$note"
pass 'create note'

timeline=$(api GET '/notes?page=1&size=10' "$token_b")
biz_ok "$timeline" 'timeline'
if ! printf '%s' "$timeline" | node -e '
  const wanted = String(process.argv[1]);
  let input = "";
  process.stdin.setEncoding("utf8");
  process.stdin.on("data", chunk => input += chunk);
  process.stdin.on("end", () => {
    const root = JSON.parse(input);
    const data = root.data;
    const list = Array.isArray(data) ? data : (data?.records || data?.list || []);
    process.exit(list.some(item => String(item?.id) === wanted) ? 0 : 1);
  });
' "$note_id"; then
  fail "timeline does not contain note $note_id" "$timeline"
fi
pass 'timeline contains created note'

like_on=$(api POST "/notes/$note_id/like" "$token_b")
biz_ok "$like_on" 'like note'
[[ "$(printf '%s' "$like_on" | json_get data.liked)" == true ]] || fail 'like did not toggle on' "$like_on"
pass 'like toggle on'

like_off=$(api POST "/notes/$note_id/like" "$token_b")
biz_ok "$like_off" 'unlike note'
[[ "$(printf '%s' "$like_off" | json_get data.liked)" == false ]] || fail 'like did not toggle off' "$like_off"
pass 'like toggle off'
api POST "/notes/$note_id/like" "$token_b" >/dev/null

surprise=$(api POST /surprises "$token_a" "{\"title\":\"Acceptance surprise $suffix\",\"content\":\"Acceptance secret\",\"hint\":\"Click to unlock\",\"unlockCondition\":\"click\"}")
biz_ok "$surprise" 'create surprise'
surprise_id=$(printf '%s' "$surprise" | json_get data.id)
require_value "$surprise_id" 'created surprise id' "$surprise"
pass 'create surprise'

locked=$(api GET "/surprises/$surprise_id" "$token_b")
biz_ok "$locked" 'locked surprise detail'
locked_content=$(printf '%s' "$locked" | json_get data.content)
[[ -z "$locked_content" ]] || fail 'locked surprise leaked content' "$locked"
pass 'locked surprise hides content'

unlocked=$(api POST "/surprises/$surprise_id/unlock" "$token_b" '{}')
biz_ok "$unlocked" 'unlock surprise'
[[ "$(printf '%s' "$unlocked" | json_get data.content)" == 'Acceptance secret' ]] || fail 'unlock did not reveal content' "$unlocked"
pass 'unlock surprise reveals content'

stats=$(api GET /stats "$token_a")
biz_ok "$stats" 'stats'
note_count=$(printf '%s' "$stats" | json_get data.noteCount)
happy_count=$(printf '%s' "$stats" | json_get data.moodDistribution.happy)
[[ "$note_count" =~ ^[0-9]+$ && "$note_count" -ge 1 ]] || fail 'stats noteCount is below 1' "$stats"
[[ "$happy_count" =~ ^[0-9]+$ && "$happy_count" -ge 1 ]] || fail 'stats happy mood count is below 1' "$stats"
pass 'stats include note and mood data'

delete_attempt=$(api DELETE "/notes/$note_id" "$token_b")
delete_code=$(printf '%s' "$delete_attempt" | json_get code)
[[ "$delete_code" == 403 ]] || fail "non-author delete returned code ${delete_code:-missing}" "$delete_attempt"
pass 'author-only note deletion'

printf 'Acceptance complete: %s checks passed.\n' "$PASSED"

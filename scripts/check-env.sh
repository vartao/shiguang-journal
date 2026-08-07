#!/usr/bin/env bash
set -euo pipefail
command -v node >/dev/null || { echo 'ERROR: Node.js 未安装'; exit 1; }
command -v npm >/dev/null || { echo 'ERROR: npm 未安装'; exit 1; }
command -v mvn >/dev/null || { echo 'ERROR: Maven 未安装'; exit 1; }
current_java=''
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then current_java=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n1 || true); fi
if [[ "$current_java" != *'17.'* ]]; then
  for candidate in /d/development-tools/environment/jdk-17.0.9 '/c/Program Files/Java/jdk-17'; do
    if [[ -x "$candidate/bin/java" ]]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
[[ -x "${JAVA_HOME:-}/bin/java" ]] || { echo 'ERROR: 找不到 JDK 17，请设置 JAVA_HOME'; exit 1; }
export PATH="$JAVA_HOME/bin:$PATH"
version=$(java -version 2>&1 | head -n1)
[[ "$version" == *'17.'* ]] || { echo "ERROR: 需要 JDK 17，当前为 $version"; exit 1; }
echo "Java: $version"
echo "Maven: $(mvn -version | head -n1)"
echo "Node: $(node --version)"
echo "npm: $(npm --version)"

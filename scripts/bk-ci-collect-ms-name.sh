#!/bin/bash
# 收集ci微服务的名字.
set -eu

# 基于脚本位置定位.
my_path="$(readlink -f "$0")"
BK_CI_SRC_DIR="${my_path%/*/*}"

cd "$BK_CI_SRC_DIR"
shopt -s nullglob
# agentless, dockerhost, assembly特殊处理. 不计入.
possible_ci_ms_dirs=(*/boot-*.jar */META-INF/MANIFEST.MF)
shopt -u nullglob
if [ ${#possible_ci_ms_dirs[@]} -eq 0 ]; then
  echo >&2 "invalid install package dirs."
else
  printf "%s\n" "${possible_ci_ms_dirs[@]}" | sed 's@/[^ ]*@@g' |
  grep -xv -e dockerhost -e agentless -e assembly | sort -u
fi

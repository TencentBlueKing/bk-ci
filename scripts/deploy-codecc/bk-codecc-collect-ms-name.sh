#!/bin/bash
# 收集codecc微服务的名字.
set -eu

# 基于脚本位置定位.
my_path="$(readlink -f "$0")"
BK_CODECC_SRC_DIR="${my_path%/*/*/*}"

cd "$BK_CODECC_SRC_DIR"
shopt -s nullglob
# assembly特殊处理. 不计入.
possible_codecc_ms_dirs=(*/boot-*.jar */META-INF/MANIFEST.MF)
shopt -u nullglob
if [ ${#possible_codecc_ms_dirs[@]} -eq 0 ]; then
  echo >&2 "invalid install package dirs."
else
  printf "%s\n" "${possible_codecc_ms_dirs[@]}" | sed 's@/[^ ]*@@g' |
  grep -xv -e assembly | sort -u
fi

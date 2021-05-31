#!/bin/bash
# 收集ci微服务的名字.
set -eu

# 基于脚本位置定位.
my_path="$(readlink -f "$0")"
BK_CI_SRC_DIR="${my_path%/*/*}"

cd "$BK_CI_SRC_DIR"
shopt -s nullglob
# agentless, dockerhost, assembly特殊处理. 不计入.
ci_ms_names=$(ls */boot-*.jar */META-INF/MANIFEST.MF | sed 's@/.*@@' |
  grep -xv -e dockerhost -e agentless -e assembly | sort -u)
shopt -u nullglob

echo "$ci_ms_names"

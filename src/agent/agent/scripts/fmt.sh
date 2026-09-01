#!/usr/bin/env bash
# fmt.sh — Linux/macOS 平台代码格式化
# 根据当前系统排除不适用的平台文件，避免 goimports-reviser 处理跨平台文件报错。

set -euo pipefail

UNAME_S=$(uname -s)

case "$UNAME_S" in
  Linux)
    EXCLUDE='! -name *_win.go ! -name *_win_test.go ! -name *_darwin.go ! -name *_darwin_test.go ! -name *_darwin_cgo.go'
    ;;
  Darwin)
    EXCLUDE='! -name *_win.go ! -name *_win_test.go ! -name *_linux.go ! -name *_linux_test.go'
    ;;
  *)
    EXCLUDE='! -name *_unix.go ! -name *_unix_test.go ! -name *_darwin.go ! -name *_darwin_test.go ! -name *_darwin_cgo.go ! -name *_linux.go ! -name *_linux_test.go'
    ;;
esac

# goimports 处理全部文件（它本身支持跨平台）
goimports -w .

# gofmt 和 goimports-reviser 只处理当前平台适用的文件
# shellcheck disable=SC2086
find ./ -name "*.go" $EXCLUDE | xargs gofmt -w -l

# shellcheck disable=SC2086
find ./ -name "*.go" $EXCLUDE | xargs goimports-reviser -rm-unused -set-alias -format

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="${SCRIPT_DIR}/dist"
VERSION="${1:-}"

mkdir -p "${DIST_DIR}"

build_target() {
  local goos="$1"
  local goarch="$2"
  local output_name="$3"
  echo "==> building ${output_name}"
  CGO_ENABLED=0 GOOS="${goos}" GOARCH="${goarch}" go build -trimpath -ldflags="-s -w" -o "${DIST_DIR}/${output_name}" .
}

cd "${SCRIPT_DIR}"

build_target linux amd64 agent-util_linux_amd64
build_target windows amd64 agent-util_windows_amd64.exe

if [[ -n "${VERSION}" ]]; then
  echo "version tag: ${VERSION}"
fi

echo
echo "build completed"
echo "output directory: ${DIST_DIR}"
ls -lh "${DIST_DIR}"

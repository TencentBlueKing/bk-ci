#!/bin/bash
#
# Package agent binaries + scripts into per-platform zip archives.
#
# Usage:
#   ./package_agent.sh <upgrade_dir> <script_dir> <output_dir>
#
# Example:
#   ./package_agent.sh ./upgrade ./script ./dist
#
# Produces:
#   dist/agent_linux_amd64.zip
#   dist/agent_linux_arm64.zip
#   dist/agent_linux_mips64.zip
#   dist/agent_linux_loong64.zip
#   dist/agent_macos_amd64.zip
#   dist/agent_macos_arm64.zip
#   dist/agent_windows_amd64.zip
#   dist/agent_windows_386.zip
#
# Binary naming convention (from Makefile):
#   Linux amd64:   devopsAgent_linux         (no arch suffix = amd64)
#   Linux arm64:   devopsAgent_linux_arm64
#   macOS amd64:   devopsAgent_macos
#   macOS arm64:   devopsAgent_macos_arm64
#   Windows amd64: devopsAgent.exe           (no arch suffix = amd64)
#   Windows 386:   devopsAgent_386.exe
#
# Inside the zip, binaries are renamed to the runtime names:
#   Linux/macOS: devopsAgent, devopsDaemon, tmp/upgrader, installer
#   Windows:     devopsAgent.exe, devopsDaemon.exe, tmp/upgrader.exe, installer.exe

set -euo pipefail

UPGRADE_DIR="${1:?Usage: $0 <upgrade_dir> <script_dir> <output_dir>}"
SCRIPT_DIR="${2:?Usage: $0 <upgrade_dir> <script_dir> <output_dir>}"
OUTPUT_DIR="${3:?Usage: $0 <upgrade_dir> <script_dir> <output_dir>}"

mkdir -p "$OUTPUT_DIR"

BINARIES=(devopsAgent devopsDaemon upgrader)

# package_unix <os> <arch> <binary_suffix> <script_subdir>
#   binary_suffix: e.g. "_linux", "_linux_arm64", "_macos", "_macos_arm64"
package_unix() {
    local os="$1" arch="$2" suffix="$3" script_sub="$4"
    local zip_name="agent_${os}_${arch}.zip"
    local tmp_dir
    tmp_dir=$(mktemp -d)

    echo "=== Packaging ${zip_name} ==="

    for bin in "${BINARIES[@]}"; do
        local src="${UPGRADE_DIR}/${bin}${suffix}"
        if [[ ! -f "$src" ]]; then
            echo "  SKIP: ${bin}${suffix} not found"
            continue
        fi
        if [[ "$bin" == "upgrader" ]]; then
            mkdir -p "${tmp_dir}/tmp"
            cp "$src" "${tmp_dir}/tmp/upgrader"
            chmod +x "${tmp_dir}/tmp/upgrader"
            echo "  ADD: ${bin}${suffix} -> tmp/upgrader"
        else
            cp "$src" "${tmp_dir}/${bin}"
            chmod +x "${tmp_dir}/${bin}"
            echo "  ADD: ${bin}${suffix} -> ${bin}"
        fi
    done

    if [[ -d "${SCRIPT_DIR}/${script_sub}" ]]; then
        for f in "${SCRIPT_DIR}/${script_sub}"/*; do
            [[ -f "$f" ]] || continue
            cp "$f" "${tmp_dir}/$(basename "$f")"
            chmod +x "${tmp_dir}/$(basename "$f")"
            echo "  ADD: script/${script_sub}/$(basename "$f")"
        done
    fi

    (cd "$tmp_dir" && zip -qr "${OUTPUT_DIR}/${zip_name}" .)
    echo "  => ${OUTPUT_DIR}/${zip_name}"
    rm -rf "$tmp_dir"
}

# package_windows <arch> <binary_suffix>
#   binary_suffix: e.g. ".exe" (amd64), "_386.exe" (386)
package_windows() {
    local arch="$1" suffix="$2"
    local zip_name="agent_windows_${arch}.zip"
    local tmp_dir
    tmp_dir=$(mktemp -d)

    echo "=== Packaging ${zip_name} ==="

    for bin in "${BINARIES[@]}"; do
        local src="${UPGRADE_DIR}/${bin}${suffix}"
        if [[ ! -f "$src" ]]; then
            echo "  SKIP: ${bin}${suffix} not found"
            continue
        fi
        if [[ "$bin" == "upgrader" ]]; then
            mkdir -p "${tmp_dir}/tmp"
            cp "$src" "${tmp_dir}/tmp/upgrader.exe"
            echo "  ADD: ${bin}${suffix} -> tmp/upgrader.exe"
        else
            cp "$src" "${tmp_dir}/${bin}.exe"
            echo "  ADD: ${bin}${suffix} -> ${bin}.exe"
        fi
    done

    if [[ -d "${SCRIPT_DIR}/windows" ]]; then
        for f in "${SCRIPT_DIR}/windows"/*; do
            [[ -f "$f" ]] || continue
            cp "$f" "${tmp_dir}/$(basename "$f")"
            echo "  ADD: script/windows/$(basename "$f")"
        done
    fi

    (cd "$tmp_dir" && zip -qr "${OUTPUT_DIR}/${zip_name}" .)
    echo "  => ${OUTPUT_DIR}/${zip_name}"
    rm -rf "$tmp_dir"
}

# ── Linux ─────────────────────────────────────────────────────────────────
package_unix linux amd64   "_linux"          linux
package_unix linux arm64   "_linux_arm64"    linux
package_unix linux mips64  "_linux_mips64"   linux
package_unix linux loong64 "_linux_loong64"  linux

# ── macOS ─────────────────────────────────────────────────────────────────
package_unix macos amd64   "_macos"          macos
package_unix macos arm64   "_macos_arm64"    macos

# ── Windows ───────────────────────────────────────────────────────────────
package_windows amd64 ".exe"
package_windows 386   "_386.exe"

echo ""
echo "All packages:"
ls -lh "${OUTPUT_DIR}"/agent_*.zip

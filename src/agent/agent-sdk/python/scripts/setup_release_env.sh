#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BASE_PYTHON="${BASE_PYTHON:-python3}"
RELEASE_VENV="${RELEASE_VENV:-${PROJECT_DIR}/.venv-release}"
VENV_PYTHON="${RELEASE_VENV}/bin/python"

if [[ ! -x "${VENV_PYTHON}" ]]; then
  echo "Creating release virtual environment at ${RELEASE_VENV}"
  if ! "${BASE_PYTHON}" -m venv "${RELEASE_VENV}"; then
    echo "Failed to create the release virtual environment with ${BASE_PYTHON}" >&2
    echo "Install a Python distribution with the venv module, or set BASE_PYTHON to one." >&2
    exit 1
  fi
fi

echo "Installing release tools into ${RELEASE_VENV}"
"${VENV_PYTHON}" -m pip install -r "${PROJECT_DIR}/requirements-release.txt"
echo "Release environment is ready: ${VENV_PYTHON}"

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RELEASE_VENV="${RELEASE_VENV:-${PROJECT_DIR}/.venv-release}"
DIST_DIR="${DIST_DIR:-${PROJECT_DIR}/dist}"
CLEAN_DIST="${CLEAN_DIST:-1}"

if [[ -z "${PYTHON_BIN:-}" ]]; then
  if [[ ! -x "${RELEASE_VENV}/bin/python" ]]; then
    RELEASE_VENV="${RELEASE_VENV}" "${SCRIPT_DIR}/setup_release_env.sh"
  fi
  PYTHON_BIN="${RELEASE_VENV}/bin/python"
fi

if ! "${PYTHON_BIN}" -c "import build" >/dev/null 2>&1; then
  echo "Missing build dependency. Run: RELEASE_VENV=${RELEASE_VENV} ${SCRIPT_DIR}/setup_release_env.sh" >&2
  exit 1
fi

mkdir -p "${DIST_DIR}"

if [[ "${CLEAN_DIST}" == "1" ]]; then
  old_artifacts=(
    "${DIST_DIR}"/bkci_agent_sdk-*.whl
    "${DIST_DIR}"/bkci_agent_sdk-*.tar.gz
  )
  for artifact in "${old_artifacts[@]}"; do
    [[ -e "${artifact}" ]] && rm -f -- "${artifact}"
  done
fi

echo "Building distributions into ${DIST_DIR}"
"${PYTHON_BIN}" -m build \
  --sdist \
  --wheel \
  --outdir "${DIST_DIR}" \
  "${PROJECT_DIR}"

artifacts=(
  "${DIST_DIR}"/bkci_agent_sdk-*.whl
  "${DIST_DIR}"/bkci_agent_sdk-*.tar.gz
)
built_count=0
for artifact in "${artifacts[@]}"; do
  if [[ -f "${artifact}" ]]; then
    echo "Built: ${artifact}"
    built_count=$((built_count + 1))
  fi
done

if [[ "${built_count}" -lt 2 ]]; then
  echo "Build did not produce both wheel and source distribution" >&2
  exit 1
fi

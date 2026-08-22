#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RELEASE_VENV="${RELEASE_VENV:-${PROJECT_DIR}/.venv-release}"
DIST_DIR="${DIST_DIR:-${PROJECT_DIR}/dist}"

if [[ -z "${TWINE_REPOSITORY_URL:-}" ]]; then
  echo "TWINE_REPOSITORY_URL is required" >&2
  exit 1
fi
if [[ -z "${TWINE_USERNAME:-}" ]]; then
  echo "TWINE_USERNAME is required" >&2
  exit 1
fi
if [[ -z "${TWINE_PASSWORD:-}" ]]; then
  echo "TWINE_PASSWORD is required" >&2
  exit 1
fi

if [[ -z "${PYTHON_BIN:-}" ]]; then
  if [[ ! -x "${RELEASE_VENV}/bin/python" ]]; then
    RELEASE_VENV="${RELEASE_VENV}" "${SCRIPT_DIR}/setup_release_env.sh"
  fi
  PYTHON_BIN="${RELEASE_VENV}/bin/python"
fi

if ! "${PYTHON_BIN}" -c "import twine" >/dev/null 2>&1; then
  echo "Missing twine dependency. Run: RELEASE_VENV=${RELEASE_VENV} ${SCRIPT_DIR}/setup_release_env.sh" >&2
  exit 1
fi

artifacts=()
for artifact in \
  "${DIST_DIR}"/bkci_agent_sdk-*.whl \
  "${DIST_DIR}"/bkci_agent_sdk-*.tar.gz; do
  [[ -f "${artifact}" ]] && artifacts+=("${artifact}")
done

if [[ "${#artifacts[@]}" -eq 0 ]]; then
  echo "No distribution artifacts found in ${DIST_DIR}; run scripts/build_dist.sh first" >&2
  exit 1
fi

echo "Checking ${#artifacts[@]} distribution artifact(s)"
"${PYTHON_BIN}" -m twine check --strict "${artifacts[@]}"

echo "Uploading ${#artifacts[@]} distribution artifact(s) to ${TWINE_REPOSITORY_URL}"
export TWINE_NON_INTERACTIVE="${TWINE_NON_INTERACTIVE:-1}"
"${PYTHON_BIN}" -m twine upload \
  --repository-url "${TWINE_REPOSITORY_URL}" \
  "${artifacts[@]}"

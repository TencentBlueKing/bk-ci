#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -d, --days <n>      List worktrees unused for more than n days (required)
  -h, --help          Show this help
EOF
    exit 1
}

get_repo_root() {
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local dir="$script_dir"
    for _ in 1 2 3 4; do dir="$(dirname "$dir")"; done
    echo "$dir"
}

DAYS=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        -d|--days)    DAYS="$2"; shift 2 ;;
        -h|--help)    usage ;;
        *)            echo "Unknown option: $1" >&2; usage ;;
    esac
done

if [[ -z "$DAYS" ]]; then
    echo "Error: --days is required." >&2
    usage
fi

REPO_ROOT="$(get_repo_root)"
cd "$REPO_ROOT"

cutoff_ts=$(date -d "-${DAYS} days" +%s 2>/dev/null || date -v-"${DAYS}"d +%s 2>/dev/null)
if [[ -z "$cutoff_ts" ]]; then
    echo "Error: failed to compute cutoff date." >&2
    exit 1
fi

declare -a candidate_branches=()

get_last_activity_ts() {
    local branch="$1" wt_path="$2"
    if [[ -n "$branch" ]]; then
        local ts
        ts=$(git log -1 --format=%ct "$branch" 2>/dev/null || true)
        if [[ "$ts" =~ ^[0-9]+$ ]]; then
            echo "$ts"
            return
        fi
    fi
    # detached HEAD: fall back to directory mtime
    if [[ -d "$wt_path" ]]; then
        if [[ "$(uname)" == "Darwin" ]]; then
            stat -f %m "$wt_path" 2>/dev/null || echo "0"
        else
            stat -c %Y "$wt_path" 2>/dev/null || echo "0"
        fi
    else
        echo "0"
    fi
}

current_path=""
current_branch=""
is_bare=false
first=true

process_entry() {
    local p="$1" b="$2"
    if [[ -z "$p" ]] || ! [[ -d "$p" ]]; then return; fi
    local activity_ts
    activity_ts=$(get_last_activity_ts "$b" "$p")
    if [[ "$activity_ts" -lt "$cutoff_ts" ]]; then
        candidate_branches+=("$b")
    fi
}

while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == worktree\ * ]]; then
        if [[ -n "$current_path" ]] && ! $first; then
            if ! $is_bare; then
                process_entry "$current_path" "$current_branch"
            fi
        fi
        current_path="${line#worktree }"
        current_branch=""
        is_bare=false
        if $first; then first=false; fi
    elif [[ "$line" == branch\ refs/heads/* ]]; then
        current_branch="${line#branch refs/heads/}"
    elif [[ "$line" == "bare" ]]; then
        is_bare=true
    fi
done < <(git worktree list --porcelain)

if [[ -n "$current_path" ]] && ! $first && ! $is_bare; then
    process_entry "$current_path" "$current_branch"
fi

if [[ ${#candidate_branches[@]} -eq 0 ]]; then
    echo "No stale worktrees found (threshold: $DAYS days)."
    exit 0
fi

for b in "${candidate_branches[@]}"; do
    echo "$b"
done

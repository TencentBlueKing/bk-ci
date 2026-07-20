#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<EOF
Usage: $(basename "$0") <Action> [options]

Actions:
  add      Create a new branch and worktree
  list     List all worktrees
  remove   Remove a worktree

Options:
  -b, --branch <name>   Branch name (required for add; optional for remove)
  -p, --path <path>     Explicit worktree path
  -B, --base <ref>      Base ref for the new branch/worktree (default: master)
  -r, --root <dir>      Custom worktree root directory
  -f, --force           Force removal
  -h, --help            Show this help
EOF
    exit 1
}

get_repo_root() {
    local script_dir
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local skills_dir
    skills_dir="$(dirname "$script_dir")"
    local cursor_dir
    cursor_dir="$(dirname "$skills_dir")"
    local dot_cursor
    dot_cursor="$(dirname "$cursor_dir")"
    echo "$(dirname "$dot_cursor")"
}

get_default_worktree_root() {
    local repo_root="$1"
    local repo_parent repo_name
    repo_parent="$(dirname "$repo_root")"
    repo_name="$(basename "$repo_root")"
    echo "${repo_parent}/${repo_name}-wt"
}

get_branch_path_name() {
    local branch="$1"
    echo "$branch" | sed -E 's/[\\/:*?"<>|]+/-/g; s/[[:space:]]+/-/g'
}

resolve_target_path() {
    local repo_root="$1" branch="$2" explicit_path="$3" explicit_root="$4"

    if [[ -n "$explicit_path" ]]; then
        echo "$(cd "$(dirname "$explicit_path")" 2>/dev/null && pwd)/$(basename "$explicit_path")" || echo "$explicit_path"
        return
    fi
    if [[ -z "$branch" ]]; then
        echo "Error: Branch is required when Path is not provided." >&2
        exit 1
    fi

    local wt_root
    if [[ -n "$explicit_root" ]]; then
        wt_root="$(cd "$explicit_root" 2>/dev/null && pwd)" || wt_root="$explicit_root"
    else
        wt_root="$(get_default_worktree_root "$repo_root")"
    fi
    echo "${wt_root}/$(get_branch_path_name "$branch")"
}

invoke_git() {
    echo "git $*"
    git "$@"
}

test_local_branch_exists() {
    git show-ref --verify --quiet "refs/heads/$1" 2>/dev/null
}

get_worktree_path_by_branch() {
    local target_branch="$1" current_path="" line
    while IFS= read -r line; do
        if [[ "$line" == worktree\ * ]]; then
            current_path="${line#worktree }"
        elif [[ "$line" == "branch refs/heads/$target_branch" ]]; then
            echo "$current_path"
            return
        fi
    done < <(git worktree list --porcelain)
}

sync_directory() {
    local repo_root="$1" target_path="$2" dir_name="$3" display_name="$4"
    local source_dir="${repo_root}/${dir_name}"
    local target_dir="${target_path}/${dir_name}"

    if [[ ! -d "$source_dir" ]]; then
        return
    fi

    mkdir -p "$target_path"
    if command -v rsync >/dev/null 2>&1; then
        rsync -a --delete "${source_dir}/" "${target_dir}/"
    else
        rm -rf "$target_dir"
        cp -R "$source_dir" "$target_dir"
    fi
    echo "Synced ${display_name}: $target_dir"
}

run_all_jooq() {
    local target_path="$1"
    local backend_dir="${target_path}/src/backend/ci"

    if [[ ! -x "${backend_dir}/gradlew" ]]; then
        echo "Skip JOOQ: gradlew not found at ${backend_dir}/gradlew" >&2
        return
    fi

    local tasks=(
        "generateGenenrateJooq"
        "generateArtifactoryGenenrateJooq"
        "generateDispatchGenenrateJooq"
        "generateDispatch_kubernetesGenenrateJooq"
        "generateEnvironmentGenenrateJooq"
        "generatePluginGenenrateJooq"
        "generateProcessGenenrateJooq"
        "generateProjectGenenrateJooq"
        "generateQualityGenenrateJooq"
        "generateRepositoryGenenrateJooq"
    )

    echo "Running all JOOQ tasks in: $backend_dir"
    (cd "$backend_dir" && ./gradlew "${tasks[@]}" --console=plain)
}

ACTION=""
BRANCH=""
PATH_ARG=""
BASE="master"
ROOT=""
FORCE=false

if [[ $# -lt 1 ]]; then
    usage
fi

ACTION="$(echo "$1" | tr '[:upper:]' '[:lower:]')"
shift

while [[ $# -gt 0 ]]; do
    case "$1" in
        -b|--branch) BRANCH="$2"; shift 2 ;;
        -p|--path)   PATH_ARG="$2"; shift 2 ;;
        -B|--base)   BASE="$2"; shift 2 ;;
        -r|--root)   ROOT="$2"; shift 2 ;;
        -f|--force)  FORCE=true; shift ;;
        -h|--help)   usage ;;
        *) echo "Unknown option: $1" >&2; usage ;;
    esac
done

REPO_ROOT="$(get_repo_root)"
cd "$REPO_ROOT"

case "$ACTION" in
    add)
        if [[ -z "$BRANCH" ]]; then
            echo "Error: add requires --branch." >&2
            exit 1
        fi

        TARGET_PATH="$(resolve_target_path "$REPO_ROOT" "$BRANCH" "$PATH_ARG" "$ROOT")"
        TARGET_PARENT="$(dirname "$TARGET_PATH")"
        mkdir -p "$TARGET_PARENT"

        if test_local_branch_exists "$BRANCH"; then
            echo "Error: Local branch '$BRANCH' already exists. The add action always creates a new branch together with the worktree." >&2
            exit 1
        fi

        invoke_git worktree add -b "$BRANCH" "$TARGET_PATH" "$BASE"

        sync_directory "$REPO_ROOT" "$TARGET_PATH" ".idea" "IDEA config"
        sync_directory "$REPO_ROOT" "$TARGET_PATH" ".cursor" "Cursor config"
        echo "Created worktree: $TARGET_PATH"
        run_all_jooq "$TARGET_PATH"
        ;;
    list)
        invoke_git worktree list
        ;;
    remove)
        TARGET_PATH=""
        if [[ -n "$PATH_ARG" ]]; then
            TARGET_PATH="$(cd "$PATH_ARG" 2>/dev/null && pwd)" || TARGET_PATH="$PATH_ARG"
        elif [[ -n "$BRANCH" ]]; then
            TARGET_PATH="$(get_worktree_path_by_branch "$BRANCH")"
            if [[ -z "$TARGET_PATH" ]]; then
                TARGET_PATH="$(resolve_target_path "$REPO_ROOT" "$BRANCH" "" "$ROOT")"
            fi
        else
            echo "Error: remove requires --branch or --path." >&2
            exit 1
        fi

        if $FORCE; then
            invoke_git worktree remove --force "$TARGET_PATH"
        else
            invoke_git worktree remove "$TARGET_PATH"
        fi
        echo "Removed worktree: $TARGET_PATH"
        ;;
    *)
        echo "Error: Unknown action '$ACTION'. Use add, list, or remove." >&2
        usage
        ;;
esac

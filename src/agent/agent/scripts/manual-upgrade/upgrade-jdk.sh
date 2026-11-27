#!/usr/bin/env bash
# Run as the Agent installation user after its builds have finished.
set -eu -o pipefail
die() { echo "JDK upgrade: $*" >&2; exit 1; }
usage='bash upgrade-jdk.sh AGENT_DIR JDK8_URL_OR_FILE JDK17_URL_OR_FILE'
if [[ $# == 3 && $1 != --* ]]; then
    agent_dir=$1; jdk8=$2; jdk17=$3
else
    while [[ $# -ge 2 ]]; do
        case $1 in --agent-dir) agent_dir=$2 ;; --jdk8) jdk8=$2 ;; --jdk17) jdk17=$2 ;; *) die "$usage" ;; esac
        shift 2
    done
    [[ $# == 0 ]] || die "$usage"
fi
[[ -n ${agent_dir:-} && -n ${jdk8:-} && -n ${jdk17:-} ]] || die "$usage"
root=$(cd -- "$agent_dir" && pwd -P)
props="$root/.agent.properties"
[[ -f $props && -f $root/devopsAgent ]] || die "Expected an Agent installation at $root"
os=$(uname -s)
work="$root/.bkci-jdk-upgrade"

property() {
    awk -v key="$1" 'NR==1 {sub(/^\357\273\277/,"")} /^[[:space:]]*\[/ {exit}
        {i=match($0,/[=:]/); k=substr($0,1,i-1); gsub(/^[[:space:]]+|[[:space:]]+$/,"",k)
         if(i && k==key) {v=substr($0,i+1); gsub(/^[[:space:]]+|[[:space:]]+$/,"",v)}}
        END {print v}' "$props"
}
agent_id=$(property devops.agent.id)
[[ $agent_id =~ ^[A-Za-z0-9_-]+$ ]] || die "Invalid devops.agent.id"
unit="devops_agent_$agent_id.service"
if [[ -f $root/.install_type ]]; then controller=cli
elif [[ $os == Linux && $(systemctl show -p LoadState "$unit" 2>/dev/null || :) == LoadState=loaded ]]; then controller=systemd
elif [[ -f $root/start.sh && -f $root/stop.sh ]]; then controller=scripts
else die "Cannot find the installed Agent service or start.sh/stop.sh"; fi

control() (
    cd "$root"
    case $controller in cli) ./devopsAgent "$1" ;; systemd) systemctl "$1" "$unit" ;; scripts) bash "./$1.sh" ;; esac
)

# Beta.96's scripts use these PID files and can return success even after a failed stop/start.
alive() {
    local pid file="$root/runtime/$1.pid" name=devopsAgent
    [[ $1 != daemon ]] || name=devopsDaemon
    [[ -f $file ]] && pid=$(cat "$file") && [[ $pid =~ ^[1-9][0-9]*$ ]] || return 1
    if [[ $os == Linux ]]; then [[ /proc/$pid/exe -ef $root/$name ]]
    else [[ $(ps -ww -p "$pid" -o comm= 2>/dev/null) == "$root/$name" ]]; fi
}
wait_for() {
    local i
    for ((i=0; i<30; i++)); do
        if [[ $1 == start ]]; then alive daemon && return 0
        elif ! alive daemon && ! alive agent; then return 0; fi
        sleep 1
    done
    echo "Timed out waiting for $1 in $root" >&2
    return 1
}

target() {
    local key=jdk17 name value
    [[ $1 != 8 ]] || key=jdk
    value=$(property "devops.agent.$key.dir.path")
    value=${value:-$root/$key}
    name=${value##*/}
    [[ $value == "$root/$name" && ! -L $value ]] || die "Expected an Agent-private JDK path: $value"
    if [[ $1 == 8 ]]; then [[ $name =~ ^(jdk|jre)(-[0-9]+)?$ ]]
    else [[ $name =~ ^jdk17(-[0-9]+)?$ ]]; fi || die "Unexpected JDK directory: $value"
    echo "$name"
}

prepare() {
    local v=$1 source=$2 unpack="$work/unpack$1" home bin archive output
    case $source in
        http://*|https://*) archive="$work/$v.tar.gz"; curl -fsSL --retry 2 --connect-timeout 30 -o "$archive" -- "$source" ;;
        *) archive=$source ;;
    esac
    mkdir "$unpack"
    tar -xzf "$archive" --no-same-owner -C "$unpack"
    local candidates=()
    while IFS= read -r -d '' bin; do
        home=${bin%/bin/javac}
        [[ ! -f $home/release ]] || candidates+=("$home")
    done < <(find "$unpack" -path '*/bin/javac' -print0)
    [[ -n ${candidates[0]:-} && -z ${candidates[1]:-} ]] || die "Expected one complete JDK $v in the package"
    home=${candidates[0]}
    output=$("$home/bin/java" -version 2>&1) || die "Cannot run JDK $v: $output"
    local major='1\.8'; [[ $v == 8 ]] || major=17
    grep -Eq "^(openjdk|java) version \"$major([._+\"-]|$)" <<< "$output" || die "Expected Java $v: $output"
    echo "$output"
    if [[ $os == Darwin && $home == */Contents/Home ]]; then mv "${home%/Contents/Home}" "$work/new$v"
    elif [[ $os == Darwin ]]; then mkdir -p "$work/new$v/Contents"; mv "$home" "$work/new$v/Contents/Home"
    else mv "$home" "$work/new$v"; fi
}

mkdir -m 700 "$work" || die "Check for another upgrade or remaining backups in $work"
phase=prepare
finish() {
    local rc=$? v dest
    trap - EXIT INT TERM HUP
    if [[ $phase == replace ]]; then
        echo "Restoring previous JDKs before restarting the Agent." >&2
        for v in 17 8; do
            dest="$root/${names[$v]}"
            if [[ ! -e $work/new$v ]]; then mv "$dest" "$work/new$v" || exit 1; fi
            if [[ -e $work/old$v ]]; then mv "$work/old$v" "$dest" || exit 1; fi
        done
        control start && wait_for start || { echo "Check Agent startup. Kept $work" >&2; exit 1; }
    elif [[ $phase == starting ]]; then
        echo "Check Agent startup. New JDKs are installed; previous directories are in $work/old8 and old17." >&2
        exit 1
    fi
    rm -rf "$work"
    exit "$rc"
}
trap finish EXIT
trap 'exit 130' INT TERM HUP
prepare 8 "$jdk8"
prepare 17 "$jdk17"
for v in 8 17; do names[$v]=$(target "$v"); echo "JDK $v -> $root/${names[$v]}"; done
printf '%s\n' "${names[8]}" "${names[17]}" > "$work/targets"
# Legacy stop.sh kills the PID from disk. Reject a stale PID that now belongs to another program.
for role in daemon agent; do
    pid_file="$root/runtime/$role.pid"
    if [[ -f $pid_file ]]; then
        pid=$(cat "$pid_file")
        if [[ $pid =~ ^[1-9][0-9]*$ ]] && kill -0 "$pid" 2>/dev/null && ! alive "$role"; then
            die "Stale $pid_file points to another process; check it before stopping this Agent"
        fi
    fi
done
control stop
wait_for stop
phase=replace
for v in 8 17; do
    [[ $(target "$v") == "${names[$v]}" ]] || die "JDK configuration changed during preparation"
    dest="$root/${names[$v]}"
    if [[ -e $dest ]]; then mv "$dest" "$work/old$v"; fi
    mv "$work/new$v" "$dest"
done
phase=starting
control start
wait_for start
phase=done
echo "Agent restarted with the new JDKs."

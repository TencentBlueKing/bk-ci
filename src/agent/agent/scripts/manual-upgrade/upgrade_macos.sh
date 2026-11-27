#!/usr/bin/env bash
# macOS only. Linux uses upgrade.sh, Windows uses upgrade.ps1.
set -Eeuo pipefail
export LC_ALL=C
usage() { echo "用法: bash upgrade_macos.sh upgrade 安装目录 包URL或本地tar.gz | rollback 安装目录 | confirm 安装目录" >&2; exit 2; }
fail() { echo "失败 [$step] $*" >&2; exit 1; }
warn() { echo "提醒 [$step] $*" >&2; }
[[ $# -ge 2 ]] || usage
if [[ $# == 2 && "$1" != rollback && "$1" != confirm && "$1" != upgrade ]]; then set -- upgrade "$@"; fi
action=$1
case "$action:$#" in upgrade:3|rollback:2|confirm:2) ;; *) usage ;; esac
step=检查安装目录
root=$(cd -- "$2" && pwd -P) || fail "找不到目录 $2"
[[ -f "$root/.agent.properties" && ! -L "$root/.agent.properties" ]] || fail "$root 缺少原安装的 .agent.properties"
state="$root/.bkci-script-upgrade"
[[ ! -L "$state" ]] || fail "$state 是符号链接"
original_umask=$(umask)
umask 077
mkdir -p "$state"
log=$(mktemp "$state/run.XXXXXXXX")
exec > >(tee -a "$log") 2>&1
echo "安装目录 $root；日志 $log"
pending="$state/pending"
busy="$state/running"
mkdir "$busy" 2>/dev/null || fail "无法获取 $busy；请检查写权限及另一升级进程，异常中断见手册"
prepared_here=no
restart_needed=no
item=
trap 'echo "失败 [$step] ${item:-} (行 $LINENO，退出码 $?)" >&2' ERR

check_destination() {
    local rel="$1"
    while :; do
        [[ ! -L "$root/$rel" ]] || fail "$rel 是符号链接"
        [[ "$rel" == */* ]] || break
        rel=${rel%/*}
        [[ ! -e "$root/$rel" || -d "$root/$rel" ]] || fail "$rel 不是目录"
    done
    [[ ! -e "$root/$1" || -f "$root/$1" ]] || fail "$1 不是普通文件"
}

# Only act on processes whose executable belongs to this installation.
instance_pids() {
    local name=$1 pid exe candidates
    candidates=$(pgrep -x "$name") || {
        if [[ $? != 1 && ${2:-} == inspect ]]; then echo "?查询失败"; fi
        return 0
    }
    for pid in $candidates; do
        exe=$(ps -ww -p "$pid" -o comm= 2>/dev/null) || exe=''
        if [[ "$exe" != "$root/$name" ]] && command -v lsof >/dev/null 2>&1 &&
            lsof -a -p "$pid" -d txt -Fn 2>/dev/null | grep -Fx -e "n$root/$name" -e "n$pending/old/$name" >/dev/null; then
            exe="$root/$name"
        fi
        if [[ "${exe% (deleted)}" == "$root/$name" || "${exe% (deleted)}" == "$pending/old/$name" ]]; then
            echo "$pid"
        elif [[ ${2:-} == inspect && "$exe" != /* ]]; then
            # Ignore exited children, which can retain their process name as zombies.
            case "$(ps -p "$pid" -o stat= 2>/dev/null)" in ''|*Z*) ;; *) echo "?$pid" ;; esac
        fi
    done
}

instance_list() {
    local name pid
    for name in devopsDaemon devopsAgent devopsCollector; do
        for pid in $(instance_pids "$name" inspect); do
            case "$pid" in
                \?*) echo "$name PID=${pid#?}（路径待确认）" ;;
                *) echo "$root/$name PID=$pid" ;;
            esac
        done
    done
}

# Prefer the installation's own stop.sh/start.sh, then a confirmed service
# manager, and finally this installation's own processes.
select_controller() {
    command -v pgrep >/dev/null || { warn "缺少 pgrep，无法识别本实例进程"; return 1; }
    local id mode wd
    id=$(sed -nE 's/^[[:space:]]*devops\.agent\.id[[:space:]]*=[[:space:]]*([a-zA-Z0-9_-]+)[[:space:]]*$/\1/p' "$root/.agent.properties")
    [[ "$id" =~ ^[a-zA-Z0-9_-]+$ ]] || { warn "无法读取 devops.agent.id"; return 1; }
    service="devops_agent_$id"
    mode=''
    if [[ -f "$root/.install_type" ]]; then mode=$(tr -d '[:space:]' < "$root/.install_type" | tr '[:lower:]' '[:upper:]'); fi
    controller=direct
    if [[ -f "$root/stop.sh" && -f "$root/start.sh" ]]; then
        if [[ -L "$root/stop.sh" || -L "$root/start.sh" ]]; then
            warn "stop.sh / start.sh 是符号链接；改用进程启停"
        else
            controller=script
        fi
    elif [[ -f "$root/stop.sh" || -f "$root/start.sh" ]]; then
        warn "安装目录只找到 stop.sh 或 start.sh 中的一个；改用进程启停"
    fi
    if [[ "$controller" != script ]]; then
        domain="gui/$(id -u)"
        plist="$HOME/Library/LaunchAgents/$service.plist"
        if [[ "$mode" == DAEMON || ( ! -f "$plist" && -f "/Library/LaunchDaemons/$service.plist" ) ]]; then
            domain=system
            plist="/Library/LaunchDaemons/$service.plist"
        elif [[ "$mode" == BACKGROUND ]]; then domain="user/$(id -u)"; fi
        if [[ -f "$plist" ]]; then
            wd=$(/usr/libexec/PlistBuddy -c 'Print :WorkingDirectory' "$plist" 2>/dev/null) || wd=''
            if [[ -n "$wd" && "$wd" -ef "$root" ]]; then
                controller=launchd
                if ! launchctl help bootstrap >/dev/null 2>&1; then controller=launchd-old; fi
            else
                warn "$plist 的 WorkingDirectory 与当前目录不符或未设置；跳过 launchd 自动启停"
            fi
        elif [[ -n "$mode" && "$mode" != DIRECT ]]; then
            warn "找不到 $plist；改用按进程启停，请确认原服务不会自动拉起"
        fi
    fi
    echo "启停方式 $controller；实例 $service"
}

check_upgrader() {
    [[ -z "$(instance_pids upgrader)" ]] || warn "本实例的自动升级进程仍在运行，请核对最终版本"
}

# Bound the wait for old hooks and service managers.
run_control() {
    local child deadline
    (cd "$root" && umask "$original_umask" && exec "$@") </dev/null >>"$log" 2>&1 &
    child=$!
    deadline=$((SECONDS + 30))
    while kill -0 "$child" 2>/dev/null; do
        if (( SECONDS >= deadline )); then
            warn "$* 超过 30 秒，结束等待，请人工核对"
            if jobs -pr | grep -Fx -- "$child" >/dev/null; then kill -TERM "$child" 2>/dev/null || :; fi
            return 1
        fi
        sleep 1
    done
    wait "$child"
}

stop_instance() {
    local name pid i file exe running use_stop_hook=yes
    running=$(instance_list)
    echo "停机前进程检查：${running:-未发现本目录的进程}"
    check_upgrader
    if [[ "$controller" == script ]]; then
        for name in daemon agent; do
            file="$root/runtime/$name.pid"
            [[ -e "$file" || -L "$file" ]] || { use_stop_hook=no; continue; }
            case "$name" in daemon) exe=devopsDaemon ;; agent) exe=devopsAgent ;; esac
            pid=''
            if [[ -f "$file" && ! -L "$file" ]] && pid=$(cat "$file") &&
                [[ "$pid" =~ ^[1-9][0-9]*$ && "$pid" != 1 ]] && instance_pids "$exe" | grep -Fx -- "$pid" >/dev/null; then
                continue
            fi
            warn "$file (PID $pid) 无法确认归属，跳过 stop.sh，仅处理已核实的进程"
            use_stop_hook=no
        done
    fi
    case "$controller" in
        script)
            if [[ "$use_stop_hook" == yes ]]; then
                run_control bash ./stop.sh || warn "stop.sh 执行失败，继续处理"
            fi ;;
        launchd) if launchctl print "$domain/$service" >/dev/null 2>&1; then run_control launchctl bootout "$domain/$service" || warn "服务停止失败，继续处理"; fi ;;
        launchd-old) if launchctl list "$service" >/dev/null 2>&1; then run_control launchctl unload "$plist" || warn "服务停止失败，继续处理"; fi ;;
    esac
    for name in devopsDaemon devopsAgent devopsCollector; do
        for pid in $(instance_pids "$name"); do
            if [[ "$pid" == 1 ]] || ! instance_pids "$name" | grep -Fx -- "$pid" >/dev/null; then
                warn "无法再次确认 $root/$name (PID $pid)，跳过该进程"
                continue
            fi
            echo "停止已确认的 $root/$name (PID $pid)"
            kill -TERM "$pid" 2>/dev/null || [[ -z "$(instance_pids "$name")" ]] || warn "无法停止 $name (PID $pid)"
        done
        for ((i=0; i<12; i++)); do [[ -z "$(instance_pids "$name")" ]] && break; sleep 1; done
        [[ -z "$(instance_pids "$name")" ]] || warn "等待 $name 停止超时"
    done
    check_upgrader
}

start_instance() {
    local i pair previous='' launched='' running
    running=$(instance_list)
    if [[ -n "$running" ]]; then
        warn "跳过启动，避免重复实例；请人工核对：$running"
        return 0
    fi
    case "$controller" in
        script) run_control bash ./start.sh || { warn "start.sh 执行失败"; return 1; } ;;
        launchd)
            if ! launchctl print "$domain/$service" >/dev/null 2>&1; then run_control launchctl bootstrap "$domain" "$plist" || return 1; fi
            run_control launchctl kickstart "$domain/$service" || return 1 ;;
        launchd-old) if ! launchctl list "$service" >/dev/null 2>&1; then run_control launchctl load "$plist" || return 1; fi ;;
        direct)
            if [[ -z "$(instance_pids devopsDaemon)" ]]; then
                [[ -x "$root/devopsDaemon" ]] || { warn "无法执行 $root/devopsDaemon"; return 1; }
                (cd "$root"; umask "$original_umask"; exec nohup "$root/devopsDaemon") </dev/null >/dev/null 2>&1 &
                launched=$!
            fi ;;
    esac
    for ((i=0; i<30; i++)); do
        pair="$(instance_pids devopsDaemon):$(instance_pids devopsAgent)"
        if [[ "$pair" != :* && "$pair" != *: && "$pair" == "$previous" ]]; then
            if [[ "$pair" == *$'\n'* ]]; then
                warn "$root 有多个稳定进程，保留全部供核对（Daemon:Agent PID）：$pair"
            fi
            return 0
        fi
        if [[ -n "$launched" && "$pair" == :* ]] && ! kill -0 "$launched" 2>/dev/null; then warn "devopsDaemon 启动后退出，请查看 $root/logs"; return 1; fi
        previous=$pair
        sleep 1
    done
    warn "等待本目录的 Daemon 和 Agent 启动超时，请查看 $root/logs"
    return 1
}

manage_instance() {
    if { instance_pids devopsDaemon; instance_pids devopsAgent; } | grep -Fx '1' >/dev/null; then
        warn "$root 包含 PID 1，由容器控制器重启"
        return 0
    fi
    if ! select_controller; then
        [[ "$1" == stop_instance ]] || { warn "无法确认启动方式，跳过启动"; return 0; }
        controller=none
    fi
    if ! "$1"; then
        warn "$root 的进程操作未完成，请人工核对服务状态"
    fi
}

finish() {
    local done_dir
    done_dir=$(mktemp -d "$state/done.XXXXXXXX")
    mv "$pending" "$done_dir/transaction"
    rm -rf "$done_dir" || echo "已结束，临时文件待清理：$done_dir"
}

restore() {
    step=回滚
    rm -f "$pending/applied" "$pending/complete"
    local kind path old
    while read -r kind path; do
        item=$path
        check_destination "$path"
        old="$pending/old/$path"
        if [[ "$kind" == E && -f "$old" ]]; then
            mv -f "$old" "$root/$path"
        elif [[ "$kind" == N && ! -e "$pending/new/$path" ]]; then
            rm -f "$root/$path"
        fi
    done < "$pending/journal"
    while IFS= read -r path; do
        rmdir "$root/$path" 2>/dev/null || :
    done < "$pending/dirs"
    cp "$pending/owned.before" "$state/owned"
}

recover() {
    if [[ -f "$pending/armed" ]]; then manage_instance stop_instance; restore; fi
    manage_instance start_instance
    finish
    echo "原版本文件已恢复。"
}

record_owned() {
    sort -u "$pending/owned.before" "$pending/files" > "$state/owned.next"
    mv -f "$state/owned.next" "$state/owned"
}

cleanup() {
    local rc=$?
    trap - EXIT ERR INT TERM
    set +e
    if [[ "$restart_needed" == yes ]]; then
        echo "升级中断 [$step] $item；正在恢复原版本及服务"
        (set -e; recover)
        if [[ $? != 0 ]]; then echo "恢复未完成。请保留 $pending，排除日志中的错误后执行 rollback。"; fi
    elif [[ "$prepared_here" == yes && ! -e "$pending/armed" ]]; then
        rm -rf "$pending"
    fi
    rm -f "$busy/pid"
    rmdir "$busy"
    exit "$rc"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
echo "$$" > "$busy/pid"

if [[ "$action" != upgrade ]]; then
    [[ -d "$pending" && ! -L "$pending" ]] || fail "没有待处理升级"
    if [[ "$action" == confirm ]]; then
        step=验收
        [[ -f "$pending/applied" ]] || fail "文件替换未完成；保持停机并执行 rollback"
        record_owned
        finish
        echo "已提交并清理本次备份，可以恢复节点接单。"
    elif [[ -f "$pending/armed" || -f "$pending/restart" ]]; then
        step=回滚
        recover
    else
        finish
        echo "已清理未写入安装目录的暂存包。"
    fi
    exit 0
fi

if [[ ! -L "$pending" && -f "$pending/complete" ]]; then finish; fi
[[ ! -e "$pending" && ! -L "$pending" ]] || fail "上次升级未结束，请先执行 rollback"
mkdir "$pending"
prepared_here=yes
mkdir "$pending/new" "$pending/old"
step="1/4 获取升级包"
echo "[$step]"
case "$3" in
    http://*|https://*) curl -fL --retry 2 --output "$pending/package.tar.gz" -- "$3" ;;
    *) cp -- "$3" "$pending/package.tar.gz" ;;
esac

step="2/4 检查包和目标文件"
echo "[$step]"
tar -tzf "$pending/package.tar.gz" > "$pending/members"
tar -tvzf "$pending/package.tar.gz" > "$pending/types"
awk 'substr($0,1,1)!="-" {print "包中含有非常规文件: " $0; exit 1}' "$pending/types"
awk '
    function bad() {print "不支持的升级包条目: " $0 > "/dev/stderr"; exit 1}
    $0=="bkci-package-target" {if (marker++) bad(); next}
    {
        n=split($0,a,"/"); root=tolower(a[1])
        if (!n || root ~ /^(workspace|runtime|logs|tmp|upgrade|config|conf|cert|data|bkci-package-target)$/ ||
            tolower($0) ~ /\.(properties|lock|pid)$/ || seen[tolower($0)]++) bad()
        for (i=1;i<=n;i++) if (a[i] !~ /^[A-Za-z0-9_+-][A-Za-z0-9_.+-]*$/ || a[i] ~ /\.$/) bad()
        print
    }
    END {if (marker!=1) exit 1}
' "$pending/members" > "$pending/files"
[[ -s "$pending/files" ]] || fail "包中没有升级文件"
(umask 022; tar -xzf "$pending/package.tar.gz" --no-same-owner -C "$pending/new")
rm "$pending/package.tar.gz"
[[ "$(uname -s)" == Darwin ]] || fail "此脚本仅用于 macOS，Linux 请使用 upgrade.sh"
os=darwin
arch=$(uname -m)
case "$arch" in
    x86_64|amd64) arch=amd64 ;;
    arm64) arch=arm64 ;;
    *) fail "不支持的架构 $arch" ;;
esac
target=$(cat "$pending/new/bkci-package-target")
[[ "$target" == "$os-$arch" ]] || fail "包为 $target，本机需要 $os-$arch"

touch "$state/owned"
cp "$state/owned" "$pending/owned.before"
plan() {
    : > "$pending/journal"
    : > "$pending/dirs.unsorted"
    while IFS= read -r item; do
        check_destination "$item"
        if [[ -e "$root/$item" ]]; then
            case "$item" in devopsAgent|devopsDaemon|upgrader|devopsCollector|worker-agent.jar|agent_docker_init.sh|jdk/*|jdk17/*) ;;
                *) grep -Fxq -- "$item" "$state/owned" || fail "同名文件 $item 已存在且不属于发布程序" ;;
            esac
            echo "E $item" >> "$pending/journal"
            echo "  替换 $item"
        else
            echo "N $item" >> "$pending/journal"
            echo "  新增 $item"
        fi
        parent=$item
        while [[ "$parent" == */* ]]; do
            parent=${parent%/*}
            [[ -d "$root/$parent" ]] || printf '%s\n' "$parent" >> "$pending/dirs.unsorted"
        done
    done < "$pending/files"
    sort -ru "$pending/dirs.unsorted" > "$pending/dirs"
}
plan
step="3/4 停机替换"
echo "[$step] 尝试停止 $root 的进程"
touch "$pending/restart"
restart_needed=yes
manage_instance stop_instance
plan > /dev/null
touch "$pending/armed"
while read -r kind item; do
    echo "[$step] $item"
    if [[ "$item" == */* ]]; then
        (umask 022; mkdir -p "$root/${item%/*}")
        mkdir -p "$pending/old/${item%/*}"
    fi
    if [[ "$kind" == E ]]; then mv "$root/$item" "$pending/old/$item"; fi
    mv "$pending/new/$item" "$root/$item"
done < "$pending/journal"
touch "$pending/applied"
record_owned
touch "$pending/complete"
restart_needed=no
step="4/4 重启并检查进程"
echo "[$step] 尝试启动 $root 的进程"
manage_instance start_instance

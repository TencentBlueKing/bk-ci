#!/usr/bin/env bash
# Supply two real archives/URLs to smoke-test them instead of the offline fixtures.
set -eu -o pipefail
script=$(cd "$(dirname "$0")/.." && pwd -P)/upgrade-jdk.sh
tmp=$(mktemp -d)
root="$tmp/test agent"
mkdir -p "$root/jre" "$root/runtime"
cp /bin/sleep "$root/devopsDaemon"
cat > "$root/devopsAgent" <<'CONTROL'
#!/usr/bin/env bash
set -eu
echo "$1" >> calls
case $1 in
    stop) if [[ -f runtime/daemon.pid ]]; then kill "$(cat runtime/daemon.pid)"; fi ;;
    start) [[ ! -f fail-start ]]; ./devopsDaemon 300 </dev/null >/dev/null 2>&1 & echo $! > runtime/daemon.pid ;;
esac
CONTROL
chmod +x "$root/devopsAgent"
printf 'bash ./devopsAgent start\n' > "$root/start.sh"
printf 'bash ./devopsAgent stop\n' > "$root/stop.sh"
cleanup() {
    if [[ -f $root/runtime/daemon.pid ]]; then kill "$(cat "$root/runtime/daemon.pid")" 2>/dev/null || :; fi
    rm -rf "$tmp"
}
trap cleanup EXIT
echo old > "$root/jre/old"
printf 'devops.agent.id=test\ndevops.agent.jdk.dir.path=%s/jre\ndevops.agent.jdk17.dir.path=%s/jdk17-123\n' "$root" "$root" > "$root/.agent.properties"
(cd "$root" && bash ./start.sh)
if [[ $# == 2 ]]; then
    bash "$script" "$root" "$1" "$2"
    echo 'Real-package restart passed.'
    exit 0
fi

for v in 8 17; do
    home="$tmp/package$v/Kona"
    [[ $(uname -s) != Darwin ]] || home="$home/Contents/Home"
    mkdir -p "$home/bin"
    java_version=17.0.1; [[ $v != 8 ]] || java_version=1.8.0_111
    printf '#!/bin/sh\necho "openjdk version \\"%s\\"" >&2\n' "$java_version" > "$home/bin/java"
    touch "$home/release" "$home/bin/javac"
    chmod +x "$home/bin/"*
    tar -czf "$tmp/$v.tar.gz" -C "$tmp/package$v" .
done
run() { bash "$script" "$root" "$tmp/8.tar.gz" "$1"; }
if run "$tmp/8.tar.gz"; then exit 1; fi
[[ $(cat "$root/calls") == start && -f $root/jre/old ]]
# Simulate a rename failure after 8 was installed, while 17 was originally missing.
mkdir "$tmp/tools"
cat > "$tmp/tools/mv" <<'MV'
#!/bin/sh
case "$1:$2" in */new17:*/jdk17-123) exit 1 ;; esac
exec /bin/mv "$@"
MV
chmod +x "$tmp/tools/mv"
if PATH="$tmp/tools:$PATH" run "$tmp/17.tar.gz"; then exit 1; fi
[[ -f $root/jre/old && ! -e $root/jdk17-123 && ! -e $root/.bkci-jdk-upgrade ]]
run "$tmp/17.tar.gz"                              # beta.96 start.sh / stop.sh
touch "$root/.install_type"
run "$tmp/17.tar.gz"                              # current CLI
[[ -d $root/jre && -d $root/jdk17-123 && ! -e $root/jre/old && ! -e $root/.bkci-jdk-upgrade ]]
touch "$root/fail-start"
if run "$tmp/17.tar.gz"; then exit 1; fi
[[ -d $root/.bkci-jdk-upgrade/old8 && -d $root/.bkci-jdk-upgrade/old17 ]]
echo 'Bash stop/replace/start tests passed.'

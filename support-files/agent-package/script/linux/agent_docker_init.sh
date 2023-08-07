#!/bin/bash -l
# 本脚本应该兼容 POSIX shell.

CI_DIR="/data"
CI_LOG_DIR="$CI_DIR/logs"
CI_LOG_FILE="$CI_LOG_DIR/docker.log"

ci_log() {
   date=$(date +%Y%m%d-%H%M%S)
   logl=${ci_log_LEVEL:-INFO}
   msg="$date $logl $*"
   if [ -n "$CI_LOG_FILE" ]; then echo "$msg" >>"$CI_LOG_FILE"; fi
   echo "$msg" >&2
}

unset_proxy() {
    unset http_proxy
    unset https_proxy
    unset ftp_proxy
    unset no_proxy
    unset all_proxy
    unset HTTP_PROXY
    unset HTTPS_PROXY
    unset FTP_PROXY
    unset NO_PROXY
    unset ALL_PROXY
}

cert_update() {
    mkdir -p /etc/pki/ca-trust/source/anchors/
    cp /data/bkdevops/apps/certs/git.code.oa.com/* /etc/pki/ca-trust/source/anchors/
    update-ca-trust
}

mkdir  -p /data/devops
cd /data/devops
mkdir -p /data/logs

ci_log "docker_init.sh was launched."

export LANG="zh_CN.UTF-8"

ci_log "download docker_init.sh success, start it..."
ci_log $(cat docker_init.sh)
ci_log "start to copy worker-agent.jat as the docker.jar..."

cp /data/worker-agent.jar /data/devops/docker.jar
chmod +x docker.jar

ci_log "copy docker.jar finished, ready to start it..."

exec /usr/local/jre/bin/java -Dfile.encoding=UTF-8 -DLC_CTYPE=UTF-8 -Dbuild.type=DOCKER -Dsun.zip.disableMemoryMapping=true -Xmx1024m -Xms128m -jar docker.jar "$@" >>/data/logs/docker.log 2>&1

ci_log "Start to copy the log message to workspace"

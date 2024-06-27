#!/bin/bash -l
# 本脚本应该兼容 POSIX shell.

CI_DIR="/data/devops"
CI_LOG_DIR="$CI_DIR/logs"
CI_LOG_FILE="$CI_LOG_DIR/docker.log"

ci_log() {
   date=$(date +%Y%m%d-%H%M%S)
   logl=${ci_log_LEVEL:-INFO}
   msg="$date $logl $*"
   if [ -n "$CI_LOG_FILE" ]; then echo "$msg" >>"$CI_LOG_FILE"; fi
   echo "$msg" >&2
}

mkdir -p /data/devops/logs
cd /data/devops

ci_log "docker_init.sh was launched."

export LANG="zh_CN.UTF-8"

ci_log "start to copy worker-agent.jat as the docker.jar..."

cp /data/worker-agent.jar /data/devops/docker.jar
chmod +x docker.jar

ci_log "copy docker.jar finished, ready to start it..."

exec /usr/local/jre/bin/java -Dfile.encoding=UTF-8 -DLC_CTYPE=UTF-8 -Dbuild.type=DOCKER -Dsun.zip.disableMemoryMapping=true -Xmx1024m -Xms128m -jar docker.jar "$@" >>"$CI_LOG_DIR/docker.log" 2>&1

ci_log "end"

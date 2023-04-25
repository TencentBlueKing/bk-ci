#!/bin/bash
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

ci_log "unset proxy"
unset_proxy
ci_log "git.code.oa.com certs update"
cert_update

yum clean all || echo no yum
rm -rf /var/lib/rpm/__db*
rpm --rebuilddb || echo no rpm

ci_log "start to download the docker_init.sh..."

curl -k -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}" -o  docker_init.sh "${devops_gateway}/static/bkrepo/files/docker_init.sh" -L

ci_log "download docker_init.sh success, start it..."
ci_log $(cat docker_init.sh)

chmod +x ./docker_init.sh
exec ./docker_init.sh "$@"

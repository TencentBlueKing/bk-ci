#!/bin/bash
set -x
mkdir  -p /data/devops
cd /data/devops
mkdir -p /data/logs

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

echo "unset proxy" > /data/logs/docker.log
unset_proxy
echo "git.code.oa.com certs update" >> /data/logs/docker.log
cert_update

yum clean all || echo no yum
rm -rf /var/lib/rpm/__db*
rpm --rebuilddb || echo no rpm

echo "start to download the docker_init.sh..." > /data/logs/docker.log
curl -k -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}" -o  docker_init.sh "${devops_gateway}/static/bkrepo/files/docker_init.sh" -L
echo docker_init
cat docker_init.sh
echo "download docker_init.sh success, start it..." >> /data/logs/docker.log
cat docker_init.sh >> /data/logs/docker.log

chmod +x ./docker_init.sh
exec ./docker_init.sh "$@"
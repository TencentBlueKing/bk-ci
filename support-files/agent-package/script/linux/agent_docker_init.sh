#!/bin/bash
set -x
mkdir  -p /data/devops
cd /data/devops
mkdir -p /data/logs
echo "start to download the docker_init.sh..." > /data/logs/docker.log
curl -k -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}" -o  docker_init.sh "${devops_gateway}/static/local/files/docker_init.sh" -L
echo docker_init
cat docker_init.sh
echo "download docker_init.sh success, start it..." >> /data/logs/docker.log
cat docker_init.sh >> /data/logs/docker.log
sh docker_init.sh $@
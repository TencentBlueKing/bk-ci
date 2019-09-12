#!/bin/bash

echo "start to download the docker_init.sh..." > /data/devops/logs/docker.log
cd /data/devops/workspace
#sleep 1000

curl -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}" -o  docker_init.sh "${devops_gateway}/ms/dispatch/api/build/scripts?scriptName=docker_init.sh"
echo "download docker_init.sh success, start it..." >> /data/devops/logs/docker.log
#cp -r /data/bkdevops/apps/jdk/1.8.0_161_landun/jre /usr/local/jre
#export PATH="/usr/local/jre/bin:${PATH}"
cat docker_init.sh >> /data/devops/logs/docker.log
sh docker_init.sh $@
#!/bin/bash
set -x

mkdir  -p /data/devops
cd /data/devops

mkdir -p logs
echo "start to download the docker_init.sh..." > logs/docker.log

curl -k -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}" -o  docker_init.sh "${devops_gateway}/static/local/files/docker_init.sh" -L

echo docker_init
cat docker_init.sh

echo "download docker_init.sh success, start it..." >> logs/docker.log

if [ -d "/data/bkdevops/apps/jdk/KonaJDK8_landun" ]; then
  echo "local /data/bkdevops/apps/jdk/KonaJDK8_landun is exsited,copy jre to /usr/local/jre" > logs/docker.log
  cp -r /data/bkdevops/apps/jdk/KonaJDK8_landun /usr/local/jre >> logs/docker.log 2>&1
else
  echo "local /data/bkdevops/apps/jdk/KonaJDK8_landun is not exsited." > logs/docker.log
fi

cat docker_init.sh >> logs/docker.log
sh docker_init.sh $@
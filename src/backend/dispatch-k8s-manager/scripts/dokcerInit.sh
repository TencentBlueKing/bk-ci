#!/bin/bash
set -x

if [ -z "${BUILDLESS_STARTUP_FLAG}" ]; then
    # MY_ENV_VAR 未设置，将其设置为 "new"
    export BUILDLESS_STARTUP_FLAG="NEW"
    echo "BUILDLESS_STARTUP_FLAG is not set, setting its value to 'NEW'"
else
    # BUILDLESS_STARTUP_FLAG 已存在，将其设置为 "second"
    export BUILDLESS_STARTUP_FLAG="RESTART"
    echo "BUILDLESS_STARTUP_FLAG is set, changing its value to 'RESTART'"
fi

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

## Download the docker.jar
cd /data/devops
echo "start to download the docker.jar..." >> logs/docker.log

curl -k -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}"  -o docker.jar "${devops_gateway}/static/local/files/jar/worker-agent.jar"

echo "download the docker.jar finished, ready to start it..." >> logs/docker.log
md5sum docker.jar >> logs/docker.log 2>&1
fileSize=`wc -c docker.jar | awk '{print $1}'`
echo $fileSize >> logs/docker.log
if [ "$fileSize" -lt 102400 ]
then
    cat docker.jar >> logs/docker.log
fi

random_string=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 16)

/usr/local/jre/bin/java -Dfile.encoding=UTF-8 -DLC_CTYPE=UTF-8 -Dsun.jnu.encoding=UTF-8 -DLOG_PATH=/data/devops/logs/${random_string} -Dlandun.env=prod -Dbuild.type=DOCKER -Ddevops.gateway=${devops_gateway}  -jar docker.jar $@ >> logs/docker.log 2>&1

#!/bin/bash
## Download the docker.jar
export LANG="zh_CN.UTF-8"

URL=${devops_gateway}/ms/dispatch/api/build/dockers
cd /data/devops/workspace
echo "start to download the docker.jar..." > /data/devops/logs/docker.log

curl -s -H "X-DEVOPS-BUILD-TYPE: DOCKER" -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}"  -o docker.jar ${URL}

echo "download the docker.jar finished, ready to start it..." >> /data/devops/logs/docker.log 
md5sum docker.jar >> /data/devops/logs/docker.log 2>&1
fileSize=`wc -c docker.jar | awk '{print $1}'`
echo $fileSize >> /data/devops/logs/docker.log
if [ "$fileSize" -lt 102400 ]
then
    cat docker.jar >> /data/devops/logs/docker.log
fi

/usr/local/jre/bin/java -Dfile.encoding=UTF-8 -DLC_CTYPE=UTF-8 -Dlandun.env=prod -Dbuild.type=DOCKER -Ddevops.gateway=${devops_gateway}  -jar docker.jar $@ >> /data/devops/logs/docker.log 2>&1
#/usr/local/jre/bin/java -Dfile.encoding=UTF-8 -DLC_CTYPE=UTF-8 -Dlandun.env=dev -jar docker.jar $@ >> /data/devops/logs/docker.log
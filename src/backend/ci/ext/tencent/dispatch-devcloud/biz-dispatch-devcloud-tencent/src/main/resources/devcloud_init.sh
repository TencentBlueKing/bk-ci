#!/bin/bash


echo "$(date):start to make director and make link ." >> /data/logs/docker.log
mkdir -p /data/bkdevops
if [[ -d "/data/bkdevops/apps" ]]; then echo link to apps success ; else ln -s /tools/ /data/bkdevops/apps;  fi
mkdir -p /data/landun/thirdparty/maven_repo
mkdir -p /root/.m2/
ln -s /data/landun/thirdparty/maven_repo /root/.m2/repository
mkdir -p /data/landun/thirdparty/npm_prefix
mkdir -p /root/.npm/
ln -s /data/landun/thirdparty/npm_prefix /root/.npm/prefix
mkdir -p /data/landun/thirdparty/npm_cache
mkdir -p /root/.npm/
ln -s /data/landun/thirdparty/npm_cache /root/.npm/cache
# mkdir -p /data/landun/thirdparty
# ln -s /data/landun/thirdparty /root/.npmrc
mkdir -p /data/landun/thirdparty/ccache/
ln -s /data/landun/thirdparty/ccache/ /root/.ccache
mkdir -p /data/landun/thirdparty/gradle_caches
mkdir -p /root/.gradle/
ln -s /data/landun/thirdparty/gradle_caches /root/.gradle/caches
mkdir -p /data/landun/thirdparty/go_cache
mkdir -p /root/go/pkg/
ln -s /data/landun/thirdparty/go_cache /root/go/pkg/mod
mkdir -p /data/landun/thirdparty/.ivy2
mkdir -p /root/
ln -s /data/landun/thirdparty/.ivy2 /root/.ivy2
mkdir -p /data/landun/thirdparty/sbt_cache
mkdir -p /root/
ln -s /data/landun/thirdparty/sbt_cache /root/.cache
mkdir -p /data/landun/thirdparty/yarn_cache
mkdir -p /usr/local/share/.cache
ln -s /data/landun/thirdparty/yarn_cache /usr/local/share/.cache

cd /data/
mkdir -p /data/logs/

echo "$(date):make director and make link finished ." >> /data/logs/docker.log

echo "$(date):start to copy jre." >> /data/logs/docker.log
cp -r /tools/jdk/1.8.0_161_landun/jre /usr/local/jre
echo "$(date):copy jre finished." >> /data/logs/docker.log

#for((i=1;i<=3;i++));
#do
#    echo "$(date):start to download docker_init.sh $i time."
#    echo "$(date):start to download docker_init.sh $i time." >> /data/logs/docker.log
#    curl -s -o  docker_init.sh "http://${devops_gateway}/dispatch/gw/build/scripts/docker_init.sh"  >> /data/logs/docker.log 2>&1
#    # curl -H "x-devops-project-id:grayproject" -s -o  docker_init.sh "http://${devops_gateway}/dispatch/gw/build/scripts/docker_init.sh"  >> /data/logs/docker.log 2>&1
#    if [ ! -f "docker_init.sh" ]; then
#        echo "$(date):download docker_init.sh failed,retry to download the docker_init.sh."
#        echo "$(date):download docker_init.sh failed,retry to download the docker_init.sh." >> /data/logs/docker.log
#        continue
#    else
#        break
#    fi
#done

num=3
for i in `seq $num`;
do
    echo "$(date):start to download docker_init.sh $i time."
    echo "$(date):start to download docker_init.sh $i time." >> /data/logs/docker.log
    curl -s -o  docker_init.sh "http://${devops_gateway}/dispatch/gw/build/scripts/docker_init.sh"  >> /data/logs/docker.log 2>&1
    # curl -H "x-devops-project-id:grayproject" -s -o  docker_init.sh "http://${devops_gateway}/dispatch/gw/build/scripts/docker_init.sh"  >> /data/logs/docker.log 2>&1
    if [ ! -f "docker_init.sh" ]; then
        echo "$(date):download docker_init.sh failed,retry to download the docker_init.sh."
        echo "$(date):download docker_init.sh failed,retry to download the docker_init.sh." >> /data/logs/docker.log
        continue
    else
        break
    fi
done

echo "$(date):download docker_init.sh success, start it..."
echo "$(date):download docker_init.sh success, start it..." >> /data/logs/docker.log
echo "$(date):docker_init.sh content="
echo "$(date):docker_init.sh content=" >> /data/logs/docker.log
cat docker_init.sh >> /data/logs/docker.log

echo "ready to execute docker_init.sh..."
echo "ready to execute docker_init.sh..." >> /data/logs/docker.log
sh docker_init.sh $@


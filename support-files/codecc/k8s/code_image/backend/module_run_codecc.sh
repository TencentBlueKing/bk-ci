#! /bin/sh

# build init.sh to hosted which will be used in container
hosts=$(ping -c 1 $BK_CI_PRIVATE_HOST|head -1|egrep -o "([0-9]{1,3}.){3}[0-9]{1,3}")
hosts="$hosts $BK_CI_PRIVATE_HOST"
template=$(cat /base/init.sh) && echo "${template/__hosts__/$hosts}" > /data/docker/bkci/ci/agent-package/script/init.sh

mkdir -p /data/docker/bkci/codecc/backend/logs
java -cp boot-$module.jar \
    -server \
    -Xloggc:/data/docker/bkci/codecc/backend/logs/gc.log \
    -XX:NewRatio=1 \
    -XX:SurvivorRatio=8 \
    -XX:+PrintTenuringDistribution \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -XX:+UseConcMarkSweepGC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=oom.hprof \
    -XX:ErrorFile=error_sys.log \
    -Dservice-suffix=ci \
    -Dloader.path="/data/docker/bkci/codecc/backend/classpath/" \
    -Dspring.profiles.active=codecc-prod \
    -Dspring.cloud.config.enabled=false \
    -Dservice.log.dir=/data/docker/bkci/codecc/backend/logs/ \
    -Dsun.jnu.encoding=UTF-8 \
    -Dfile.encoding=UTF-8 \
    -Dspring.config.location=/data/docker/bkci/codecc/backend/bootstrap/bootstrap.yaml \
    -Dspring.cloud.consul.host=$NODE_IP \
    org.springframework.boot.loader.PropertiesLauncher
#! /bin/sh
mkdir -p /data/docker/bkci/codecc/backend/logs
suffix=ci
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
    -Dservice-suffix=${suffix} \
    -Dloader.path="/data/docker/bkci/codecc/backend/classpath/" \
    -Dspring.profiles.active=codecc-prod \
    -Dspring.cloud.config.enabled=false \
    -Dservice.log.dir=/data/docker/bkci/codecc/backend/logs/ \
    -Dsun.jnu.encoding=UTF-8 \
    -Dfile.encoding=UTF-8 \
    -Dspring.config.location=/data/docker/bkci/codecc/backend/bootstrap/bootstrap.yaml \
    -Dspring.cloud.consul.host=$NODE_IP \
    -Dspring.application.name=report${suffix} \
    -Dserver.port=${BK_CODECC_REPORT_API_PORT} \
    org.springframework.boot.loader.PropertiesLauncher
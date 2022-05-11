#! /bin/sh

mkdir -p $BK_REPO_LOGS_DIR
chmod 777 $BK_REPO_LOGS_DIR

java -server \
     -Dsun.jnu.encoding=UTF-8 \
     -Dfile.encoding=UTF-8 \
     -Xloggc:$BK_REPO_LOGS_DIR/gc.log \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=oom.hprof \
     -XX:ErrorFile=$BK_REPO_LOGS_DIR/error_sys.log \
     -Dspring.profiles.active=$BK_REPO_PROFILE \
     -Dservice.prefix=$BK_REPO_SERVICE_PREFIX \
     $BK_REPO_JVM_OPTION \
     -jar /data/workspace/app.jar

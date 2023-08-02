#!/bin/bash
JAVA_TOOL_OPTIONS_TMP=$JAVA_TOOL_OPTIONS
echo "source env files..."
source service.env
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS $JAVA_TOOL_OPTIONS_TMP"

MEM_OPTS="-XX:+UseContainerSupport -Xss512k -XX:MaxMetaspaceSize=500m -XX:CompressedClassSpaceSize=100m -XX:ReservedCodeCacheSize=400m -XX:-UseAdaptiveSizePolicy -XX:MaxGCPauseMillis=100"
GC_LOG="-Xloggc:/data/workspace/$MS_NAME/jvm/gc-%t.log -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
API_PORT=80

echo "create log dir"
ci_ms_log="/data/logs"
ci_ms_data="/data/local/"
mkdir -p "$ci_ms_log" "$ci_ms_data"
ln -srfT "$ci_ms_log" logs
ln -srfT "$ci_ms_data" data

echo "create java args"
java_env=() java_argv=() java_run=""
java_env+=("CLASSPATH=$CLASSPATH")
java_argv+=("-Dfatjar=/$MS_NAME/boot-$MS_NAME.jar") # 兼容fatjar文件名匹配进程.
java_run="$MAIN_CLASS"
for k in LANG USER HOME SHELL LOGNAME PATH HOSTNAME LD_LIBRARY_PATH ${!JAVA_*} ${!SPRING_*}; do
  if [ -n "${!k-}" ]; then java_env+=("$k=${!k}"); fi # 如果定义, 则传递.
done
java_argv+=(
  "-Ddevops_gateway=$DEVOPS_GATEWAY"
  "-Dserver.port=$API_PORT" # 强制覆盖配置文件里的端口.
  "-Dbksvc=bk-ci-$MS_NAME"
  "-Dspring.cloud.kubernetes.config.sources[0].name=${RELEASE_NAME:-bkci}-${CHART_NAME:-bk-ci}-common"
  "-Dspring.cloud.kubernetes.config.sources[1].name=${RELEASE_NAME:-bkci}-${CHART_NAME:-bk-ci}-${MS_NAME}"
  "-Dspring.main.allow-bean-definition-overriding=true"
  "-Dspring.main.allow-circular-references=true"
  "-Dservice-suffix="
  "-Dspring.profiles.active=local,dev"
  "-Dspring.application.name=$MS_NAME"
  "-Dspring.main.allow-circular-references=true"
  "-Dspring.cloud.kubernetes.config.includeProfileSpecificSources=false"
)

echo "run java"
java -server "${java_argv[@]}" $MEM_OPTS $GC_LOG $java_run

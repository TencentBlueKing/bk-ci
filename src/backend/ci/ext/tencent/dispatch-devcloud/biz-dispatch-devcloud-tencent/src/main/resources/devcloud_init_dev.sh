#!/bin/bash
# shellcheck disable=SC1090
# CI公共构建机初始化. 本脚本应该兼容 POSIX shell.
# 脚本退出码:
# 0 启动启动成功.
# 20-49 可安全重试, dispatch会重新调度到其他主机. 一般适用于网络问题.
# 50-80 不可重试, dispatch应该停止调度, 报错供用户排查.

ci_log (){
  date=$(date +%Y%m%d-%H%M%S)
  logl=${ci_log_LEVEL:-INFO}
  msg="$date $logl $*"
  if [ -n "$CI_LOG_FILE" ]; then echo "$msg" >> "$CI_LOG_FILE"; fi
  echo "$msg" >&2
}

fail (){
  if [ $# -lt 2 ]; then
    ret=4
    set -- "$0:$LINENO: Usage: fail RET_CODE MESSAGE"
  else
    ret=$1
    shift
  fi
  ci_log_LEVEL=ERROR ci_log "$@"
  exit "$ret"
}

ci_curl (){
  ci_curl_TIMEOUT=${ci_curl_TIMEOUT:-60}
  curl -k -sfS -L --connect-timeout 3 -m "$ci_curl_TIMEOUT" \
    -H "X-DEVOPS-BUILD-TYPE: DOCKER" \
    -H "X-DEVOPS-PROJECT-ID: ${devops_project_id}" \
    -H "X-DEVOPS-AGENT-ID: ${devops_agent_id}" \
    -H "X-DEVOPS-AGENT-SECRET-KEY: ${devops_agent_secret_key}" \
    "$@"
  return $?
}

ci_down (){
  ci_down_FILE=$1
  ci_down_URL=$2
  ci_down_COUNT=0
  ci_down_SUCCESS=0
  until [ "$ci_down_SUCCESS" -gt 0 ]; do
    ci_down_COUNT=$((ci_down_COUNT+1))
    ci_log "ci_down: start downloading $ci_down_URL to $ci_down_FILE."
    ci_down_RESP_HEADER="$(ci_curl -v -o "$ci_down_FILE" "$ci_down_URL" 2>&1)"
    ret=$?
    ci_down_REMOTE_SIZE=$(echo "$ci_down_RESP_HEADER" | awk -v RS="[\r\n]+" '/[Cc]ontent-[Ll]ength:/{s=$3}END{print s}')
    ci_down_FILE_SIZE=$(du -b "$ci_down_FILE" | cut -f 1)
    # 下载成功后, 校验大小, 通过则成功.
    if [ "$ret" -eq 0 ]; then
      if [ "$ci_down_FILE_SIZE" = "$ci_down_REMOTE_SIZE" ]; then
        ci_log "ci_down: download ok. $ci_down_URL was saved to $ci_down_FILE, $ci_down_FILE_SIZE bytes."
        ci_down_SUCCESS=1
        return 0
      else
        ci_log "ci_down: file size mismatch: $ci_down_FILE: header=$ci_down_REMOTE_SIZE, actual=$ci_down_FILE_SIZE. try again $ci_down_COUNT..."
      fi
    else
      ci_log "ci_down: curl returns $ret, try again $ci_down_COUNT..."
    fi
    if [ "$ci_down_COUNT" -ge 3 ]; then  # 重试3次.
      ci_log "ci_down: max retry reached, break."
      break
    fi
    sleep 2
  done
  return "$ret"
}

CI_DIR="/data"
CI_LOG_DIR="$CI_DIR/logs"
CI_LOG_FILE="$CI_LOG_DIR/docker.log"
#touch "$CI_LOG_FILE" || true  # 尝试创建文件.
mkdir -p "$CI_LOG_DIR" || fail 20 "failed to create dir: $CI_LOG_DIR"  # 可能主机磁盘不足, 允许重新调度
cd "$CI_DIR" || fail 54 "failed to cd ci-dir: $CI_DIR"  # 可能执行用户权限及镜像问题. 需要直接退出.

ci_log "devcloud init.sh was launched."

ci_log "start to make director and make link."
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

#cd /data/
#mkdir -p /data/logs/

ci_log "make director and make link finished ."

ci_log "start to copy jre."
cp -r /tools/jdk/1.8.0_161_landun/jre /usr/local/jre
ci_log "copy jre finished."

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

#num=3
#for i in `seq $num`;
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
ci_log "start to download the docker_init.sh..."
# 允许使用环境变量 CI_DOCKER_INIT_URL 改变下载地址.
if [ -z "$CI_DOCKER_INIT_URL" ]; then
  CI_DOCKER_INIT_URL="http://${devops_gateway}/dispatch/gw/build/scripts/docker_init.sh"
  ci_log "set CI_DOCKER_INIT_URL to $CI_DOCKER_INIT_URL."
else
  ci_log "pick CI_DOCKER_INIT_URL from env: $CI_DOCKER_INIT_URL."
fi
#sleep 1000
docker_init_filename="docker_init.sh"
if ci_down "$docker_init_filename" "$CI_DOCKER_INIT_URL"; then
  ci_log "download $docker_init_filename success, load it..."
else
  ci_log "failed to download $docker_init_filename. first 10 line is:"
  ci_log "$(head "$docker_init_filename")"
  fail 22 "docker-init-download-failed"  # 如果下载失败, 可能当前主机网络问题, 可重新调度.
fi


#echo "$(date):download docker_init.sh success, start it..."
#echo "$(date):download docker_init.sh success, start it..." >> /data/logs/docker.log
#echo "$(date):docker_init.sh content="
#echo "$(date):docker_init.sh content=" >> /data/logs/docker.log
#cat docker_init.sh >> /data/logs/docker.log

ci_log "ready to execute docker_init.sh..."
sh docker_init.sh $@


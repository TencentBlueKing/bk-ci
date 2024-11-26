#!/bin/bash
function isPidExists()
{
  for i in `ps aux | grep -v grep | awk '{print $2}'`;do
    if [[ $1 == $i ]];then
      return 0
    fi
  done
  return 1
}

function start()
{
  echo "start unzipping jdk17(jdk17.zip) package"
  if [[ -d "jdk17" ]]; then
    echo "jdk17 already exists, skip unzip"
  else
    unzip -q -o jdk17.zip -d jdk17
  fi
  echo "start unzipping jdk(jre.zip) package"
  if [[ -d "jdk" ]]; then
    echo "jdk already exists, skip unzip"
  else
    unzip -q -o jre.zip -d jdk
  fi

  if [[ ! -d  "${workspace}/workspace" ]]; then
    echo "create agent workspace dir: ${workspace}/workspace"
    mkdir -p ${workspace}/workspace
    chmod 777 ${workspace}/workspace
  fi

  if [[ ! -d  "${workspace}/logs" ]]; then
    echo "create agent logs dir: ${workspace}/logs"
    mkdir -p ${workspace}/logs
    chmod 777 ${workspace}/logs
  fi

  pid=0
  if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
     pid=`cat ${workspace}/runtime/daemon.pid`
  fi
  if isPidExists $pid; then
    echo "agent daemon already running, pid: $pid"
  else
    chmod +x devopsDaemon
    chmod +x devopsAgent
    chmod +x *.sh
    nohup ${workspace}/devopsDaemon $1> /dev/null 2>&1 &
    sleep 2s
    if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
      pid=`cat ${workspace}/runtime/daemon.pid`
      if isPidExists $pid; then
        echo "agent daemon is running, pid: $pid"
      fi
    fi
  fi
}

# ----------------------------------

workspace=`pwd`
user=${USER}
echo "current user: ${user}"
echo "agent workdir: ${workspace}"

start $1
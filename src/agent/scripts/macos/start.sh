#!/bin/bash
workspace=`pwd`
user=${USER}

echo "current user: ${user}"
echo "agent workdir: ${workspace}"

function isPidExists()
{
  for i in `ps aux | grep -v grep | awk '{print $2}'`;do
    if [[ $1 == ${i} ]];then
      return 0
    fi
  done
  return 1
}

function start()
{
  if [[ ! -d  "jre" ]]; then
    echo "unzipping the jre "
    unzip -q -o jre.zip -d jre
  fi

  if [[ ! -d  "${workspace}/workspace" ]]; then
    echo "create workspace dir: ${workspace}/workspace"
    mkdir -p ${workspace}/workspace
    chmod 777 ${workspace}/workspace
  fi

  if [[ ! -d  "${workspace}/logs" ]]; then
    echo "create logs dir: ${workspace}/logs"
    mkdir -p ${workspace}/logs
    chmod 777 ${workspace}/logs
  fi

  pid=0
  if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
     pid=`cat ${workspace}/runtime/daemon.pid`
  fi
  if isPidExists ${pid}; then
    echo "agent daemon is running, pid: $pid"
  else
    echo "start agent"
    chmod +x devopsDaemon
    chmod +x devopsAgent
    chmod +x *.sh

    nohup ${workspace}/devopsDaemon > /dev/null 2>&1 &
    echo "agent starts"

    sleep 2s
    if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
      pid=`cat ${workspace}/runtime/daemon.pid`
      if isPidExists ${pid}; then
        echo "agent daemon is running, pid: $pid"
      fi
    fi
  fi
}

start
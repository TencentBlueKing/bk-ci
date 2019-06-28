#!/bin/bash
workspace=`pwd`
user=${USER}

function start()
{
  if [[ ! -d  "jre" ]]; then
    echo "Unzipping the jre package"
    unzip -q -o jre.zip -d jre
  fi

  if [[ ! -d  "${workspace}/workspace" ]]; then
    echo "Create the workspace ${workspace}/workspace"
    mkdir -p ${workspace}/workspace
    chmod 777 ${workspace}/workspace
  fi

  if [[ ! -d  "${workspace}/logs" ]]; then
    echo "Create the logs ${workspace}/logs"
    mkdir -p ${workspace}/logs
    chmod 777 ${workspace}/logs
  fi

  pid=$(ps aux | grep devopsDaemon | grep -v grep | awk '{print $2}')
  if [[ -n "$pid" ]]; then
    echo "agent is running, pid: $pid"
  else
    echo "start agent"
    chmod +x devopsDaemon
    chmod +x devopsAgent
    chmod +x *.sh

    ${workspace}/devopsDaemon &
    echo "agent starts"

    sleep 2s
    pid=$(ps aux | grep devopsDaemon | grep -v grep | awk '{print $2}')
    if [[ -n "$pid" ]]; then
      echo "agent is running, pid: $pid"
    fi
  fi
}

start
#!/bin/bash
workspace=`pwd`
user=${USER}

function isPidExists()
{
  for i in `ps aux | grep -v grep | awk '{print $2}'`;do
    if [[ $1 == ${i} ]];then
      return 0
    fi
  done
  return 1
}

function stop()
{
  pid=0
  if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
    pid=`cat ${workspace}/runtime/daemon.pid`
  fi
  if isPidExists ${pid}; then
    echo "stop daemon process"
    kill -9 "$pid"
    echo "daemon is stopped"
  else
    echo "daemon is not running, skip"
  fi

  pid=0
  if [[ -f "${workspace}/runtime/agent.pid" ]]; then
    pid=`cat ${workspace}/runtime/agent.pid`
  fi
  if isPidExists ${pid}; then
    echo "stop agent process"
    kill -9 "$pid"
    echo "agent is stopped"
  else
    echo "agent is not running, skip"
  fi
}

stop

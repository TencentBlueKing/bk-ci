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

function stop()
{
  pid=0
  if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
    pid=`cat ${workspace}/runtime/daemon.pid`
  fi
  if isPidExists $pid; then
    echo "stop daemon process"
    if ! kill -9 "$pid"; then
      echo "failed to kill daemon process with PID $pid"
    else
      echo "daemon is stopped"
    fi
  else
    echo "daemon is not running, skip"
  fi
  pid=0
  if [[ -f "${workspace}/runtime/agent.pid" ]]; then
    pid=`cat ${workspace}/runtime/agent.pid`
  fi
  if isPidExists $pid; then
    echo "stop agent process"
    if ! kill -9 "$pid"; then
      echo "failed to kill agent process with PID $pid"
    else
      echo "agent is stopped"
    fi
  else
    echo "agent is not running, skip"
  fi
}

# ----------------------------------

workspace=`pwd`
user=$USER

stop

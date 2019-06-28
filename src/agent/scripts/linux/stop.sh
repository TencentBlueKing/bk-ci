#!/bin/bash
workspace=`pwd`
user=${USER}

function stop()
{
  pid=$(ps aux | grep devopsDaemon | grep -v grep | awk '{print $2}')
  if [[ -n "$pid" ]]; then
    echo "stop daemon process"
    kill -9 "$pid"
    echo "daemon is stoped"
  else
    echo "daemon is not running, skip"
  fi

  pid=$(ps aux | grep devopsAgent | grep -v grep | awk '{print $2}')
  if [[ -n "$pid" ]]; then
    echo "stop agent process"
    kill -9 "$pid"
    echo "agent is stoped"
  else
    echo "agent is not running, skip"
  fi
}

stop

#!/bin/bash
function initLangSup()
{
  current_locale=$(locale | grep -E 'LANG|LC_CTYPE' | cut -d= -f2)
  if echo "$current_locale" | grep -qE 'zh_CN|zh_TW'; then
    sup_zh=true
  else
    sup_zh=false
  fi
}

function print_zh() {
  if [ "$sup_zh" = true ]; then
    echo "$1"
  else
    echo "$2"
  fi
}

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
    print_zh "停止Agent守护进程" "stop daemon process"
    if ! kill -9 "$pid"; then
      print_zh "Agent守护进程停止失败，进程ID: $pid" "failed to kill process with PID $pid"
    else
      print_zh "Agent守护进程停止成功" "daemon is stopped"
    fi
  else
    print_zh "Agent守护进程并未运行，无需停止" "daemon is not running, skip"
  fi
  pid=0
  if [[ -f "${workspace}/runtime/agent.pid" ]]; then
    pid=`cat ${workspace}/runtime/agent.pid`
  fi
  if isPidExists ${pid}; then
    print_zh "停止Agent进程" "stop agent process"
    if ! kill -9 "$pid"; then
      print_zh "Agent进程停止失败，进程ID: $pid" "failed to kill process with PID $pid"
    else
      print_zh "Agent进程停止成功" "agent is stopped"
    fi
  else
    print_zh "Agent进程并未运行，无需停止" "agent is not running, skip"
  fi
}

# ----------------------------------

initLangSup
print_zh "开始停止蓝盾Agent" "start installing Agent..."
workspace=`pwd`
print_zh "安装目录: $workspace" "install Dir: $workspace"
user=$USER
print_zh "安装用户: $workspace" "install User: $user"
stop

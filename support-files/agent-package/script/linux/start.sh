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

function start()
{
  print_zh "开始解压缩jdk17安装包(jdk17.zip)" "start unzipping jdk17 package"
  if [[ -d "jdk17" ]]; then
    print_zh "jdk17安装目录(jdk17)已经存在，无需解压" "jdk17 already exists, skip unzip"
  else
    unzip -q -o jdk17.zip -d jdk17
  fi
  print_zh "开始解压缩jdk8安装包(jre.zip)" "start unzipping jdk package"
  if [[ -d "jdk" ]]; then
    print_zh "jdk8安装目录(jdk)已经存在，无需解压" "jdk already exists, skip unzip"
  else
    unzip -q -o jre.zip -d jdk
  fi

  if [[ ! -d  "${workspace}/workspace" ]]; then
    print_zh "创建Agent工作空间: ${workspace}/workspace" "create workspace dir: ${workspace}/workspace"
    mkdir -p ${workspace}/workspace
    chmod 777 ${workspace}/workspace
  fi

  if [[ ! -d  "${workspace}/logs" ]]; then
    print_zh "创建Agent日志目录: ${workspace}/logs" "create logs dir: ${workspace}/logs"
    mkdir -p ${workspace}/logs
    chmod 777 ${workspace}/logs
  fi

  print_zh "开始启动Agent守护进程" "start agent daemon"
  pid=0
  if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
     pid=`cat ${workspace}/runtime/daemon.pid`
  fi
  if isPidExists ${pid}; then
    print_zh "Agent守护进程已经存在无需启动，进程ID: $pid" "agent daemon is running, pid: $pid"
  else
    chmod +x devopsDaemon
    chmod +x devopsAgent
    chmod +x *.sh
    nohup ${workspace}/devopsDaemon $1> /dev/null 2>&1 &
    sleep 2s
    if [[ -f "${workspace}/runtime/daemon.pid" ]]; then
      pid=`cat ${workspace}/runtime/daemon.pid`
      if isPidExists ${pid}; then
        print_zh "Agent守护进程启动成功，进程ID: $pid" "agent daemon is running, pid: $pid"
      fi
    fi
  fi
}

# ----------------------------------

initLangSup
print_zh "开始启动蓝盾Agent" "start installing Agent..."
workspace=`pwd`
print_zh "安装目录: $workspace" "install Dir: $workspace"
user=$USER
print_zh "安装用户: $workspace" "install User: $user"

start $1
#!/bin/bash
echo "Start installing the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}
agent_id='##agentId##'

function initArch() {
  ARCH=$(uname -m)
  case $ARCH in
    aarch64) ARCH="arm64";;
    arm64) ARCH="arm64";;
    mips64) ARCH="mips64";;
    *) ARCH="";;
  esac
}

function getServiceName()
{
  echo "devops_agent_"${agent_id}
}

function unzip_jdk()
{
  echo "start unzipping the jdk package"
  if [[ -d "jdk" ]]; then
    echo "jdk already exists, skip unzip"
    return
  fi
  unzip -q -o jre.zip -d jdk
}

exists()
{
  command -v "$1" >/dev/null 2>&1
}

function download_agent()
{
  echo "start download agent install package"
  if [[ -f "agent.zip" ]]; then
    echo "agent.zip already exist, skip download"
    return
  fi
  if exists curl; then
    curl -H "X-DEVOPS-PROJECT-ID: ##projectId##" -o agent.zip "##agent_url##"
    if [[ $? -ne 0 ]]; then
      echo "fail to use curl to download the agent, use wget"
      wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##"
    fi
  elif exists wget; then
    wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##"
  else
    echo "curl & wget command don't exist, download fail"
    exit 1
  fi
}

function addRunAtLoad()
{
  mkdir -p ~/Library/LaunchAgents
  cat > ~/Library/LaunchAgents/$(getServiceName).plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
        <string>$(getServiceName)</string>

    <key>Program</key>
        <string>${workspace}/devopsDaemon</string>

    <key>RunAtLoad</key>
        <true/>

    <key>WorkingDirectory</key>
        <string>${workspace}</string>

    <key>KeepAlive</key>
        <false/>
</dict>
</plist>
EOF
}

function uninstallAgentService()
{
  if [[ "$user" != "root"  && -f ~/Library/LaunchAgents/$(getServiceName).plist ]]; then
    echo "remove run at load"
    rm -f ~/Library/LaunchAgents/$(getServiceName).plist
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

function installAgentService()
{
  if [[ "$user" != "root" ]]; then
    echo "add run at load with user ${user}"
    addRunAtLoad
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
}

function writeSSHConfig()
{
    config_file=$HOME/.ssh/config
    if [[ ! -d $HOME/.ssh ]];then
        mkdir -p $HOME/.ssh
    fi

    if [[ -f ${config_file} ]];then

        if [[ $(cat ${config_file}| grep "\-svn.tencent.com"  | wc -l) -lt 1 ]];then
            echo "" >> ${config_file}
            echo "Host *-svn.tencent.com" >> ${config_file}
            echo "StrictHostKeyChecking no" >> ${config_file}
            echo "Port 22" >> $config_file
        fi
        if [[ $(cat ${config_file}| grep "\-scm.tencent.com"  | wc -l) -lt 1 ]];then
            echo "" >> ${config_file}
            echo "Host *-scm.tencent.com" >> ${config_file}
            echo "StrictHostKeyChecking no" >> ${config_file}
            echo "Port 22" >> ${config_file}
        fi
        if [[ $(cat $config_file| grep "\-cd1.tencent.com"  | wc -l) -lt 1 ]];then
            echo "" >> ${config_file}
            echo "Host *-cd1.tencent.com" >> ${config_file}
            echo "StrictHostKeyChecking no" >> ${config_file}
            echo "Port 22" >> ${config_file}
        fi
        if [[ $(cat ${config_file}| grep "Host git.code.oa.com"  | wc -l) -lt 1 ]];then
            echo "" >> ${config_file}
            echo "Host git.code.oa.com" >> ${config_file}
            echo "StrictHostKeyChecking no" >> ${config_file}
            echo "HostName git.code.oa.com" >> ${config_file}
            echo "Port 22" >> ${config_file}
        fi
    else
      cat > ${config_file} <<EOF
Host *-svn.tencent.com
StrictHostKeyChecking no
Port 22
Host *-scm.tencent.com
StrictHostKeyChecking no
Port 22
Host *-cd1.tencent.com
StrictHostKeyChecking no
Port 22
Host git.code.oa.com
StrictHostKeyChecking no
HostName git.code.oa.com
Port 22
EOF
    fi
}

# if [[ "${workspace}" = ~ ]]; then
#   echo 'agent should not install in root of user home directory'
#   echo 'please run install script in an empty directory with full permission'
#   exit 1
# fi

cd ${workspace}

initArch
download_agent
unzip -o agent.zip
unzip_jdk

os=`uname`
arch1=`uname -m`
echo "OS: $os"
echo "ARCH: ${arch1}"

echo "check java version"
jdk/Contents/Home/bin/java -version

echo "check and write ssh config"
writeSSHConfig

uninstallAgentService
installAgentService




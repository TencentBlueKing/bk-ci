#!/bin/bash
echo "Start installing the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}

function initArch() {
  ARCH=$(uname -m)
  case $ARCH in
    aarch64) ARCH="arm64";;
    arm64) ARCH="arm64";;
    mips64) ARCH="mips64";;
    *) ARCH="";;
  esac
}

function initOs() {
  OS=$(uname -s)
  case $OS in
    Darwin) OS="macos";;
    *) OS="linux";;
  esac
}

function getServiceName()
{
  echo "devops_agent_"${agent_id}
}

function unzip_jdk()
{
  echo "start unzipping jdk package"
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

# ------ linux

function installLinuxAgentService()
{
  echo "install agent service with user ${user}"
  grep_result=$(grep "${service_name}" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
      echo "cd ${workspace} && ./devopsDaemon & # ${service_name}" >> /etc/rc.d/rc.local
      echo "add to rclocal"
    else
      echo "already add to rclocal"
    fi
  fi
  
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
}

function uninstallLinuxAgentService()
{
  echo "uninstall agent service"
  grep_result=$(grep "${service_name}" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
      echo "already remove from rclocal"
    else
      sed -i "/${service_name}/d" "/etc/rc.d/rc.local"
      echo "removal done"
    fi
  fi
  
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

# ------ macos

function addMacosRunAtLoad()
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

function uninstallMacosAgentService()
{
  if [[ "$user" != "root"  && -f ~/Library/LaunchAgents/$(getServiceName).plist ]]; then
    echo "remove run at load"
    rm -f ~/Library/LaunchAgents/$(getServiceName).plist
  fi
  
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

function installMacosAgentService()
{
  if [[ "$user" != "root" ]]; then
    echo "add run at load with user ${user}"
    addMacosRunAtLoad
  fi
  
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
}

# ------

function getAgentId()
{
  agentIdstr=$(cat $workspace/.agent.properties | grep devops.agent.id=)
  agent_id="${agentIdstr#*=}"
}

if [[ "$os" == "Darwin" ]]; then
  cd ${workspace}
  
  initOs
  echo "OS: $OS"
  initArch
  echo "ARCH: $ARCH"
  
  download_agent
  
  unzip -o agent.zip
  
  getAgentId
  echo "agentid $agent_id"
  
  unzip_jdk
  echo "check java version"
  jdk/Contents/Home/bin/java -version
  
  uninstallMacosAgentService
  installMacosAgentService
else
  cd ${workspace}
  
  if [[ ! -f "agent.zip" ]]; then
    initOs
    echo "OS: $OS"
    initArch
    echo "ARCH: $ARCH"
    download_agent
    unzip -o agent.zip
  fi
  
  getAgentId
  echo "agentid $agent_id"
  
  unzip_jdk
  echo "check java version"
  jdk/bin/java -version
  
  service_name=`getServiceName`
  
  if [[ "$user" = "root" ]]; then
    uninstallLinuxAgentService
    installLinuxAgentService
  else
    cd ${workspace}
    chmod +x *.sh
    ${workspace}/start.sh
  fi
fi
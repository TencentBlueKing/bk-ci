#!/bin/bash
function checkFiles()
{
  if [[ "$enable_check_files" == "true" ]]; then
    # 检查当前目录是否有文件
    if [ "$(find . -maxdepth 1 -type f | wc -l)" -gt 0 ]; then
      echo "fatal: current directory is not empty, please install in an empty directory"
      exit 1
    fi
  fi
}

function exists()
{
  command -v "$1" >/dev/null 2>&1
}

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
  if [ -f "./jdk17.zip" ]; then
    echo "start unzipping jdk17(jdk17.zip) package"
    if [[ -d "jdk17" ]]; then
      echo "jdk17 already exists, skip unzip"
    else
      unzip -q -o jdk17.zip -d jdk17
    fi
  else
    echo "'jdk17.zip' is not exist"
  fi

  if [ -f "./jre.zip" ]; then
    echo "start unzipping jdk(jre.zip) package"
    if [[ -d "jdk" ]]; then
      echo "jdk already exists, skip unzip"
    else
      unzip -q -o jre.zip -d jdk
    fi
  else
    echo "'jre.zip' is not exist"
  fi
}

function download_agent()
{
  echo "start download agent install package"
  if [[ -f "agent.zip" ]]; then
    echo "agent.zip already exist, skip download"
    return
  fi
  if exists curl; then
    echo "download using curl"
    curl -H "X-DEVOPS-PROJECT-ID: ##projectId##" -o agent.zip "##agent_url##"
    if [[ $? -ne 0 ]]; then
      echo "Fail to use curl to download the agent, use wget"
      wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##"
    fi
    elif exists wget; then
    echo "download using wget"
    wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##"
  else
    echo "curl & wget command don't exist, download fail"
    exit 1
  fi
}

function installRcLocal()
{
  echo "install agent service with rc.local"
  grep_result=$(grep "$service_name" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
      echo "add $service_name to rc.local"
      echo "cd $workspace && ./devopsDaemon & # $service_name" >> /etc/rc.d/rc.local
    else
      echo "already add $service_name to rc.local, no repeated install"
    fi
  fi
  cd $workspace
  chmod +x *.sh
  ${workspace}/start.sh
}

# no using systemd KillMode, will affect Agent self-upgrade
function installSystemd()
{
  echo "install agent service with systemd"
  cat <<EOL > /etc/systemd/system/${service_name}.service
[Unit]
Description=bkdevops agent
After=network.target

[Service]
Type=forking
ExecStart=$workspace/start.sh
ExecStop=$workspace/stop.sh
WorkingDirectory=$workspace
PrivateTmp=false
KillMode=none

[Install]
WantedBy=multi-user.target
EOL
  cd ${workspace}
  chmod +x *.sh
  systemctl daemon-reload
  systemctl enable --now $service_name
}

function installAgentService()
{
  if exists systemctl; then
    installSystemd
  else
    installRcLocal
  fi
  echo "service $service_name has been installed"
}

function doUninstallRcLocal()
{
  if [[ -x "/etc/rc.d/rc.local" ]]; then
    if grep -q "$service_name" "/etc/rc.d/rc.local"; then
      sed -i "/${service_name}/d" "/etc/rc.d/rc.local"
    fi
  fi
}

function uninstallRcLocal()
{
  echo "uninstall agent service $service_name on rc.local"
  doUninstallRcLocal
  cd $workspace
  chmod +x *.sh
  ${workspace}/stop.sh
}

function uninstallSystemd()
{
  echo "uninstall agent service $service_name on systemd"
  # compatible with old data
  doUninstallRcLocal
  if systemctl list-unit-files | grep -q "^${service_name}"; then
    if systemctl is-active --quiet $service_name; then
      systemctl stop $service_name
    fi
    if systemctl is-enabled --quiet $service_name; then
      systemctl disable $service_name
    fi
    if systemctl is-failed --quiet $service_name; then
      systemctl reset-failed $service_name
    fi
  fi
  local SERVICE_FILE="/etc/systemd/system/${service_name}.service"
  if [ -f "$SERVICE_FILE" ]; then
    rm -f "$SERVICE_FILE"
  fi
}

function uninstallAgentService()
{
  echo "uninstall agent services $service_name to reinstall"
  if exists systemctl; then
    uninstallSystemd
  else
    uninstallRcLocal
  fi
  echo "service $service_name has been uninstalled"
}

function writeSSHConfig()
{
  echo "no need write ssh config"
}

# ----------------------------------

echo "start installing Agent..."
workspace=`pwd`
echo "install Dir: $workspace"
user=$USER
echo "install User: $user"
agent_id='##agentId##'
echo "AgentId: $agent_id"
enable_check_files='##enableCheckFiles##'
echo "EnableCheckFiles: $enable_check_files"

cd $workspace

checkFiles

echo "check if the install package(agent.zip) exists"
if [[ ! -f "agent.zip" ]]; then
  echo "install package does not exist. start download"
  initArch
  echo "download Agent arch: $ARCH"
  download_agent
  echo "unzip install package(agent.zip)"
  unzip -o agent.zip
else
  echo "agent.zip exist. reinstall, do nothing"
fi

unzip_jdk

os=`uname`
echo "OS: $os"
arch1=`uname -m`
echo "ARCH: $arch1"

if [ -d "./jdk17" ]; then
  echo "check java17 version"
  jdk17/bin/java -version
else
  echo "jdk17 folder is not exist"
fi

if [ -d "./jdk" ]; then
  echo "check java version"
  jdk/bin/java -version
else
  echo "jdk folder is not exist"
fi

echo "check if write ssh config"
writeSSHConfig

service_name=`getServiceName`

if [[ "$user" = "root" ]]; then
  echo "root, instll agent service, $service_name"
  uninstallAgentService
  installAgentService
else
  echo "no root, only start agent"
  cd $workspace
  chmod +x *.sh
  ${workspace}/start.sh
fi

#!/bin/bash
function exists()
{
  command -v "$1" >/dev/null 2>&1
}

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
}

function download_agent()
{
  print_zh "开始下载Agent安装包" "start download agent install package"
  if [[ -f "agent.zip" ]]; then
    print_zh "Agent安装包(agent.zip)已经存在，无需下载" "agent.zip already exist, skip download"
    return
  fi
  if exists curl; then
    print_zh "使用 curl 命令进行下载" "download using curl"
    curl -H "X-DEVOPS-PROJECT-ID: ##projectId##" -o agent.zip "##agent_url##"
    if [[ $? -ne 0 ]]; then
      print_zh "使用 curl 命令下载失败，尝试 wget 命令" "Fail to use curl to download the agent, use wget"
      wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##"
    fi
    elif exists wget; then
    print_zh "使用 wget 命令进行下载" "download using wget"
    wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##"
  else
    print_zh "curl 和 wget 命令都无法使用，下载失败" "curl & wget command don't exist, download fail"
    exit 1
  fi
}

function installRcLocal()
{
  print_zh "使用rc.local安装Agent相关服务" "install agent service with rc.local"
  grep_result=$(grep "$service_name" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
      print_zh "添加服务配置 $service_name 到rc.local" "add to rclocal"
      echo "cd $workspace && ./devopsDaemon & # $service_name" >> /etc/rc.d/rc.local
    else
      print_zh "rc.local中已经存在服务 $service_name，不再重复安装" "already add to rclocal"
    fi
  fi
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
}

function installSystemd()
{
  print_zh "使用Systemd安装的Agent相关服务" "install agent service with systemd"
  cat <<EOL > /etc/systemd/system/${service_name}.service
[Unit]
Description=bkdevops agent
After=network.target

[Service]
Type=forking
ExecStart=$workspace/start.sh
ExecStop=$workspace/stop.sh
WorkingDirectory=$workspace
PrivateTmp=true

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
  print_zh "Agent服务 $service_name 安装完成" "$service_name service has been installed"
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
  print_zh "卸载rc.local上的Agent相关服务" "uninstall agent service with rc.local"
  doUninstallRcLocal
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

function uninstallSystemd()
{
  print_zh "卸载Systemd上的Agent相关服务" "uninstall agent service with systemd"
  # 兼容旧数据
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
  print_zh "卸载可能存在的Agent相关服务，方便重新安装" "uninstall agent service"
  if exists systemctl; then
    uninstallSystemd
  else
    uninstallRcLocal
  fi
  print_zh "Agent服务 $service_name 卸载完成" "$service_name service has been uninstalled"
}

function writeSSHConfig()
{
  print_zh "无需配置SSH配置" "no need write ssh config"
}

# ----------------------------------

initLangSup
print_zh "开始安装蓝盾Agent" "start installing Agent..."
workspace=`pwd`
print_zh "安装目录: $workspace" "install Dir: $workspace"
user=$USER
print_zh "安装用户: $workspace" "install User: $user"
agent_id='##agentId##'
echo "AgentId: $agent_id"

cd $workspace

print_zh "检查Agent安装包(agent.zip)是否存在" "check if the install package(agent.zip) exists"
if [[ ! -f "agent.zip" ]]; then
  print_zh "安装包不存在，准备下载" "the install package does not exist. Start download"
  initArch
  print_zh "需要下载的Agent架构为: $ARCH" "download Agent arch: $ARCH"
  download_agent
  print_zh "开始解压缩安装包(agent.zip)" "unzip install package(agent.zip)"
  unzip -o agent.zip
fi

unzip_jdk

os=`uname`
print_zh "机器系统: $os" "OS: $os"
arch1=`uname -m`
print_zh "机器架构: $arch1" "ARCH: ${arch1}"
print_zh "检查jdk17版本" "check java17 version"
jdk17/bin/java -version
print_zh "检查jdk8版本" "check java version"
jdk/bin/java -version

print_zh "检查是否写入Agent相关SSH配置" "check and write ssh config"
writeSSHConfig

service_name=`getServiceName`

if [[ "$user" = "root" ]]; then
  print_zh "root用户，安装Agent系统服务: $service_name" "root, Agent service name $service_name"
  uninstallAgentService
  installAgentService
else
  print_zh "非root用户，直接启动Agent" "no root, start agent"
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
fi

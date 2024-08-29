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

function getServiceName()
{
  echo "devops_agent_"${agent_id}
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
  doUninstallRcLocal()
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

function uninstallSystemd()
{
  print_zh "卸载Systemd上的Agent相关服务" "uninstall agent service with systemd"
  # 兼容旧数据
  doUninstallRcLocal()
  systemctl stop $service_name
  systemctl disable $service_name
  local SERVICE_FILE="/etc/systemd/system/${service_name}.service"
  if [ -f "$SERVICE_FILE" ]; then
    rm "$SERVICE_FILE"
  fi
  systemctl daemon-reload
  systemctl reset-failed $service_name
}

function uninstallAgentService()
{
  print_zh "卸载可能存在的Agent相关服务" "uninstall agent service"
  if exists systemctl; then
    uninstallSystemd()
  else
    uninstallRcLocal()
  fi
  print_zh "Agent服务 $service_name 卸载完成" "$service_name service has been uninstalled"
}

# ----------------------------------

initLangSup
print_zh "开始卸载蓝盾Agent" "start uninstalling Agent..."
print_zh "开始安装蓝盾Agent" "start installing Agent..."
workspace=`pwd`
print_zh "安装目录: $workspace" "install Dir: $workspace"
user=$USER
print_zh "安装用户: $workspace" "install User: $user"
agent_id='##agentId##'
echo "AgentId: $agent_id"

service_name=`getServiceName`

if [[ "$user" = "root" ]]; then
  print_zh "root用户，卸载Agent系统服务: $service_name" "root, uninstall agent service name $service_name"
  uninstallAgentService
else
  print_zh "非root用户，直接停止Agent" "no root, stop agent"
  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
fi
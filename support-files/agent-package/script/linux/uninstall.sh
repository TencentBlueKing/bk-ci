#!/bin/bash
function exists()
{
  command -v "$1" >/dev/null 2>&1
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
  echo "uninstall agent service $service_name on rc.local"
  doUninstallRcLocal
  cd $workspace
  chmod +x *.sh
  ${workspace}/stop.sh
}

function uninstallSystemd()
{
  echo "uninstall agent service $service_name on systemd"
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
  echo "uninstall agent service"
  if exists systemctl; then
    uninstallSystemd
  else
    uninstallRcLocal
  fi
  echo "service $service_name has been uninstalled"
}

# ----------------------------------

echo "start uninstalling Agent..."
workspace=`pwd`
user=$USER
agent_id='##agentId##'
echo "AgentId: $agent_id"

service_name=`getServiceName`

if [[ "$user" = "root" ]]; then
  echo "root, uninstall agent service name $service_name"
  uninstallAgentService
else
  echo "no root, stop agent"
  cd $workspace
  chmod +x *.sh
  ${workspace}/stop.sh
fi
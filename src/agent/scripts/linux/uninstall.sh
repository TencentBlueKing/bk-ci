#!/bin/bash
echo "start uninstalling the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}
agent_id='##agentId##'

function getServiceName()
{
  echo "devops_agent_"${agent_id}
}

function uninstallAgentService()
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

service_name=`getServiceName`

if [[ "$user" = "root" ]]; then
    uninstallAgentService
else
    cd ${workspace}
    chmod +x *.sh
    ${workspace}/stop.sh
fi
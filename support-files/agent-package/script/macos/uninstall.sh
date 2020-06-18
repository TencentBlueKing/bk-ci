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
  if [[ "$user" != "root"  && -f ~/Library/LaunchAgents/$(getServiceName).plist ]]; then
    echo "remove run at load"
    rm -f ~/Library/LaunchAgents/$(getServiceName).plist
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

uninstallAgentService
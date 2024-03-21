#!/bin/bash
echo "start uninstalling the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}
function getAgentId()
{
  agentIdstr=$(cat $workspace/.agent.properties | grep devops.agent.id=)
  agent_id="${agentIdstr#*=}"
}

function getServiceName()
{
  echo "devops_agent_"${agent_id}
}

function uninstallAgentService()
{
  if [[ -f /Library/LaunchDaemons/$(getServiceName).plist ]]; then
    echo "stop and remove run at load"
    sudo launchctl unload /Library/LaunchDaemons/devops_agent_${agent_id}.plist
    rm -f /Library/LaunchDaemons/$(getServiceName).plist
  fi
}

getAgentId
uninstallAgentService
echo "uninstalling the agent done"

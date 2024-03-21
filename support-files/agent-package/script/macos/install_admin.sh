#!/bin/bash
echo "Start installing the agent..."
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

function addRunAtLoad()
{
  mkdir -p /Library/LaunchDaemons
  cat > /Library/LaunchDaemons/$(getServiceName).plist <<EOF
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
  if [[ -f /Library/LaunchDaemons/$(getServiceName).plist ]]; then
    echo "stop and remove run at load"
    sudo launchctl unload /Library/LaunchDaemons/devops_agent_${agent_id}.plist
    sudo rm -f /Library/LaunchDaemons/$(getServiceName).plist
  fi
}

function installAgentService()
{
  echo "add run at load with user ${user}"

  addRunAtLoad

  sudo launchctl load /Library/LaunchDaemons/devops_agent_${agent_id}.plist
}

cd ${workspace}

getAgentId
echo "agentid $agent_id"

uninstallAgentService
installAgentService
echo "install agent done"

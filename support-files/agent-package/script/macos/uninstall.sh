#!/bin/bash
echo "Start uninstalling the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}

function uninstall()
{
  if [[ "$user" != "root" && -f ~/Library/LaunchAgents/landun_devops_agent.plist ]]; then
    echo "remove run at load"
    rm -f ~/Library/LaunchAgents/landun_devops_agent.plist
  fi

  cd ${workspace}
  chmod +x ${workspace}/*.sh
  ${workspace}/stop.sh
}

uninstall
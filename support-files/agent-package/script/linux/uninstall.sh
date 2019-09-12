#!/bin/bash
echo "Start uninstalling the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}

function uninstall()
{
  echo "uninstall agent"
  grep_result=$(grep "devopsDaemon" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
        echo "already remove from rclocal"
    else
        sed -i '/devopsDaemon/d' "/etc/rc.d/rc.local"
        echo "remove from rclocal done"
    fi
  fi

  ${workspace}/stop.sh
}

if [[ "$user" = "root" ]]; then
    uninstall
else
    cd ${workspace}
    ${workspace}/stop.sh
fi
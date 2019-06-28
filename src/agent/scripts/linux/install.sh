#!/bin/bash
echo "Start installing the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}

function unzip_jre()
{
  echo "Unzipping the jre package"
  if [[ -d "jre" ]]; then
    echo "Cleaning jre folder"
    rm -rf jre
  fi
  unzip -q -o jre.zip -d jre
}

exists()
{
  command -v "$1" >/dev/null 2>&1
}

function download_agent()
{
  echo "Trying to download the agent install package"
  if [[ -f "agent.zip" ]]; then
    echo "agent.zip aleady exist, skip download"
    return
  fi
  if exists curl; then
    curl -H "X-DEVOPS-PROJECT-ID: ##projectId##" -o agent.zip '##agent_url##'
    if [[ $? -eq 0 ]]; then
      echo "Fail to use curl to download the agent, use wget"
      wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip '##agent_url##'
    fi
  elif exists wget; then
    wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip '##agent_url##'
  else
    echo "Curl & wget command don't exist, download fail"
    exit 1
  fi
}

function uninstallAgentService()
{
  echo "Uninstall agent service"
  grep_result=$(grep "devopsDaemon" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
        echo "already remove from rclocal"
    else
        sed -i '/devopsDaemon/d' "/etc/rc.d/rc.local"
        echo "removal done"
    fi
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

function installAgentService()
{
  echo "Install agent service with user ${user}"
  grep_result=$(grep "devopsDaemon" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
      echo "cd ${workspace} && ./devopsDaemon &" >> /etc/rc.d/rc.local
      echo "add to rclocal"
    else
      echo "already add to rclocal"
    fi
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
}

cd ${workspace}

download_agent

unzip -o agent.zip
unzip_jre

os=`uname`
echo "OS: $os"

echo "Check the java version"
jre/bin/java -version

if [[ "$user" = "root" ]]; then
    uninstallAgentService
    installAgentService
else
    cd ${workspace}
    chmod +x *.sh
    ${workspace}/start.sh
fi

#!/bin/bash
echo "Start installing the agent..."
t=`date +"%Y-%m-%d_%H-%M-%S"`
workspace=`pwd`
user=${USER}
agent_id='##agentId##'

function getServiceName()
{
  echo "devops_agent_"${agent_id}
}

function unzip_jre()
{
  echo "start unzipping jre package"
  if [[ -d "jre" ]]; then
    echo "jre already exists, skip unzip"
    return
  fi
  unzip -q -o jre.zip -d jre
}

exists()
{
  command -v "$1" >/dev/null 2>&1
}

function download_agent()
{
  echo "start download agent install package"
  if [[ -f "agent.zip" ]]; then
    echo "agent.zip already exist, skip download"
    return
  fi
  if exists curl; then
    curl -H "X-DEVOPS-PROJECT-ID: ##projectId##" -o agent.zip '##agent_url##'
    if [[ $? -ne 0 ]]; then
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

function installAgentService()
{
  echo "install agent service with user ${user}"
  grep_result=$(grep "${service_name}" /etc/rc.d/rc.local)
  if test -x "/etc/rc.d/rc.local" ; then
    if [[ -z "$grep_result" ]]; then
      echo "cd ${workspace} && ./devopsDaemon & # ${service_name}" >> /etc/rc.d/rc.local
      echo "add to rclocal"
    else
      echo "already add to rclocal"
    fi
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
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

function writeSSHConfig()
{
}

# if [[ "${workspace}" = ~ ]]; then
#  echo 'agent should not install in root of user home directory'
#  echo 'please run install script in an empty directory with full permission'
#  exit 1
# fi

cd ${workspace}

if [[ ! -f "agent.zip" ]]; then
  download_agent
  unzip -o agent.zip
fi

unzip_jre

os=`uname`
echo "OS: $os"

echo "check java version"
jre/bin/java -version

echo "check and write ssh config"
writeSSHConfig

service_name=`getServiceName`

if [[ "$user" = "root" ]]; then
    uninstallAgentService
    installAgentService
else
    cd ${workspace}
    chmod +x *.sh
    ${workspace}/start.sh
fi

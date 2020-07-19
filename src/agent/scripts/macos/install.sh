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
  echo "start unzipping the jre package"
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
      echo "fail to use curl to download the agent, use wget"
      wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip '##agent_url##'
    fi
  elif exists wget; then
    wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip '##agent_url##'
  else
    echo "curl & wget command don't exist, download fail"
    exit 1
  fi
}

function addRunAtLoad()
{
  mkdir -p ~/Library/LaunchAgents
  cat > ~/Library/LaunchAgents/$(getServiceName).plist <<EOF
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
  if [[ "$user" != "root"  && -f ~/Library/LaunchAgents/$(getServiceName).plist ]]; then
    echo "remove run at load"
    rm -f ~/Library/LaunchAgents/$(getServiceName).plist
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/stop.sh
}

function installAgentService()
{
  if [[ "$user" != "root" ]]; then
    echo "add run at load with user ${user}"
    addRunAtLoad
  fi

  cd ${workspace}
  chmod +x *.sh
  ${workspace}/start.sh
}

function writeSSHConfig()
{
}

# if [[ "${workspace}" = ~ ]]; then
#   echo 'agent should not install in root of user home directory'
#   echo 'please run install script in an empty directory with full permission'
#   exit 1
# fi

cd ${workspace}

download_agent
unzip -o agent.zip
unzip_jre

os=`uname`
echo "OS: $os"

echo "check java version"
jre/Contents/Home/bin/java -version

echo "check and write ssh config"
writeSSHConfig

uninstallAgentService
installAgentService




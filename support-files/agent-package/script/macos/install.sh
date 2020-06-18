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
  if [[ -f  "agent.zip" ]]; then
    echo "agent.zip aleady exist, skip download"
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

function addRunAtLoad()
{
  mkdir -p ~/Library/LaunchAgents
  cat > ~/Library/LaunchAgents/landun_devops_agent.plist <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple Computer//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
        <string>landun_devops_agent</string>

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
  if [[ "$user" != "root" ]]; then
    echo "remove run at load"
    rm -f ~/Library/LaunchAgents/landun_devops_agent.plist
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

cd ${workspace}

download_agent

unzip -o agent.zip
unzip_jre

os=`uname`
echo "OS: $os"

echo "Check the java version"
jre/Contents/Home/bin/java -version

uninstallAgentService
installAgentService


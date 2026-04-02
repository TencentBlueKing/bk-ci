#!/bin/bash
function checkFiles()
{
  if [[ "$enable_check_files" == "true" ]]; then
    # 检查当前目录是否有文件
    if [ "$(find . -maxdepth 1 -type f | wc -l)" -gt 0 ]; then
      echo "fatal: current directory is not empty, please install in an empty directory"
      exit 1
    fi
  fi
}

function exists()
{
  command -v "$1" >/dev/null 2>&1
}

function initArch() {
  ARCH=$(uname -m)
  case $ARCH in
    aarch64) ARCH="arm64";;
    arm64) ARCH="arm64";;
    mips64) ARCH="mips64";;
    *) ARCH="";;
  esac
}

function unzip_jdk()
{
  if [ -f "./jdk17.zip" ]; then
    echo "start unzipping jdk17(jdk17.zip) package"
    if [[ -d "jdk17" ]]; then
      echo "jdk17 already exists, skip unzip"
    else
      unzip -q -o jdk17.zip -d jdk17
    fi
  else
    echo "'jdk17.zip' is not exist"
  fi

  if [ -f "./jre.zip" ]; then
    echo "start unzipping jdk(jre.zip) package"
    if [[ -d "jdk" ]]; then
      echo "jdk already exists, skip unzip"
    else
      unzip -q -o jre.zip -d jdk
    fi
  else
    echo "'jre.zip' is not exist"
  fi
}

function download_agent()
{
  echo "start download agent install package"
  if [[ -f "agent.zip" ]]; then
    echo "agent.zip already exist, skip download"
    return
  fi
  local download_ok=false
  if exists curl; then
    echo "download using curl"
    local http_code
    http_code=$(curl -sS -w "%{http_code}" -H "X-DEVOPS-PROJECT-ID: ##projectId##" -o agent.zip "##agent_url##")
    if [[ $? -ne 0 ]] || [[ "$http_code" -lt 200 ]] || [[ "$http_code" -ge 400 ]]; then
      echo "curl download failed (HTTP $http_code), response body:"
      cat agent.zip 2>/dev/null
      echo ""
      rm -f agent.zip
      if exists wget; then
        echo "retrying with wget..."
        wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##" 2>&1
        if [[ $? -eq 0 ]]; then
          download_ok=true
        fi
      fi
    else
      download_ok=true
    fi
  elif exists wget; then
    echo "download using wget"
    wget --header="X-DEVOPS-PROJECT-ID: ##projectId##" -O agent.zip "##agent_url##" 2>&1
    if [[ $? -eq 0 ]]; then
      download_ok=true
    fi
  else
    echo "curl & wget command don't exist, download fail"
    exit 1
  fi

  if [[ "$download_ok" != "true" ]]; then
    echo "download agent.zip failed, response body:"
    cat agent.zip 2>/dev/null
    echo ""
    rm -f agent.zip
    exit 1
  fi

  # verify the file is a valid zip (not an HTML/JSON error page)
  if ! unzip -t agent.zip >/dev/null 2>&1; then
    echo "downloaded file is not a valid zip, server may have returned an error:"
    cat agent.zip
    echo ""
    rm -f agent.zip
    exit 1
  fi
}

function writeSSHConfig()
{
  echo "no need write ssh config"
}

# ----------------------------------

echo "start installing Agent..."
workspace=`pwd`
echo "install Dir: $workspace"
user=$USER
echo "install User: $user"
agent_id='##agentId##'
echo "AgentId: $agent_id"
enable_check_files='##enableCheckFiles##'
echo "EnableCheckFiles: $enable_check_files"

cd $workspace

checkFiles

echo "check if the install package(agent.zip) exists"
if [[ ! -f "agent.zip" ]]; then
  echo "install package does not exist. start download"
  initArch
  echo "download Agent arch: $ARCH"
  download_agent
  echo "unzip install package(agent.zip)"
  unzip -o agent.zip
else
  echo "agent.zip exist. reinstall, do nothing"
fi

unzip_jdk

os=`uname`
echo "OS: $os"
arch1=`uname -m`
echo "ARCH: $arch1"

if [ -d "./jdk17" ]; then
  echo "check java17 version"
  jdk17/bin/java -version
else
  echo "jdk17 folder is not exist"
fi

if [ -d "./jdk" ]; then
  echo "check java version"
  jdk/bin/java -version
else
  echo "jdk folder is not exist"
fi

echo "check if write ssh config"
writeSSHConfig

cd $workspace
chmod +x devopsAgent devopsDaemon 2>/dev/null

echo "installing agent service via CLI..."
./devopsAgent install

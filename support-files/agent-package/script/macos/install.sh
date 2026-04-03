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

function initArch()
{
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

function exists()
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
  if ! exists curl; then
    echo "no curl command, download fail"
    exit 1
  fi

  echo "GET -H 'X-DEVOPS-PROJECT-ID: ##projectId##' ##agent_url##"

  local http_code
  http_code=$(curl -v --show-error \
    -H "X-DEVOPS-PROJECT-ID: ##projectId##" \
    -o agent.zip \
    -w '%{http_code}' \
    "##agent_url##")
  local curl_exit=$?

  echo ""
  echo "--- download result ---"
  echo "HTTP Status: $http_code"

  if [[ $curl_exit -ne 0 ]]; then
    echo "curl exit code: $curl_exit"
    rm -f agent.zip
    exit 1
  fi

  if [[ "$http_code" -ge 400 ]]; then
    echo "server response body:"
    cat agent.zip 2>/dev/null
    echo ""
    rm -f agent.zip
    exit 1
  fi

  local file_size
  file_size=$(stat -c%s agent.zip 2>/dev/null || stat -f%z agent.zip 2>/dev/null || echo "unknown")
  echo "file size: ${file_size} bytes"
  echo "download OK"
}

function writeSSHConfig()
{
  echo "no need write ssh config"
}

# ----------------------------------

echo "start installing the agent..."
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

initArch
download_agent

echo "unzip install package(agent.zip)"
unzip -o agent.zip

unzip_jdk

os=`uname`
echo "OS: $os"
arch1=`uname -m`
echo "ARCH: $arch1"

if [ -d "./jdk17" ]; then
  echo "check java17 version"
  jdk17/Contents/Home/bin/java -version
else
  echo "jdk17 folder is not exist"
fi

if [ -d "./jdk" ]; then
  echo "check java version"
  jdk/Contents/Home/bin/java -version
else
  echo "jdk folder is not exist"
fi

echo "check if write ssh config"
writeSSHConfig

cd $workspace
chmod +x devopsAgent devopsDaemon 2>/dev/null

echo "installing agent service via CLI..."
./devopsAgent install

#!/bin/bash

usage () {
    cat <<EOF
用法:
    $PROGRAM [OPTIONS]...
            安装turbo-linux-client工具包

            [ --overwrite           [可选] 覆盖安装 ]
            [ -h, --help            [可选] 查看脚本帮助 ]
EOF
}

# Color
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

function red() {
    printf "${RED}$@${NC}"
}

function green() {
    printf "${GREEN}$@${NC}"
}

function yellow() {
    printf "${YELLOW}$@${NC}"
}

usage_and_exit () {
    usage
    exit "$1"
}

# configurations
TBS_SERVER='__TBS_HOST__'
DOWNLOAD_SERVER='__TBS_HOST__'
OVERWRITE=''
SKIP_INSTALL=''

# parse flags
while (( $# > 0 )); do
    case "$1" in
        -r | --region )
            shift
            ;;
        --overwrite )
            OVERWRITE="1"
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "不可识别的参数: $1"
            ;;
        *)
            ;;
    esac
    shift
done

function download_pack()
{
    echo "`yellow [WORKING]` downloading turbo-client"
    if [ -f /tmp/linux-turbo-client.tgz ]; then
        rm /tmp/linux-turbo-client.tgz
    fi
    wget -O /tmp/linux-turbo-client.tgz "$DOWNLOAD_SERVER/downloads/clients/linux-turbo-client.tgz"

    if [[ $? -ne 0 ]]; then
        echo "`red [FAILURE]` download turbo-client via wget failed"
        exit 1
    fi
    echo "`green [SUCCESS]` downloaded turbo-client"
}

function decompress_pack()
{
    echo "`yellow [WORKING]` decompressing turbo-client"
    if [ -d "/tmp/linux-turbo-client" ]; then
        rm -r /tmp/linux-turbo-client
    fi

    mkdir /tmp/linux-turbo-client
    if [[ $? -ne 0 ]]; then
        echo "`red [FAILURE]` make turbo-client decompress dir /tmp/linux-turbo-client failed"
        exit 1
    fi

    tar -zxf /tmp/linux-turbo-client.tgz -C /tmp/linux-turbo-client --strip-components=1
    if [[ $? -ne 0 ]]; then
        echo "`red [FAILURE]` decompress turbo-client via tar failed"
        exit 1
    fi
    echo "`green [SUCCESS]` decompressed turbo-client"
}

function install()
{
    echo "`yellow [WORKING]` installing turbo-client"
    cd /tmp/linux-turbo-client && sh install.sh
    if [[ $? -ne 0 ]]; then
        echo "`red [FAILURE]` install turbo-client failed"
        exit 1
    fi
    echo "`green [SUCCESS]` installed turbo-client"
}

# check if overwrite
type bk-booster >/dev/null 2>&1 && {
    if [[ -z $OVERWRITE ]]; then
        echo "`yellow [WARNING]` bk-booster already exist, run install with --overwrite if you want to update it"
        SKIP_INSTALL='1'
    fi
}

# check user
user=`whoami`
if [ "${user}" != "root" ];then
    echo "`red [FAILURE]` need root to install this tool, please su to root"
    exit 1
fi

if [[ -z "$SKIP_INSTALL" ]]; then
    download_pack
    decompress_pack
    install
fi

if [[ ! -z "$TBS_SERVER" ]]; then
    echo "{\"server\":\"$TBS_SERVER/server/\"}"  > /etc/bk_dist/config.json
fi

bk-booster --version
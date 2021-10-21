#!/usr/bin/env bash
# 用途：构建并推送charts包

# 安全模式
set -euo pipefail

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

VERSION=1.0.2
APP_VERSION=latest
PUSH=0
REGISTRY=http://localhost/helm
USERNAME=
PASSWORD=

cd $(dirname $0)
WORKING_DIR=$(pwd)

usage () {
    cat <<EOF
用法:
    $PROGRAM [OPTIONS]...

            [ -v, --version         [可选] charts版本, 默认1.0.0 ]
            [ -a, --app-version     [可选] app版本, 默认latest ]
            [ -p, --push            [可选] 推送charts包到helm远程仓库，默认不推送 ]
            [ -r, --registry        [可选] helm chars仓库地址, 默认http://localhost/helm ]
            [ --username            [可选] helm chars仓库用户名 ]
            [ --password            [可选] helm chars仓库密码 ]
            [ -h, --help            [可选] 查看脚本帮助 ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# 解析命令行参数，长短混合模式
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do
    case "$1" in
        -v | --version )
            shift
            VERSION=$1
            ;;
        -a | --app-version )
            shift
            APP_VERSION=$1
            ;;
        -p | --push )
            PUSH=1
            ;;
        -r | --registry )
            shift
            REGISTRY=$1
            ;;
        --username )
            shift
            USERNAME=$1
            ;;
        --password )
            shift
            PASSWORD=$1
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "不可识别的参数: $1"
            ;;
        *)
            break
            ;;
    esac
    shift
done

#替换values.yaml中的${app_version}
sed -i  "s/\${app_version}/$APP_VERSION/g" codecc/values.yaml

helm package codecc --version $VERSION --app-version $APP_VERSION
if [[ $PUSH -eq 1 ]] ; then
    helm push codecc-$VERSION.tgz $REGISTRY -f --username $USERNAME --password $PASSWORD
fi
log "BUILD SUCCESSFUL!"

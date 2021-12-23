#!/usr/bin/env bash
# 用途：构建并推送docker镜像

# 安全模式
set -euo pipefail 

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

ALL=1
GATEWAY=0
BACKEND=0
INIT=0
VERSION=latest
PUSH=0
REGISTRY=docker.io
USERNAME=
PASSWORD=
BACKENDS=(task defect apiquery codeccjob openapi)
DEFECT_BACKENDS=(defect report opensourcereport asyncreport)

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend/codecc
FRONTEND_DIR=$ROOT_DIR/src/frontend
GATEWAY_DIR=$ROOT_DIR/support-files/kubernetes/images/gateway/conf

usage () {
    cat <<EOF
用法: 
    $PROGRAM [OPTIONS]... 

            [ --gateway             [可选] 打包gateway镜像 ]
            [ --backend             [可选] 打包backend镜像 ]
            [ --init                [可选] 打包init镜像 ]
            [ -v, --version         [可选] 镜像版本tag, 默认latest ]
            [ -p, --push            [可选] 推送镜像到docker远程仓库，默认不推送 ]
            [ -r, --registry        [可选] docker仓库地址, 默认docker.io ]
            [ --username            [可选] docker仓库用户名 ]
            [ --password            [可选] docker仓库密码 ]
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
        --gateway )
            ALL=0
            GATEWAY=1
            ;;
        --backend )
            ALL=0
            BACKEND=1
            ;;
        --init )
            ALL=0
            INIT=1
            ;;
        -v | --version )
            shift
            VERSION=$1
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

if [[ $PUSH -eq 1 && -n "$USERNAME" ]] ; then
    docker login --username $USERNAME --password $PASSWORD $REGISTRY
    log "docker login成功"
fi

# 创建临时目录
mkdir -p $WORKING_DIR/tmp
tmp_dir=$WORKING_DIR/tmp
# 执行退出时自动清理tmp目录
trap 'rm -rf $tmp_dir' EXIT TERM

# 编译frontend
if [[ $ALL -eq 1 || $GATEWAY -eq 1 ]] ; then
    log "编译frontend..."
    cd $FRONTEND_DIR
    npm i --registry http://mirrors.tencent.com/npm/ && npm run build
    # 打包gateway镜像
    log "构建gateway镜像..."
    cd $WORKING_DIR
    rm -rf tmp/*
    cp -rf $FRONTEND_DIR/dist tmp/frontend
    cp -rf $GATEWAY_DIR tmp/gateway
    cp -rf gateway/startup.sh tmp/
    cp -rf $ROOT_DIR/scripts/render_tpl tmp/
    cp -rf $ROOT_DIR/scripts/codecc.env tmp/
    cp -rf $ROOT_DIR/support-files/templates tmp/
    docker build -f gateway/gateway.Dockerfile -t $REGISTRY/bkce/codecc/gateway:$VERSION tmp --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/bkce/codecc/gateway:$VERSION
    fi
fi

# 构建backend镜像
if [[ $ALL -eq 1 || $BACKEND -eq 1 ]] ; then
    chmod +x $BACKEND_DIR/gradlew
    log "构建镜像..."
    $BACKEND_DIR/gradlew -p $BACKEND_DIR build -Pdevops.assemblyMode=k8s -x test
    for SERVICE in ${BACKENDS[@]};
    do
        if [[ "${SERVICE}" = "defect" ]] ; then
            for SERVICE_NAME in ${DEFECT_BACKENDS[@]};
            do
                rm -rf tmp/*
                cp backend/startup.sh tmp/
                cp $BACKEND_DIR/release/boot-$SERVICE.jar tmp/app.jar
                docker build -f backend/backend.Dockerfile -t $REGISTRY/bkce/codecc/$SERVICE_NAME:$VERSION tmp --network=host
                if [[ $PUSH -eq 1 ]] ; then
                    docker push $REGISTRY/bkce/codecc/$SERVICE_NAME:$VERSION
                fi
            done
        else
            rm -rf tmp/*
            cp backend/startup.sh tmp/
            cp $BACKEND_DIR/release/boot-$SERVICE.jar tmp/app.jar
            docker build -f backend/backend.Dockerfile -t $REGISTRY/bkce/codecc/$SERVICE:$VERSION tmp --network=host
            if [[ $PUSH -eq 1 ]] ; then
                docker push $REGISTRY/bkce/codecc/$SERVICE:$VERSION
            fi            
        fi
    done
fi

# 构建init镜像
if [[ $ALL -eq 1 || $INIT -eq 1 ]] ; then
    log "构建init镜像..."
    rm -rf tmp/*
    cp -rf mongodb/init-mongodb.sh tmp/
    cp -rf mongodb/nosql tmp/
    docker build -f mongodb/init.Dockerfile -t $REGISTRY/bkce/codecc/codecc-init:$VERSION tmp --no-cache --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/bkce/codecc/codecc-init:$VERSION
    fi
fi
echo "BUILD SUCCESSFUL!"

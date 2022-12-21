#!/usr/bin/env bash
# 用途：构建并推送docker镜像

# 安全模式
set -euo pipefail

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

ALL=1
SERVER=0
GATEWAY=0
DASHBOARD=0
DOWNLOADER=0
WORKER=0
VERSION=latest
PUSH=0
REGISTRY=docker.io
BASEIMAGE=
IMAGE=
ENGINE=disttask
USERNAME=
PASSWORD=

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*}

usage () {
    cat <<EOF
用法:
    $PROGRAM [OPTIONS]...

            [ --server              [可选] 打包server镜像 ]
            [ --gateway             [可选] 打包gateway镜像 ]
            [ --dashboard           [可选] 打包dashboard镜像 ]
            [ --downloader          [可选] 打包downloader镜像 ]
            [ --worker              [可选] 打包worker镜像 ]
            [ -b --baseimage        [可选] worker镜像的基础镜像，打包worker时必选 ]
            [ -i --image            [可选] worker镜像的名称，打包worker时必选 ]
            [ -e --engine           [可选] worker镜像对应的engine类型，默认disttask ]
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
        --server )
            ALL=0
            SERVER=1
            ;;
        --gateway )
            ALL=0
            GATEWAY=1
            ;;
        --dashboard )
            ALL=0
            DASHBOARD=1
            ;;
        --downloader )
            ALL=0
            DOWNLOADER=1
            ;;
        --worker )
            ALL=0
            WORKER=1
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
        -b | --baseimage )
            shift
            BASEIMAGE=$1
            ;;
        -i | --image )
            shift
            IMAGE=$1
            ;;
        -e | --engine )
            shift
            ENGINE=$1
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

# 编译
log "编译service..."
cd $ROOT_DIR
export GO111MODULE=on
#export GOPATH=${ROOT_DIR%/*/*}
export PATH=$GOPATH/bin:$PATH
GOOS=linux GOARCH=amd64 disable_encrypt=1 enable_bcs_gateway=1 booster_server_necessary=1 make -j buildbooster dist
cd $WORKING_DIR

# 构建server镜像
if [[ $ALL -eq 1 || $SERVER -eq 1 ]] ; then
    log "构建server镜像..."
    rm -rf tmp/*
    cp -rf server/* tmp/
    cp -rf $ROOT_DIR/build/buildbooster/bk-buildbooster-server tmp/bk-tbs-server
    docker build -f tmp/Dockerfile -t $REGISTRY/bktbs-server:$VERSION tmp --no-cache --network=host
fi

# 构建gateway镜像
if [[ $ALL -eq 1 || $GATEWAY -eq 1 ]] ; then
    log "构建gateway镜像..."
    rm -rf tmp/*
    cp -rf gateway/* tmp/
    cp -rf $ROOT_DIR/build/buildbooster/bk-buildbooster-gateway tmp/bk-tbs-gateway
    docker build -f tmp/Dockerfile -t $REGISTRY/bktbs-gateway:$VERSION tmp --no-cache --network=host
fi

# 构建dashboard镜像
if [[ $ALL -eq 1 || $DASHBOARD -eq 1 ]] ; then
    log "构建dashboard镜像..."
    rm -rf tmp/*
    cp -rf dashboard/* tmp/
    cp -rf $ROOT_DIR/build/buildbooster/bk-buildbooster-dashboard tmp/bk-tbs-dashboard
    docker build -f tmp/Dockerfile -t $REGISTRY/bktbs-dashboard:$VERSION tmp --no-cache --network=host
fi

# 构建downloader镜像
if [[ $ALL -eq 1 || $DOWNLOADER -eq 1 ]] ; then
    log "构建downloader镜像..."
    rm -rf tmp/*
    cp -rf downloader/* tmp/
    mkdir -p tmp/linux-turbo-client
    cp -rf $ROOT_DIR/build/bkdist/bk-booster tmp/linux-turbo-client/
    cp -rf $ROOT_DIR/build/bkdist/bk-dist-controller tmp/linux-turbo-client/
    cp -rf $ROOT_DIR/build/bkdist/bk-dist-executor tmp/linux-turbo-client/
    cp -rf $ROOT_DIR/build/bkdist/bk-dist-worker tmp/linux-turbo-client/
    cp -rf $ROOT_DIR/build/bkdist/bkhook.so tmp/linux-turbo-client/bk-hook.so
    cp -rf $ROOT_DIR/bk_dist/install/* tmp/linux-turbo-client/
    chmod +x tmp/linux-turbo-client/* tmp/linux-turbo-client/launcher/*
    cd tmp && tar -zcf linux-turbo-client.tgz linux-turbo-client && cd -
    docker build -f tmp/Dockerfile -t $REGISTRY/bktbs-downloader:$VERSION tmp --no-cache --network=host
fi

# 构建worker镜像
if [[ $WORKER -eq 1 ]] ; then
    log "构建worker镜像..."
    rm -rf tmp/*
    mkdir -p tmp/worker
    cp -rf worker/* tmp/worker
    cp -rf $ROOT_DIR/build/bkdist/bk-dist-worker tmp/worker
    if [[ $ENGINE = 'distcc' ]] ; then
        echo 'docker build -f tmp/worker/distcc/Dockerfile --build-arg BASE_IMAGE=$BASEIMAGE -t $REGISTRY/$IMAGE:$VERSION tmp/worker --no-cache --network=host'
        docker build -f tmp/worker/distcc/Dockerfile --build-arg BASE_IMAGE=$BASEIMAGE -t $REGISTRY/$IMAGE:$VERSION tmp/worker --no-cache --network=host
    else
        echo 'docker build -f tmp/worker/Dockerfile --build-arg BASE_IMAGE=$BASEIMAGE -t $REGISTRY/$IMAGE:$VERSION tmp/worker --no-cache --network=host'
        docker build -f tmp/worker/Dockerfile --build-arg BASE_IMAGE=$BASEIMAGE -t $REGISTRY/$IMAGE:$VERSION tmp/worker --no-cache --network=host
    fi
fi

echo "BUILD SUCCESSFUL!"

if [[ $PUSH -eq 1 ]]; then
    log "推送镜像到docker远程仓库"
    if [[ $ALL -eq 1 || $SERVER -eq 1 ]] ; then
        docker push $REGISTRY/bktbs-server:$VERSION
    fi

    if [[ $ALL -eq 1 || $GATEWAY -eq 1 ]] ; then
        docker push $REGISTRY/bktbs-gateway:$VERSION
    fi

    if [[ $ALL -eq 1 || $DASHBOARD -eq 1 ]] ; then
        docker push $REGISTRY/bktbs-dashboard:$VERSION
    fi

    if [[ $ALL -eq 1 || $DOWNLOADER -eq 1 ]] ; then
        docker push $REGISTRY/bktbs-downloader:$VERSION
    fi

    if [[ $WORKER -eq 1 ]] ; then
        docker push $REGISTRY/$IMAGE:$VERSION
    fi
fi
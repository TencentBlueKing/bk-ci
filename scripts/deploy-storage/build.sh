#!/usr/bin/env bash
# 用途：编译构建bkrepo项目

# 安全模式
set -euo pipefail 

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

TEST=0

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend
GATEWAY_DIR=$ROOT_DIR/src/gateway
TEMPLATES_DIR=$ROOT_DIR/support-files/templates

usage () {
    cat <<EOF
用法: 
    $PROGRAM [OPTIONS]... 

            [ -t, --test            [可选] 是否执行测试用例，默认false ]
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
        -t | --test )
            TEST=1
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

# 创建临时目录
mkdir -p $ROOT_DIR/tmp
tmp_dir=$ROOT_DIR/tmp
# 执行退出时自动清理tmp目录
trap 'rm -rf $tmp_dir' EXIT TERM

log "编译frontend..."
yarn --cwd $FRONTEND_DIR install
yarn --cwd $FRONTEND_DIR run public

log "编译backend..."
if [[ $TEST -eq 1 ]]; then
  $BACKEND_DIR/gradlew -p $BACKEND_DIR
else
  $BACKEND_DIR/gradlew -p $BACKEND_DIR -x test
fi

log "拷贝frontend..."
cp -rf $FRONTEND_DIR/frontend $tmp_dir

log "拷贝backend..."
mkdir -p $tmp_dir/backend
for file in $BACKEND_DIR/release/boot-*.jar; do
  service_name=$(basename $file | awk -F'[-.]' '{print $2}')
  cp $file $tmp_dir/backend/boot-$service_name.jar
done

log "拷贝templates..."
cp -rf $TEMPLATES_DIR $tmp_dir

log "拷贝scripts..."
cp -rf $WORKING_DIR $tmp_dir

log "拷贝gateway..."
cp -rf $GATEWAY_DIR $tmp_dir

log "打包bkrepo.tar.gz..."
# 创建bin目录
mkdir -p $ROOT_DIR/bin
cd $tmp_dir
tar -zcf $ROOT_DIR/bin/bkrepo.tar.gz *

log "Success! 文件保存到$ROOT_DIR/bin/bkrepo.tar.gz"

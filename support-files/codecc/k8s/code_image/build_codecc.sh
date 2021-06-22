#!/bin/bash
set -e
echo "导入环境变量开始..."

SOURCE_PACKAGE="codecc"
TMP_PACKAGE="tmp_codecc"
IMAGE_VERSION=1.0
hub=__BK_CODECC_K8S_HUB__

mkdir -p $TMP_PACKAGE && rm -rf $TMP_PACKAGE/*

echo "package path: "`pwd`
echo "导入环境变量完成"
echo "docker push hub:"$hub
echo "docker push image versio:"$IMAGE_VERSION

##打包gateway镜像
echo "打包gateway镜像开始..."
rm -rf ${TMP_PACKAGE}/*
mkdir $TMP_PACKAGE/frontend
cp -rf ${SOURCE_PACKAGE}/frontend $TMP_PACKAGE/frontend
cp -rf ${SOURCE_PACKAGE}/gateway $TMP_PACKAGE/gateway
cp -rf codecc_k8s/code_image/gateway/gateway_run_codecc.sh $TMP_PACKAGE/
cp -rf codecc_k8s/base/codecc_render_tpl $TMP_PACKAGE/
mkdir $TMP_PACKAGE/support-files
cp -rf ${SOURCE_PACKAGE}/support-files/* $TMP_PACKAGE/support-files
docker build -f codecc_k8s/code_image/gateway/gateway_codecc.Dockerfile -t $hub/bkci-codecc-gateway:$IMAGE_VERSION ./$TMP_PACKAGE --network=host
docker push $hub/bkci-codecc-gateway:$IMAGE_VERSION
echo "打包gateway镜像完成"

## 打包backend镜像
echo "打包backend镜像开始..."
backends=(task defect report asyncreport codeccjob schedule openapi apiquery quartz)
for var in ${backends[@]};
do
    echo "build $var start..."

    rm -rf $TMP_PACKAGE/*
    cp -r codecc_k8s/code_image/backend/classpath $TMP_PACKAGE/
    cp -r codecc_k8s/code_image/backend/bootstrap $TMP_PACKAGE/
    cp -r codecc_k8s/code_image/backend/font $TMP_PACKAGE/
    cp ${SOURCE_PACKAGE}/$var/boot-$var.jar $TMP_PACKAGE/

    if [ $var == "report" ]
    then
      echo 'package report image'
      cp codecc_k8s/code_image/backend/module_run_codecc_report.sh $TMP_PACKAGE/
    elif [ $var == "asyncreport" ]
    then
      echo 'package async report image'
      cp codecc_k8s/code_image/backend/module_run_codecc_asyncreport.sh $TMP_PACKAGE/
    else
      cp codecc_k8s/code_image/backend/module_run_codecc.sh $TMP_PACKAGE/
    fi

    docker build -f codecc_k8s/code_image/backend/$var.Dockerfile -t $hub/bkci-codecc-$var:$IMAGE_VERSION $TMP_PACKAGE --network=host
    docker push $hub/bkci-codecc-$var:$IMAGE_VERSION
    echo "build $var finish..."
done

## 打包配置镜像
echo '打包配置镜像中...'
rm -rf $TMP_PACKAGE/*
mkdir $TMP_PACKAGE/support-files
cp -rf ${SOURCE_PACKAGE}/support-files/codecc/* $TMP_PACKAGE/support-files
cp -rf codecc_k8s/code_image/configuration/import_config_codecc.sh $TMP_PACKAGE/
cp -rf codecc_k8s/base/codecc_render_tpl $TMP_PACKAGE/
cp -rf codecc_k8s/code_image/configuration/mongodb-org-4.0.repo $TMP_PACKAGE/
docker build -f codecc_k8s/code_image/configuration/configuration_codecc.Dockerfile -t $hub/bkci-codecc-configuration:$IMAGE_VERSION $TMP_PACKAGE --network=host
docker push $hub/bkci-codecc-configuration:$IMAGE_VERSION
echo '打包配置镜像完成'

set +e
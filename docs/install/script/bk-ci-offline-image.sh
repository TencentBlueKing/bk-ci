#!/bin/bash
# 创建目录
mkdir -p tar
# 服务镜像
images=($(helm template --debug bk-ci-charts.tgz | grep 'image: ' | awk -F ': ' '{print $2}' | sort | uniq))
for image in "${images[@]}"; do
  echo "image: $image"
  echo "download start..."
  docker pull $image
  echo "tar start..."
  tar_name=$(echo $image | awk -F '/' '{print $NF}')
  docker save $image >tar/$tar_name.tar
  echo "=============================================================================================="
done
# 默认镜像
image="bkci/ci:latest"
echo "image: $image"
echo "download start..."
docker pull $image
echo "tar start..."
tar_name=$(echo $image | awk -F '/' '{print $NF}')
docker save $image >tar/$tar_name.tar
echo "=============================================================================================="

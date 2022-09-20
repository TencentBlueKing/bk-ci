## BK-CI
此镜像包含了BK-CI的网关镜像和后端服务镜像

## 前置条件
- 从[Github Release](https://github.com/Tencent/bk-ci/releases)上下载bkci-slim.tar.gz到*bk-ci*项目目录下
- 执行 `./0.get_release.sh` 初始化镜像所需要的目录

## 镜像生成
- 网关镜像: `./1.build_gateway_image.sh ${DOCKER_IMAGE_REGISTRY} ${GATEWAY_DOCKER_IMAGE_VERSION}`
- 后端服务镜像 : `./2.build_backend_bkci_image.sh ${DOCKER_IMAGE_REGISTRY} ${BACKEND_DOCKER_IMAGE_VERSION}`

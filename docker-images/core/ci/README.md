## BK-CI
此镜像包含了BK-CI的网关镜像和后端服务镜像

## 依赖
1. 需要在项目目录下生成`bkci-slim.tar.gz` , 然后运行`./0.get_release.sh`生成镜像依赖的目录
2. 需要在mirrors.tencent.com/bkce 上有可上传镜像的账户 , 并在本地配置

## 镜像生成
- 网关镜像: `./1.build_gateway_image.sh ${GATEWAY_DOCKER_IMAGE_VERSION}`
- 后端服务镜像 : `./2.build_backend_bkci_image.sh ${BACKEND_DOCKER_IMAGE_VERSION}`

# bk-ci 前端部署文档

蓝鲸ci前端（frontend目录下）, 共有8个子目录，其中common-lib和svg-sprites为项目依赖的静态资源，其余6个目录均为vue的spa工程，其中devops-nav为主入口，其它子服务以iframe或amd的方式接入

## 系统要求

nodejs版本 8.0.0及以上


## 安装说明

- 1、打包并部署相应的vue工程
进入到src/frontend目录下
```
# 先全局安装yarn
npm install -g yarn
# 然后执行install
yarn install
# 然后安装每个子服务的依赖
yarn start
# 最后执行打包命令
yarn public
```

执行完这两条命令后，会在src/frontend目录下生成一个frontend的文件夹，里面是BK-CI前端打包后生成的资源文件

    每个前端服务模块与文件夹对应关系如下：

|   文件夹名称   |   模块名称     |
| ------------ | ---------------- |
|  console |   devops-nav
|  pipeline |   devops-pipeline
|  codelib |   devops-codelib
|  environment |   devops-environment
|  store |   devops-atomstore
|  ticket |   devops-ticket
|   common-lib   |  common-lib |
|   svg-sprites   |  svg-sprites |


最后将生成的frontend文件夹copy到`__INSTALL_PATH__/__MODULE__/`目录下
    
    最终前端部署目录结构如下：
```
__INSTALL_PATH__/__MODULE__/frontend/codelib
__INSTALL_PATH__/__MODULE__/frontend/commom-lib
__INSTALL_PATH__/__MODULE__/frontend/console
__INSTALL_PATH__/__MODULE__/frontend/environment
__INSTALL_PATH__/__MODULE__/frontend/pipeline
__INSTALL_PATH__/__MODULE__/frontend/store
__INSTALL_PATH__/__MODULE__/frontend/svg-sprites
__INSTALL_PATH__/__MODULE__/frontend/ticket
```
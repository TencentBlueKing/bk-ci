# bk-ci 前端部署文档

蓝鲸ci前端（frontend目录下）, 共有8个子目录，其中common-lib和svg-sprites为项目依赖的静态资源，其余6个目录均为vue的spa工程，其中devops-nav为主入口，其它子服务以iframe或amd的方式接入

## 系统要求

nodejs版本 8.0.0及以上


## 安装说明

- 1、打包并部署相应的vue工程
依次进入到devops-nav、devops-pipeline、devops-codelib、devops-environment、devops-atomstore、devops-ticket目录，在每个目录下均
```
# 先执行
npm install --user=root && npm rebuild node-sass
# 再执行
npm run public:external
```
(如果在install时遇到无法安装node-sass和phantomjs依赖，可选择配置国内镜像源)
(如果在执行npm run public:external过程中有遇到 Cannot read property 'range' of null 的报错，可尝试将对应工程package.json中的babel-eslint依赖的版本设置成 "babel-eslint": "^8.0.1")


执行完这两条命令后，每个目录下均会生成一个dist文件夹，把相应的dist文件夹重命名后放置到相应的部署目录即可

    每个目录下的dist文件夹重命名对应关系如下：

|   目录名称   |   dist文件夹重命名后名称     |
| ------------ | ---------------- |
|   devops-nav   |  console    |
|   devops-pipeline   |  pipeline |
|   devops-codelib   |  codelib |
|   devops-environment   |  environment    |
|   devops-atomstore   |  store |
|   devops-ticket   |  ticket |

即把devops-nav目录下的dist文件夹改名为console、devops-pipeline目录下的dist文件夹改名为pipeline......最后统一放到相应的部署目录

- 2、静态资源部署
common-lib和svg-sprites为项目依赖的静态资源,直接拷贝至部署目录下即可

所有的文件都copy到`__INSTALL_PATH__/__MODULE__/frontend`目录下

最终前端部署目录结构如下：
```shell
__INSTALL_PATH__/__MODULE__/frontend/codelib
__INSTALL_PATH__/__MODULE__/frontend/commom-lib
__INSTALL_PATH__/__MODULE__/frontend/console
__INSTALL_PATH__/__MODULE__/frontend/environment
__INSTALL_PATH__/__MODULE__/frontend/pipeline
__INSTALL_PATH__/__MODULE__/frontend/store
__INSTALL_PATH__/__MODULE__/frontend/svg-sprites
__INSTALL_PATH__/__MODULE__/frontend/ticket
```



# bkrepo 前端编译文档

## 系统要求

nodejs版本 8.0.0及以上

## 编译说明

- 1、打包并部署相应的vue工程
进入到src/frontend目录下

```shell
# 先全局安装yarn
npm install -g yarn
# 然后执行install
yarn install
# 然后安装每个子服务的依赖
yarn start
# 最后执行打包命令
yarn public
```


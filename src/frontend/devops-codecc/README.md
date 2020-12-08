# 项目开发规范

## 概要

* 项目采用前后端分离，后端与前端采用RESTful接口来通信，后端微服务间调用采用RESTful接口通信。
* 前端统一采用Vue.js框架，后端为SpringCloud微服务框架.
* 网关采用openresty, 负责用户登录鉴权以及请求的路由转发。
* 统一前端与后端网关来统一访问域名，消除跨站请求，提升效率。

## 前端代码工程规范说明

```
codecc-frontend
├── build                       # 工程化（打包、构建、应用配置、本地开发）配置文件或脚本
│   ├── dev-server.js           # 起本地开发服务
│   ├── dev.env.js              # 开发环境配置
│   ├── prod.env.js             # 生产环境配置
│   ├── webpack.base.conf.babel.js
│   ├── webpack.dev.conf.babel.js
│   └── webpack.prod.conf.babel.js
├── mock                        # 用于 mock 接口数据目录
│   └── ajax                    # 所有 ajax 形式的模拟数据处理程序目录
|       ├── app                 # 默认模块处理程序目录
|       |   └── index.js        # 默认模块的默认处理程序，发起端使用形如 app/index 请求路径来触发
│       └── module-x            # 增加一个新的模块处理程序目录
|           └── index.js        # 新模块的处理程序，发起端使用形如 module-x/index 请求路径来触发
├── src
│   ├── api                     # http client 的封装，整合了统一的请求错误提示等
│   ├── common                  # 通用的业务逻辑目录，如鉴权处理、组件库的引入、通用方法库等
│   ├── components              # Vue 业务组件目录，按模块划分
│   |   ├── module-a
|   |   |   ├── index.css
|   |   |   └── index.vue
│   |   └── module-b
│   ├── css                     # 应用于全局的样式文件目录
│   |   └── app.css
│   ├── images                  # 应用于全局的图片文件目录
│   |   └── 404.png
│   ├── router                  # 所有路由文件目录
│   |   └── index.js            # 配置 Vue Router，路由管理
│   ├── store                   # Vuex 配置管理目录，按模块划分
│   |   ├── modules
|   |   |   ├── module-a.js     # 模块a配置
|   |   |   └── module-b.js     # 模块b配置
│   |   └── index.js            # Vuex 的模块装配入口及公共 actions、mutations 等配置
│   ├── views                   # 应用的页面目录，使用 Vue 单文件组件
│   |   ├── module-a
|   |   |   ├── page1.vue       # 页面1，单文件组件
|   |   |   └── page2.vue       # 页面2，单文件组件
│   |   └── module-b            # 另一个其它模块目录
│   ├── App.vue                 # 应用的页面入口
│   └── main.js                 # 应用的脚本入口
├── static                      # 预设的静态资源和三方库 bundle 文件存放目录
├── .babelrc                    # babel 配置文件
├── .eslintrc                   # eslint 配置文件
├── index.html                  # 单页面 HTML 模板
└── package.json
```

### 说明

- 资源分类有序，应严格按照已有目录分类进行资源的存放与管理，如 应用内图片应放到 /src/images/ 目录中。
- 按模块扩展，如 components、store、views 在新增功能页面时，已有模块存在则在模块对应的目录内创建，不存在则新建模块目录。
- 保持目录整洁，赋有意义。任何时候不应该在工程根目录随意创建新的目录，对已有目录或文件不随意删除或重命名。

## 本地开发

0. 拷贝 index.html 为 index-dev.html，并替换文件中的 `__xxx__` 为实际情况的配置值

1. 配置 host

    ```
    127.0.0.1       dev.codecc.com
    ```

2. 运行 npm 脚本

    ```
    npm i # 首次
    npm run local
    ```

3. 打开浏览器，访问 <http://dev.codecc.com:8009>

## 组件

本工程是基于BKUI-CLI脚手架搭建的Vue项目，更多组件可参考[MagicBox Vue组件](https://magicbox.bk.tencent.com/static_api/v3/components_vue/2.0/example/index.html#/changelog)

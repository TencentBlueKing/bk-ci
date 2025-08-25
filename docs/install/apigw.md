## 蓝鲸API网关配置指南

### 1. 准备配置材料
从 [Releases](https://github.com/TencentBlueKing/bk-ci/releases) 下载配套版本的 `bkci-slim.tar.gz`
或从项目中获取，存放路径如下：
蓝鲸API网关配置文件： `./support-files/apigw/bk_apigw_resources_devops.yaml`
接口配套文档：`./support-files/apigw/bk_apigw_docs_devops.zip`

### 2.新建后端服务（首次配置蓝鲸API网关时）
访问蓝鲸API网关页面-后端服务-新建
服务名称填写：`backend-1`
各环境的服务配置：按需配置环境、后端服务地址填写蓝盾服务域名或IP

### 3. 页面导入
访问蓝鲸API网关页面-资源配置
点击导入-资源配置和资源文档。分别将步骤1中的`bk_apigw_resources_devops.yaml`和`bk_apigw_docs_devops.zip`导入即可

### 4. 生成版本、发布
访问蓝鲸API网关页面-资源配置 可选择生成版本。并根据页面操作发布至具体环境即可。
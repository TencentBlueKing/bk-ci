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

### 5. 让 openapi 从网关 JWT 解析 userId

用户态接口需要请求头 `X-DEVOPS-UID`。蓝鲸网关把已认证用户放在 `X-Bkapi-JWT` 中。openapi 在下面两个开关同时为 true，且能读到网关 JWT 公钥时，把 JWT 里 `user.username` 写入 `X-DEVOPS-UID`。只打开其中一个开关不会解析 userId。

| 含义 | Helm `config` | `scripts/bkenv.properties` | 默认 |
|---|---|---|---|
| 网关鉴权模式 | `bkCiOpenapiApiAuth` | `BK_CI_OPENAPI_API_AUTH` | Helm `false`，脚本 `true` |
| 启用 BlueKingApiFilter | `bkCiOpenapiApiBluekingEnable` | `BK_CI_OPENAPI_API_BLUEKING_ENABLE` | `false` |
| 公钥文件路径 | `bkCiOpenapiApiPubOuter` | `BK_CI_OPENAPI_API_PUB_OUTER` | 空 |

Kubernetes 在部署 values 中增加：

```yaml
config:
  bkCiOpenapiApiAuth: true
  bkCiOpenapiApiBluekingEnable: true
  bkCiOpenapiApiPubOuter: /data/workspace/openapi/bk_apigw_public_key.pub
openapi:
  enabled: true
  secret:
    enabled: true
    content: <公钥文件的单行 base64>
```

`content` 是网关 JWT 公钥 PEM 的 base64。Chart 只把这一个文件挂到 `bkCiOpenapiApiPubOuter`，Secret 的 data key 是路径里的文件名。路径的父目录必须已经在镜像里，`/data/workspace/openapi` 满足这个条件。公钥由蓝鲸 API 网关提供，不要提交进仓库。

脚本部署不使用 Secret。把公钥文件放到 `BK_CI_OPENAPI_API_PUB_OUTER` 指向的路径，两个开关都设为 `true`，再执行 `scripts/render_tpl`。
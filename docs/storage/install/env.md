# repo.env配置项说明

## 环境配置项

| 配置项      | 说明     | 示例              |
| ----------- | -------- | ----------------- |
| BK_REPO_ENV | 部署环境 | dev / test / prod |



## 网关配置项

| 配置项                          | 说明                | 示例                                                                                                    |
| ------------------------------- | ------------------- |-------------------------------------------------------------------------------------------------------|
| BK_REPO_HOST                    | bkrepo主机地址      | bkrepo.example.com                                                                                    |
| BK_REPO_UI_HOST                 | bkrepo前端主机地址  | bkrepo.example.com                                                                                    |
| BK_REPO_HTTP_PORT               | bkrepo http端口     | 80                                                                                                    |
| BK_REPO_HTTPS_PORT              | bkrepo https端口    | 443                                                                                                   |
| BK_REPO_APIGW_URL               | bkrepo api网关url   |                                                                                                       |
| BK_REPO_APP_CODE                | bkrepo app code     |                                                                                                       |
| BK_REPO_APP_TOKEN               | bkrepo app token    |                                                                                                       |
| BK_REPO_HOME                    | bkrepo部署目录      | /data/bkce                                                                                            |
| BK_REPO_LOGS_DIR                | bkrepo日志目录      | /data/logs                                                                                            |
| BK_REPO_PAAS_FQDN               | bkrepo paas fqdn    | paas.example.com                                                                                      |
| BK_REPO_PAAS_LOGIN_URL          | 蓝鲸paas登录地址    | http://paas.example.com:80/login/?c_url=                                                              |
| BK_REPO_AUTHORIZATION           | bkrepo认证token     | Platform MThiNjFjOWMtOTAxYi00ZWEzLTg5YzMtMWY3NGJlOTQ0YjY2OlVzOFpHRFhQcWs4NmN3TXVrWUFCUXFDWkxBa00zSw== |
| BK_REPO_GATEWAY_CORS_ALLOW_LIST | 网关跨域允许列表    |                                                                                                       |
| BK_REPO_GATEWAY_DNS_ADDR        | 网关dns解析服务地址 | 127.0.0.1:53                                                                                          |
| BK_REPO_SERVICE_PREFIX          | bkrepo微服务前缀    | bkrepo-                                                                                               |
| BK_REPO_DEPLOY_MODE             | bkrepo部署模式      | standalone / ci                                                                                       |

## consul配置项

| 配置项                     | 说明            | 示例      |
| -------------------------- | --------------- | --------- |
| BK_REPO_CONSUL_DNS_HOST    | consul host     | 127.0.0.1 |
| BK_REPO_CONSUL_DNS_PORT    | consul dns端口  | 53        |
| BK_REPO_CONSUL_SERVER_HOST | consul host     | 127.0.0.1 |
| BK_REPO_CONSUL_SERVER_PORT | consul http端口 | 8500      |
| BK_REPO_CONSUL_DOMAIN      | consul domain   | bk-repo   |
| BK_REPO_CONSUL_TAG         | consul tag      | bkce      |

## redis配置项

| 配置项                       | 说明            | 示例      |
| ---------------------------- | --------------- | --------- |
| BK_REPO_REDIS_HOST           | reids host      | 127.0.0.1 |
| BK_REPO_REDIS_ADMIN_PASSWORD | redis admin密码 |           |
| BK_REPO_REDIS_PORT           | redis端口       | 6379      |

## application配置项

| 配置项                   | 说明                                                | 示例                       |
| ------------------------ | --------------------------------------------------- | -------------------------- |
| BK_REPO_STORAGE_TYPE     | 存储方式                                            | filesystem / innercos      |
| BK_REPO_FILE_PATH        | 文件系统存储路径                                    | /data/storage              |
| BK_REPO_COS_SECRET_ID    | 对象存储secret id                                   |                            |
| BK_REPO_COS_SECRET_KEY   | 对象存储secret key                                  |                            |
| BK_REPO_COS_REGION       | 对象存储区域                                        | ap-guangzhou               |
| BK_REPO_COS_BUCKET       | 对象存储桶                                          | example-123456             |
| BK_REPO_CACHE_ENABLE     | 是否启动存储缓存。存储缓存使用分布式文件系统，如cfs | true / false               |
| BK_REPO_CACHE_MOUNT_PATH | 存储缓存挂载路径                                    | /data/bkrepo               |
| BK_REPO_CACHE_EXPIRE_DAY | 存储缓存过期天数， -1表示永不过期                   |                            |
| BK_REPO_MONGODB_URI      | mongodb连接uri                                      | mongodb://127.0.0.1/bkrepo |



## repository配置项

| 配置项                  | 说明               | 示例  |
| ----------------------- | ------------------ | ----- |
| BK_REPO_REPOSITORY_PORT | repository服务端口 | 25901 |



## auth配置项

| 配置项                    | 说明                 | 示例                             |
| ------------------------- | -------------------- | -------------------------------- |
| BK_REPO_AUTH_PORT         | auth服务端口         | 25902                            |
| BK_REPO_AUTH_REALM        | auth realm           | devops                           |
| BK_IAM_PRIVATE_URL        | 蓝鲸iam url          | http://iam.service.consul:8080   |
| BK_PAAS_PRIVATE_URL       | 蓝鲸paas url         | http://paas.service.consul       |
| BK_REPO_IAM_CALLBACK_USER | 蓝鲸iam 回调用户     |                                  |
| BK_REPO_IAM_ENV           | 蓝鲸iam环境          | prod                             |
| BK_REPO_IAM_HOST          | 蓝鲸iam主机地址      | iam.service.consul               |
| BK_REPO_IAM_HTTP_PORT     | 蓝鲸iam http端口     | 80                               |
| BK_REPO_IAM_IP0           | 蓝鲸iam ip           | iam.service.consul               |
| BK_REPO_IAM_TOKEN_URL     | 蓝鲸iam获取token url | /bkiam/api/v1/auth/access-tokens |
| BK_CI_AUTH_ENV            | 蓝盾auth环境         | prod                             |
| BK_CI_PUBLIC_URL          | 蓝盾url              | http://devops.example.com        |
| BK_CI_AUTH_TOKEN          | 蓝盾auth token       |                                  |
| BK_REPO_SSM_ENV           | 蓝鲸ssm环境          | prod                             |
| BK_REPO_SSM_HOST          | 蓝鲸ssm主机地址      | bkssm.service.consul             |
| BK_REPO_SSM_HTTP_PORT     | 蓝鲸ssm http端口     | 5000                             |
| BK_REPO_SSM_IP0           | 蓝鲸ssm ip           | bkssm.service.consul             |

## docker api配置项

| 配置项                                | 说明                | 示例                      |
| ------------------------------------- | ------------------- | ------------------------- |
| BK_REPO_DOCKERAPI_PORT                | docker api服务端口  | 25906                     |
| BK_REPO_DOCKERAPI_REALM               | docker api realm    | bkrepo                    |
| BK_REPO_DOCKERAPI_HARBOR_URL          | harbor url          |                           |
| BK_REPO_DOCKERAPI_HARBOR_USERNAME     | harbor用户名        |                           |
| BK_REPO_DOCKERAPI_HARBOR_PASSWORD     | harbor密码          |                           |
| BK_REPO_DOCKERAPI_HARBOR_IMAGE_PREFIX | harbor image prefix |                           |
| BK_SSM_PRIVATE_URL                    | 蓝鲸ssm url         | http://ssm.service.consul |

## generic配置项

| 配置项               | 说明            | 示例  |
| -------------------- | --------------- | ----- |
| BK_REPO_GENERIC_PORT | generic服务端口 | 25801 |

## docker配置项

| 配置项                    | 说明             | 示例                      |
| ------------------------- | ---------------- | ------------------------- |
| BK_REPO_DOCKER_PORT       | docker服务端口   | 25906                     |
| BK_REPO_DOCKER_HOST       | docker主机地址   | docker.bkrepo.example.com |
| BK_REPO_DOCKER_HTTP_PORT  | docker http端口  | 80                        |
| BK_REPO_DOCKER_HTTPS_PORT | docker https端口 | 443                       |
| BK_REPO_DOCKER_CERT_KEY   | docker 证书key   |                           |
| BK_REPO_DOCKER_CERT_PEM   | docker 证书pem   |                           |

## maven配置项

| 配置项             | 说明          | 示例  |
| ------------------ | ------------- | ----- |
| BK_REPO_MAVEN_PORT | maven服务端口 | 25803 |

## npm配置项

| 配置项           | 说明        | 示例  |
| ---------------- | ----------- | ----- |
| BK_REPO_NPM_PORT | npm服务端口 | 25804 |

## pypi配置项

| 配置项            | 说明         | 示例  |
| ----------------- | ------------ | ----- |
| BK_REPO_PYPI_PORT | pypi服务端口 | 25805 |

## helm配置项

| 配置项                 | 说明          | 示例                    |
| ---------------------- | ------------- | ----------------------- |
| BK_REPO_HELM_PORT      | helm服务端口  | 25806                   |
| BK_REPO_HELM_HOST      | helm主机地址  | helm.bkrepo.example.com |
| BK_REPO_HELM_HTTP_PORT | helm http端口 | 80                      |

## git配置项

| 配置项           | 说明        | 示例  |
| ---------------- | ----------- | ----- |
| BK_REPO_GIT_PORT | git服务端口 | 25810 |


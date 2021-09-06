{{- define "bkci.common.yaml" -}}
# Common config Template 重复的通用的配置抽离在到
spring:
  main:
    allow-bean-definition-overriding: true
  application:
    version: 4.0.0
# consul config do not need to change
  cloud:
    consul:
      port: {{ .Values.config.bkCiConsulHttpPort }}
      discovery:
        tags: {{ .Values.config.bkCiConsulDiscoveryTag }}
        instanceId: ${spring.application.name}-${server.port}-${spring.cloud.client.hostname}
# redis config
  redis:
#   # Redis sentinel 集群方式配置
#    sentinel:
#      master: {{ .Values.config.bkCiRedisSentinelMasterName }}
# 逗号分隔, master_IP:port
#      nodes: {{ .Values.config.bkCiRedisSentinelAddr }}
#   # Redis 单机配置方式
    host: {{ include "bkci.redisHost" . }}
    port: {{ include "bkci.redisPort" . }}
    password: {{ include "bkci.redisPassword" . }}
    database: {{ .Values.config.bkCiRedisDb }}
    pool:
      max-active: 16
      max-wait:  2000
# rabbitmq config
  rabbitmq:
    # 引擎核心业务MQ
    core:
      virtual-host: {{ include "bkci.rabbitmqVhost" . }}
      username: {{ include "bkci.rabbitmqUser" . }}
      password: {{ include "bkci.rabbitmqPassword" . }}
      addresses: {{ include "bkci.rabbitmqAddr" . }}
    # 拓展功能模块MQ -- 如果主rabbitmq出现性能瓶颈, 可以考虑使用额外的实例
    extend:
      virtual-host: {{ include "bkci.rabbitmqVhost" . }}
      username: {{ include "bkci.rabbitmqUser" . }}
      password: {{ include "bkci.rabbitmqPassword" . }}
      addresses: {{ include "bkci.rabbitmqAddr" . }}
  metrics:
    servo:
      enabled: false

endpoints:
  metrics:
    filter:
      enabled: false

service-suffix: "-{{ .Values.config.bkCiConsulDiscoveryTag }}"
# http concurrency
server:
  undertow:
    worker-threads: 100
    accesslog:
      enabled: true
      pattern: '%h %I "%{i,X-DEVOPS-UID}" [%{time,yyyyMMddHHmmss.S}] "%r" %s %D %b "%{i,Referer}" "%{i,User-Agent}"'
      dir: ${service.log.dir}

# gateway for system
devopsGateway:
  outer: {{ .Values.config.bkCiPublicUrl }}         # generic endpoint for public(internet or intranet) areas.
  outerApi: {{ .Values.config.bkCiPublicUrl }}      # endpoint for api access in public areas.
  host: {{ .Values.config.bkCiPublicUrl }}         # generic endpoint for private(inside bk-ci cluster) access.
  api: {{ .Values.config.bkCiPublicUrl }}          # endpoint for api access which inside BlueKing platform.
  build: {{ .Values.config.bkCiPublicUrl }}         # endpoint for build nodes, maybe use another dns server.
  idc: {{ .Values.config.bkCiPrivateUrl }}          # not used yet. keep it same as `host' property.
  idcProxy: {{ .Values.config.bkCiPrivateUrl }}     # not used yet. keep it same as `host' property.
  devnetProxy: {{ .Values.config.bkCiPrivateUrl }}  # not used yet. keep it same as `host' property.
  devnet: {{ .Values.config.bkCiPrivateUrl }}       # not used yet. keep it same as `host' property.
  oss: {{ .Values.config.bkCiPrivateUrl }}          # not used yet. keep it same as `host' property.
  ossProxy: {{ .Values.config.bkCiPrivateUrl }}     # not used yet. keep it same as `host' property.
  
# certificate server 配置
certificate:
  server: {{ .Values.config.bkLicensePrivateUrl }}/certificate


#S3 Storage
s3:
  endpointUrl: __BK_CI_S3_ENDPOINT_URL__   # 应该包含完整的地区等信息.
  accessKey: __BK_CI_S3_ACCESS_KEY__
  secretKey: __BK_CI_S3_SECRET_KEY__
  bucketName: __BK_CI_S3_BUCKET_NAME__

auth:
  # idProvider为权限中心对接方式，sample(开源默认实现无鉴权)/ bk_login（蓝鲸登录) / client
  # 选择sample后以下配置无效
  idProvider: {{ .Values.config.bkCiAuthProvider }}
  grantType: rization_code
  principalType: user
  envName: prod
  #  开发时需要配置Host解析到iam.service.consul
  url: {{ .Values.config.bkIamPrivateUrl }}
  appCode: {{ .Values.config.bkCiAppCode }}
  appSecret: {{ .Values.config.bkCiAppToken }}
  bkloginUrl: {{ .Values.config.bkPaasPrivateUrl }}/login/api/v2
  iamCallBackUser: {{ .Values.config.bkCiIamCallbackUser }}
  # 用户组权限申请前端跳转页面HOST
  webHost: {{ .Values.config.bkCiIamWebUrl }}

#bk esb config for cd plugin in pipeline
esb:
  code: {{ .Values.config.bkCiAppCode }}
  secret: {{ .Values.config.bkCiAppToken }}

# codecc config
codecc:
  host: {{ .Values.config.bkCodeccPrivateUrl }}  # 

codeccGateway:
  gateway: {{ .Values.config.bkCodeccPrivateUrl }}
  gatewayWeb: {{ .Values.config.bkCodeccPrivateUrl }}
  api:
   createTask: /ms/task/api/service/task/
   updateTask: /ms/task/api/service/task/
   checkTaskExists: /ms/task/api/service/task/exists/
   deleteTask: /ms/task/api/service/task/
   codeCheckReport: /ms/api/
alert:
  users: ''
codeoa:
  api-key: {{ .Values.config.bkCiCodeoaApiKey }}
  api-url: {{ .Values.config.bkCiCodeoaApiUrl }}
  git-url: {{ .Values.config.bkCiCodeoaGitUrl }}

bk:
  paas:
    host: {{ .Values.config.bkPaasPrivateUrl }}

bkci:
  security:
    public-key: "{{ .Values.config.bkCiJwtRsaPublicKey }}"
    private-key: "{{ .Values.config.bkCiJwtRsaPrivateKey }}"
    enable: false
    
process:
  deletedPipelineStoreDays: 30    

net:
  proxy:
    # 是否开启，设置为 true 才生效
    enable: false
    # 需要代理的hosts，多个使用","隔开，支持正则表达式
    hosts: ".*google.*"
    server:
      # 代理服务器类型，可 HTTP, SOCKS
      type: "HTTP"
      # 代理服务器主机，host 或者 ip
      host: 127.0.0.1
      # 代理服务器端口
      port: 8080
{{- end -}}
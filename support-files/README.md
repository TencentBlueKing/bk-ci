# 6. 初始化及配置模版(support-files)

```
|- bk-ci
  |- support-files
    |- agent-package  # 初始化时需要分发到部署主目录下，用于存放agent安装包供下载
    |- file           # 初始化时需要分发到application-artifactory.yml指定的目录下
    |- sql            # sql初始化脚本，在开发编译时就需要先初始化，否则JOOQ无法正常生成PO
    |- template       # 模板
```

## 蓝鲸运营规范---部署占位符---蓝盾需求

这些是存在配置文件中的，用于后续在部署时脚本自动/人工替换的，所以在新增占位符号时需要提交给蓝鲸运营组提供脚本的支持。


### 通用占位配置(现有)

```
# 数据库
__MYSQL_IP0__   数据库IP
__MYSQL_PORT__  数据库Port
__MYSQL_USER__  数据库用户
__MYSQL_PASS__  数据库密码

# Redis 集群配置 2选1，集群方式需要在common.yml中开启注释
__REDIS_MASTER_NAME__       如果配置了sentinel集群方式下Redis的MasterName
__REDIS_CLUSTER_HOST__      如果配置了sentinel集群方式下Redis主机节点
__REDIS_CLUSTER_PORT__      如果配置了sentinel集群方式下Redis端口
__REDIS_PASS__              密码
__REDIS_IP0__               单机方式下的RedisIP
__REDIS_PORT__              单机方式下的Redis端口

# RabbitMQ 配置 common.yml
__RABBITMQ_HOST__  RabbitMQ主机名称
__RABBITMQ_PORT__  RabbitMQ端口
__RABBITMQ_VHOST__ RabbitMQ Vhost
__RABBITMQ_USERNAME__ rabbitmq用户名
__RABBITMQ_PASSWORD__ rabbitmq密码

# DevOps平台配置 common.yml
__DEVOPS_FQDN__             DevOps域名
__DEVOPS_HTTP_PORT__        DevOps端口

# 各微服务的HTTP端口 application-{service}.yml   {service}.sh
__DEVOPS_PROJECT_API_PORT__     项目导航条服务端口，建议：21912
__DEVOPS_LOG_API_PORT__         日志服务端口，建议：21914
__DEVOPS_TICKET_API_PORT__      凭证服务端口，建议：21915
__DEVOPS_REPOSITORY_API_PORT__  代码库服务端口，建议：21916
__DEVOPS_STORE_API_PORT__       研发商店服务端口，建议：21918
__DEVOPS_ENVIRONMENT_API_PORT__ 环境管理服务端口，建议：21919
__DEVOPS_ARTIFACTORY_API_PORT__ 版本仓库服务端口，建议：21920
__DEVOPS_PROCESS_API_PORT__     流水线服务端口，建议：21921
__DEVOPS_DISPATCH_API_PORT__    调度服务端口，建议：21922
__DEVOPS_PLUGIN_API_PORT__      插件扩展服务端口，建议：21925

## ES 相关信息  application-log.yml
__DEVOPS_ES_IP__               日志ES集群IP    
__DEVOPS_ES_PORT__             日志ES集群Port
__DEVOPS_ES_CLUSTER__          日志ES集群的名称

# consul 项目  init.lua 和 common.yml
__DEVOPS_CONSUL_PORT__         consul的HTTP端口
__DEVOPS_CONSUL_TAG__          consul的注册tag(默认：devops)
__DEVOPS_CONSUL_DOMAIN__       consul的注册域名(默认：consul)
```

### 根据用户需要决定修改的占位符

```
# GitHub 配置，可有可无，看用户需要   application-repository.yml
__GITHUB_SIGN_SECRET__      用户公司在Github的repo创建的webhook统一的密钥
__GITHUB_APP__              用户公司在GitHub上创建的App名称
__GITHUB_CLIENT_ID__        用户公司在GitHub上创建的APP的Client ID
__GITHUB_CLIENT_SECRET__    用户公司在GitHub上创建的APP的Client secret

# 自搭建的GitLab仓库 -- 用户自己部署的gitlab仓库  application-repository.yml
__GITLAB_URL__              gitlab网站地址

# SVN配置 -- 如果用户有使用 application-repository.yml
__SVN_URL__         SVN地址
__SVN_API_KEY__     SVN的API密钥 

# 对接权限中心的配置 -- 如果用户要对接权限中心  init.lua
__IAM_IP0__         权限中心IP地址，比如蓝鲸权限中心地址
__IAM_HTTP_PORT__   权限中心端口，比如蓝鲸权限中心端口

# 蓝鲸PaaS平台配置, 如果对接了蓝鲸平台
__PAAS_FQDN__           蓝鲸PaaS域名
__PAAS_HTTP_PORT__      蓝鲸HTTP端口

# 对接蓝鲸时才会用到，否则忽略 蓝鲸应用在蓝鲸中的APPCode，默认bkci
__APP_CODE__  比如DevOps的AppCode是devops
__APP_TOKEN__ 对应的Token
```






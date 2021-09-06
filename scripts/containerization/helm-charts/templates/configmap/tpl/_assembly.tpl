{{- define "bkci.assembly.yaml" -}}
# 单服务的配置文件模板，整合了所有微服务模块
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}
    process:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    project:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_project?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    repository:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_repository?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    dispatch:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_dispatch?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    plugin:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_plugin?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    quality:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_quality?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    artifactory:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_artifactory?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}
    environment:
      url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_environment?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
      username: {{ include "bkci.mysqlUsername" . }}
      password: {{ include "bkci.mysqlPassword" . }}

server:
  port: {{ .Values.config.bkCiAssemblyApiPort }}

#### Artifactory
# 以下为构件存储目录，需要做分布式分享
# 如果微服务是部署多机节点，则以下目录需要做成分布式高可用的，比如NFS，CephFS挂载
# 保证多节点都能读取到
artifactory:
  realm: local  # 如果使用蓝鲸制品库，则该值要修改为 bkrepo
  archiveLocalBasePath: {{ .Values.config.bkCiDataDir }}/artifactory  # 如果多节点部署, 需要使用共享存储.
  fileTask:
    savedir: {{ .Values.config.bkCiDataDir }}/artifactory-filetask/  # 不一定共享, 但是建议准备较大容量.
    file:
      expireTimeMinutes: 720
    record:
      clear:
        enable: false
        expireTimeDays: 7

# 自已有部署Jfrog的可以对接
#jfrog:
#  url: __JFROG_URL__
#  docker_url: __DOCKER_URL__
#  docker_port: __DOCKER_PORT__
#  username: __JFROG_USERNAME__
#  password: __JFROG_PASSWORD__

#### Dispatch
dispatch:
  workerFile: {{ .Values.config.bkCiHome }}/agent-package/jar/worker-agent.jar
  dockerFile: {{ .Values.config.bkCiHome }}/agent-package/jar/worker-agent.jar
  scripts: {{ .Values.config.bkCiHome }}/agent-package/script
  #defaultImageUrl: {{ .Values.config.bkCiDockerImagePrefix }}/paas/bkdevops/centos7.2:v1
  #defaultImageName: centos7.2
  #dockerhostPort: {{ .Values.config.bkCiDockerhostApiPort }}
  jobQuota:
    systemAlertReceiver:
    enable: false

codecc:
  path: {{ .Values.config.bkCodeccDataDir }}/tools
  covFile: build_dev.py
  toolFile: build_tool_dev.py

#### environment

environment:
  agent-package: {{ .Values.config.bkCiHome }}/agent-package
  agentCollectorOn: {{ .Values.config.bkCiEnvironmentAgentCollectorOn }}

influxdb:
  server: http://{{ include "bkci.influxdbHost" . }}:{{ include "bkci.influxdbPort" . }}
  userName: {{ include "bkci.influxdbUsername" . }}
  password: {{ include "bkci.influxdbPassword" . }}

#### DockerHost

dockerhost:
  mode: docker_no_build

# docker client 配置
dockerCli:
  dockerHost: unix:///var/run/docker.sock
  dockerConfig: /root/.docker
  apiVersion: 1.23
  # docker hub 配置
  registryUrl: {{ .Values.config.bkCiDockerRegistryUrl }}
  registryUsername: {{ .Values.config.bkCiDockerRegistryUser }}
  registryPassword: {{ .Values.config.bkCiDockerRegistryPassword }}

  # 以下一般不用修改
  volumeWorkspace: /data/devops/workspace
  volumeApps: /data/devops/apps/
  volumeInit: /data/init.sh
  volumeSleep: /data/devops/sleep.sh
  volumeLogs: /data/devops/logs
  volumeCodecc: /data/devops/codecc/
  volumeProjectShare: /data/devops/share
  volumeMavenRepo: /root/.m2/repository
  volumeNpmPrefix: /root/Downloads/npm/prefix
  volumeNpmCache: /root/Downloads/npm/cache
  volumeNpmRc: /root/.npmrc
  volumeCcache: /root/.ccache
  volumeGradleCache: /root/.gradle/caches
  volumeGolangCache: /root/go/pkg/mod
  volumeSbtCache: /root/.ivy2
  volumeSbt2Cache: /root/.cache
  volumeYarnCache: /usr/local/share/.cache/
  hostPathLinkDir: /tmp/bkci
  hostPathHosts: /etc/hosts

  # docker 母机上的配置，workspace用于存放构建的工作空间，可以以挂载的方式提供
  hostPathWorkspace: {{ .Values.config.bkCiDataDir }}/docker/workspace
  hostPathApps: {{ .Values.config.bkCiDataDir }}/docker/apps/
  hostPathInit: {{ .Values.config.bkCiHome }}/agent-package/script/init.sh
  hostPathSleep: {{ .Values.config.bkCiHome }}/agent-package/script/sleep.sh
  hostPathLogs: {{ .Values.config.bkCiLogsDir }}/docker
  hostPathCodecc: /data/devops/codecc
  hostPathProjectShare: {{ .Values.config.bkCiDataDir }}/docker/share/project
  hostPathMavenRepo: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/maven_repo
  hostPathNpmPrefix: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/npm_prefix
  hostPathNpmCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/npm_cache
  hostPathNpmRc: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/.npmrc
  hostPathCcache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/ccache/
  hostPathGradleCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/gradle_caches
  hostPathGolangCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/go_cache/
  hostPathSbtCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/.ivy2/
  hostPathSbt2Cache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/sbt_cache/
  hostPathYarnCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/yarn_cache/
  # 需要共享的项目
  shareProjectCodeWhiteList:
  # docker.jar 存储路径
  dockerAgentPath: {{ .Values.config.bkCiHome }}/agent-package/worker-agent.jar
  downloadDockerAgentUrl: {{ .Values.config.bkCiPrivateUrl }}/ms/dispatch/gw/build/docker.jar
  # 定期下载
  downloadAgentCron: 0 0 3 * * ?
  landunEnv: prod
  clearLocalImageCron: 0 0 2 * * ?
  localImageCacheDays: 7

  # 无编译构建容器配置
  memoryLimitBytes: 2147483648
  cpuPeriod: 50000
  cpuQuota: 50000

#### image

image:
  dockerCli:
    dockerHost: unix:///var/run/docker.sock
    dockerConfig: /root/.docker
    apiVersion: 1.23
    registryUrl: {{ .Values.config.bkCiDockerRegistryUrl }}  # 什么类型的url?
    registryUsername: {{ .Values.config.bkCiDockerRegistryUser }}
    registryPassword: {{ .Values.config.bkCiDockerRegistryPassword }}
    imagePrefix: {{ .Values.config.bkCiDockerImagePrefix }}

#### log

log:
  storage:
    type: {{ .Values.config.bkCiLogStorageType }}                # 日志存储方式 lucene/elasticsearch
    closeInDay: {{ .Values.config.bkCiLogCloseDay }}             # 索引自动关闭时间，留空则为永不关闭
    deleteInDay: {{ .Values.config.bkCiLogDeleteDay }}           # 索引自动删除时间，留空则为永不删除
  # 通过本地文件系统进行存储的必要配置
  lucene:
    dataDirectory: {{ .Values.config.bkCiLuceneDataDir }}        # 建立lucene索引的根目录
    indexMaxSize: {{ .Values.config.bkCiLuceneIndexMaxSize }}   # 单个构建的最大日志行数，建议设在100万内
  # 通过Elasticsearch服务进行存储的必要配置
  elasticsearch:
    ip: {{ include "bkci.elasticsearchHost" . }}   # 今后只使用REST client.
    port: {{ include "bkci.elasticsearchPort" . }}
    cluster: {{ .Values.config.bkCiEsClusterName }}
    name: DEVOPS
    #  ES集群如果不要求账号密码认证，则可以去掉以下2个ES的访问账号密码配置项或留空
    username: {{ include "bkci.elasticsearchUsername" . }}
    password: {{ include "bkci.elasticsearchPassword" . }}
    #  ES集群如果要求HTTPS协议请求，则需要需要打开以下5个配置项， 设置keystore文件和truststore文件以及密码
#    https: true
#    keystore:
#      filePath:
#      password:
#    truststore:
#      filePath:
#      password:

#### misc

build:
  atomMonitorData:
    report:
      switch: false
      maxMonitorDataSize: 1677216
  data:
    clear:
      switch: false
      maxEveryProjectHandleNum: 5
      monthRange: -1
      maxKeepNum: 10000
      codeccDayRange: -14
      codeccMaxKeepNum: 14
      otherMonthRange: -1
      otherMaxKeepNum: 500

#### openapi
# 是否开启apifilter和aspect功能
api:
  gateway:
    auth: false

#### plugin

plugin:
  codecc:
    path: {{ .Values.config.bkCodeccDataDir }}/tools
    covFile: build_dev.py
    toolFile: build_tool_dev.py

#### project
# 蓝鲸登录平台API地址，对接蓝鲸平台时才需要用到
bk_login:
  path: {{ .Values.config.bkPaasPrivateUrl }}/api/c/compapi/v2/bk_login/
  getUser: get_user/
  getAllUser: get_all_users/
  bk_app_code: {{ .Values.config.bkCiAppCode }}
  bk_app_secret: {{ .Values.config.bkCiAppToken }}

#### repository

repository:
  git: # git 管理私有Token，一般不用，用于插件库分组
    devopsPrivateToken: {{ .Values.config.bkCiRepositoryGitPrivateToken }}
    devopsGroupName: {{ .Values.config.bkCiRepositoryGitPluginGroupName }}

scm:
  #svn open api url，如果不需要则放着即可
  svn:
    apiKey: {{ .Values.config.bkCiRepositorySvnApiKey }}
    apiUrl: {{ .Values.config.bkCiRepositorySvnApiUrl }}  # 结尾一般为 /rest
    webhookApiUrl: {{ .Values.config.bkCiRepositorySvnWebhookUrl }}  # 一般为 SVN_API_URL/webhooks
    svnHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/codesvn/commit
  # git 仓库配置，如果不需要则放着即可
  git:
    url: {{ .Values.config.bkCiRepositoryGitUrl }}
    apiUrl: {{ .Values.config.bkCiRepositoryGitUrl }}/api/v3
    clientId: clientId
    clientSecret: Secret
    redirectUrl: {{ .Values.config.bkCiPublicUrl }}/console/codelib
    redirectAtomMarketUrl: {{ .Values.config.bkCiPublicUrl }}/console/store/atomList
    gitHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/codegit/commit
    public:  # TODO 无用配置, 待清理.
      account: devops
      email: devops@devops.com
      secret: devops123
  external:
    #gitlab v4.
    gitlab:
      apiUrl: {{ .Values.config.bkCiRepositoryGitlabUrl }}/api/v4
      gitlabHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/gitlab/commit
    github:
      clientId: {{ .Values.config.bkCiRepositoryGithubClientId }}
      clientSecret: {{ .Values.config.bkCiRepositoryGithubClientSecret }}
      callbackUrl: {{ .Values.config.bkCiPublicUrl }}/external/api/external/github/oauth/callback
      redirectUrl: {{ .Values.config.bkCiPublicUrl }}/console/codelib
      appUrl: https://github.com/apps/{{ .Values.config.bkCiRepositoryGithubApp }}
      signSecret: {{ .Values.config.bkCiRepositoryGithubSignSecret }}
    tGit:
      apiUrl: https://git.tencent.com/api/v3
      tGitHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/codetgit/commit
      hookSecret:
      enableHookSecret: false
#### store

store:
  commentNotifyAdmin: admin
  profileUrlPrefix: {{ .Values.config.bkCiStoreUserAvatarsUrl }}
  atomDetailBaseUrl: /console/store/atomStore/detail/atom/
  templateDetailBaseUrl: /console/store/atomStore/detail/template/
  artifactoryServiceUrlPrefix: {{ .Values.config.bkCiPublicUrl }}/ms/artifactory/api
  ideAtomDetailBaseUrl: /console/store/atomStore/detail/ide/
  imageDetailBaseUrl: /console/store/atomStore/detail/image/
  serviceDetailBaseUrl: /console/store/atomStore/detail/service/
  baseImageDocsLink: /console/store/atomStore/detail/image/
  imageAdminUsers: admin
  buildResultBaseUrl: {{ .Values.config.bkCiPublicUrl }}/console/pipeline
  defaultImageSourceType: bkdevops
  defaultImageRepoUrl: {{ .Values.config.bkCiDockerRegistryUrl }}
  defaultImageRepoName: paas/bkdevops/docker-builder2.2
  defaultImageTag: v1
  defaultTicketId:
  defaultTicketProject:
  defaultImageRDType: SELF_DEVELOPED
  imageExecuteNullNotifyTplCode: IMAGE_EXECUTE_NULL_NOTIFY_TPL
  templateApproveSwitch: close
  imageApproveSwitch: close

logo:
  allowUploadLogoTypes: jpg,png,svg
  allowUploadLogoWidth: 200
  allowUploadLogoHeight: 200
  maxUploadLogoSize: 2097152

aes:
  # 部署前修改好，后续升级如果再修改，会导致历史数据读取不了，所以如果修改需要先解密重新入库
  aesKey: "J%k^yO{?vt}3tXpG"
  aesMock: "******"

statistics:
  timeSpanMonth: -3

#### ticket

cert:
  # 部署前修改好，后续升级如果再修改，会导致历史数据读取不了，所以如果修改需要先解密重新入库
  aes-key: "gHi(xG9Af)jEvCx&"

credential:
  mixer: "******"
  # 部署前修改好，后续升级如果再修改，会导致历史数据读取不了，所以如果修改需要先解密重新入库
  aes-key: "G/I%yP{?ST}2TXPg"

#### websocket

thread:
  min: 8

#### process

# 流水线加密参数密钥
parameter:
  password:
    pswKey: {{ .Values.config.bkCiPipelineParameterEncryptPassword }}

# 流水线引擎并发配置
queueConcurrency:
  buildStart: {{ .Values.config.bkCiProcessEventConcurrent }}    # 构建启动消息并发处理数量
  stage: {{ .Values.config.bkCiProcessEventConcurrent }}         # 步骤消息并发处理数量
  container: {{ .Values.config.bkCiProcessEventConcurrent }}     # Job消息并发处理数量
  task: {{ .Values.config.bkCiProcessEventConcurrent }}          # Task消息并发处理数量
  buildFinish: {{ .Values.config.bkCiProcessEventConcurrent }}   # 构建结束消息并发处理数量

# 流水线模板配置
template:
  instanceListUrl: "{{ .Values.config.bkCiPublicUrl }}/console/store/pipeline/{0}/template/{0}/instance"

# 流水线相关配置
pipeline:
  setting:
    common:
      maxModelSize: 16777215
      maxStageNum: 20
      stage:
        maxJobNum: 20
        job:
          maxTaskNum: 50
          task:
            maxInputNum: 100
            maxOutputNum: 100
            inputComponent:
              input:
                size: 1024
              textarea:
                size: 16384
              codeEditor:
                size: 16384
              default:
                size: 1024
              multiple:
                member: "selector,select-input,devops-select,atom-checkbox-list,staff-input,company-staff-input,parameter,dynamic-parameter"
                size: 4000
            outputComponent:
              default:
                size: 4000
  editPath: "console/pipeline/{0}/{1}/edit"
  atom:
    maxRelQueryNum: 2000  # 最大查询数量
    maxRelQueryRangeTime: 30  # 最大查询时间跨度
  version:
    max_keep_num: 50
    spec_channels: "AM,CODECC,GCLOUD,GIT,GONGFENGSCAN,CODECC_EE"
    spec_channel_max_keep_num: 2

#### sign
# 签名服务临时解压缩目录
bkci:
  sign:
    tmpDir: "/tmp/enterprise_sign_tmp/"
{{- end -}}
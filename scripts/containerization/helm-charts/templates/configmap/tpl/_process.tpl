{{- define "bkci.process.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21921
server:
  port: {{ .Values.config.bkCiProcessApiPort }}

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
  

build:
  atomMonitorData:
    report:
      switch: false
      maxMonitorDataSize: 1677216
 
# 流水线相关配置 
pipeline:
  build:
    retry:
      limitDays: -1  # 多少天之前的构建可以被重试（不是rebuild)，-1表示不限制， 0表示不可重试
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
scm:
  external:
    tGit:
      hookSecret: 
      enableHookSecret: false
{{- end -}}
{{- define "bkci.artifactory.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_artifactory?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21920
server:
  port: {{ .Values.config.bkCiArtifactoryApiPort }}

# 以下为构件存储目录，需要做分布式分享
# 如果微服务是部署多机节点，则以下目录需要做成分布式高可用的，比如NFS，CephFS挂载
# 保证多节点都能读取到
# fileTask为文件托管任务配置，主要用于作品平台构件分发插件，
# 可配置文件暂存目录、临时文件过期时间、数据库记录清理策略
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
{{- end -}}
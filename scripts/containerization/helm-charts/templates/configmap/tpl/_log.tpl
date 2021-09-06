{{- define "bkci.log.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_log?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21914
server:
  port: {{ .Values.config.bkCiLogApiPort }}

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
{{- end -}}
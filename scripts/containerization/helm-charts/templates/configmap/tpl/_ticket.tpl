{{- define "bkci.ticket.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_ticket?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21915
server:
  port: {{ .Values.config.bkCiTicketApiPort }}


cert:
  # 部署前修改好，后续升级如果再修改，会导致历史数据读取不了，所以如果修改需要先解密重新入库
  aes-key: "gHi(xG9Af)jEvCx&"

credential:
  mixer: "******"
  # 部署前修改好，后续升级如果再修改，会导致历史数据读取不了，所以如果修改需要先解密重新入库
  aes-key: "G/I%yP{?ST}2TXPg"
{{- end -}}
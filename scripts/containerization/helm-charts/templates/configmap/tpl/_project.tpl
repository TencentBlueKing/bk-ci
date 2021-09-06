{{- define "bkci.project.yaml" -}}
# Project Service Template 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_project?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21912
server:
  port: {{ .Values.config.bkCiProjectApiPort }}

# 蓝鲸登录平台API地址，对接蓝鲸平台时才需要用到
bk_login:
  path: {{ .Values.config.bkPaasPrivateUrl }}/api/c/compapi/v2/bk_login/
  getUser: get_user/
  getAllUser: get_all_users/
  bk_app_code: {{ .Values.config.bkCiAppCode }}
  bk_app_secret: {{ .Values.config.bkCiAppToken }}
{{- end -}}
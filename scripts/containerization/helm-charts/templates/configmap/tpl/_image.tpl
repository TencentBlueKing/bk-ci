{{- define "bkci.image.yaml" -}}
# Image Service Template 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_image?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21933
server:
  port: {{ .Values.config.bkCiImageApiPort }}


image:
  dockerCli:
    dockerHost: unix:///var/run/docker.sock
    dockerConfig: /root/.docker
    apiVersion: 1.23
    registryUrl: {{ .Values.config.bkCiDockerRegistryUrl }}  # 什么类型的url?
    registryUsername: {{ .Values.config.bkCiDockerRegistryUser }}
    registryPassword: {{ .Values.config.bkCiDockerRegistryPassword }}
    imagePrefix: {{ .Values.config.bkCiDockerImagePrefix }}
{{- end -}}
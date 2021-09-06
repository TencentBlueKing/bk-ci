{{- define "bkci.dispatch-docker.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_dispatch?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21938
server:
  port: {{ .Values.config.bkCiDispatchDockerApiPort }}

dispatch:
  workerFile: {{ .Values.config.bkCiHome }}/agent-package/jar/worker-agent.jar
  dockerFile: {{ .Values.config.bkCiHome }}/agent-package/jar/worker-agent.jar
  scripts: {{ .Values.config.bkCiHome }}/agent-package/script
  #defaultImageUrl: {{ .Values.config.bkCiDockerImagePrefix }}/paas/bkdevops/centos7.2:v1
  #defaultImageName: centos7.2
  #dockerhostPort: {{ .Values.config.bkCiDockerhostApiPort }}
  agentLessRegistryUrl: {{ .Values.config.bkCiAgentlessImageRegistryUrl }}
  agentLessImageName: {{ .Values.config.bkCiAgentlessImageName }}
  agentLessRegistryUserName: {{ .Values.config.bkCiAgentlessImageRegistryUser }}
  agentLessRegistryPassword: {{ .Values.config.bkCiAgentlessImageRegistryPassword }}
  jobQuota:
    systemAlertReceiver: 
    enable: false

codecc:
  path: {{ .Values.config.bkCodeccDataDir }}/tools
  covFile: build_dev.py
  toolFile: build_tool_dev.py
{{- end -}}
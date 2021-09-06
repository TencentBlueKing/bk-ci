{{- define "bkci.monitoring.yaml" -}}
# 服务配置文件模板

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21911
server:
  port: {{ .Values.config.bkCiMonitoringApiPort }}

influxdb:
  server: http://{{ include "bkci.influxdbHost" . }}:{{ include "bkci.influxdbPort" . }}
  userName: {{ include "bkci.influxdbUsername" . }}
  password: {{ include "bkci.influxdbPassword" . }}{{- end -}}
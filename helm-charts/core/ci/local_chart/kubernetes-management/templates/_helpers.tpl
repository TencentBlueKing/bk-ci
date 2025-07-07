{{/*
Return the mysql fullname
*/}}
{{- define "k8sm.mysql.fullname" -}}
{{- if .Values.mysql.fullnameOverride -}}
{{- .Values.mysql.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "mysql" .Values.mysql.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the redis fullname
*/}}
{{- define "k8sm.redis.fullname" -}}
{{- if .Values.redis.fullnameOverride -}}
{{ $name := .Values.redis.fullnameOverride | trunc 63 | trimSuffix "-"}}
{{- list $name "master" | join "-" -}}
{{- else -}}
{{- $name := default "redis" .Values.redis.nameOverride -}}
{{- printf "%s-%s-master" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
MySQL addr
*/}}
{{- define "k8sm.mysqlAddr" -}}
{{- if eq .Values.mysql.enabled true -}}
{{- include "k8sm.mysql.fullname" . -}}:3306
{{- else -}}
  {{- if ne .Values.externalMysql.host "" }}
    {{- .Values.externalMysql.host -}}:{{- .Values.externalMysql.port }}
  {{- else -}}
    {{- printf "%s-%s" .Release.Name "mysql" | trunc 63 | trimSuffix "-" -}}:3306
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
MySQL username
*/}}
{{- define "k8sm.mysqlUsername" -}}
{{- if eq .Values.mysql.enabled true -}}
root
{{- else -}}
{{- .Values.externalMysql.username -}}
{{- end -}}
{{- end -}}

{{/*
MySQL password
*/}}
{{- define "k8sm.mysqlPassword" -}}
{{- if eq .Values.mysql.enabled true -}}
{{- .Values.mysql.auth.rootPassword -}}
{{- else -}}
{{- .Values.externalMysql.password -}}
{{- end -}}
{{- end -}}

{{/*
Redis host
*/}}
{{- define "k8sm.redisHost" -}}
{{- if eq .Values.redis.enabled true -}}
{{- include "k8sm.redis.fullname" . -}}
{{- else -}}
  {{- if ne .Values.externalRedis.host "" }}
    {{- .Values.externalRedis.host -}}
  {{- else -}}
    {{- printf "%s-%s-master" .Release.Name "redis" | trunc 63 | trimSuffix "-" -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Redis port
*/}}
{{- define "k8sm.redisPort" -}}
{{- if eq .Values.redis.enabled true -}}
6379
{{- else -}}
{{- .Values.externalRedis.port -}}
{{- end -}}
{{- end -}}

{{/*
Redis password
*/}}
{{- define "k8sm.redisPassword" -}}
{{- if eq .Values.redis.enabled true -}}
{{- .Values.redis.auth.password -}}
{{- else -}}
{{- .Values.externalRedis.password -}}
{{- end -}}
{{- end -}}

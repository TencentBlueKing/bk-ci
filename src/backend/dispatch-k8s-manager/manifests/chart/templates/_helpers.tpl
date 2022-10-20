{{/*
Return the mysql fullname
*/}}
{{- define "bkci.mysql.fullname" -}}
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
{{- define "bkci.redis.fullname" -}}
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
{{- define "bkci.mysqlAddr" -}}
{{- if eq .Values.mysql.enabled true -}}
{{- include "bkci.mysql.fullname" . -}}:3306
{{- else -}}
{{- .Values.externalMysql.host -}}:{{- .Values.externalMysql.port -}}
{{- end -}}
{{- end -}}

{{/*
MySQL username
*/}}
{{- define "bkci.mysqlUsername" -}}
{{- if eq .Values.mysql.enabled true -}}
root
{{- else -}}
{{- .Values.externalMysql.username -}}
{{- end -}}
{{- end -}}

{{/*
MySQL password
*/}}
{{- define "bkci.mysqlPassword" -}}
{{- if eq .Values.mysql.enabled true -}}
{{- .Values.mysql.auth.rootPassword -}}
{{- else -}}
{{- .Values.externalMysql.password -}}
{{- end -}}
{{- end -}}

{{/*
Redis host
*/}}
{{- define "bkci.redisHost" -}}
{{- if eq .Values.redis.enabled true -}}
{{- include "bkci.redis.fullname" . -}}
{{- else -}}
{{- .Values.externalRedis.host -}}
{{- end -}}
{{- end -}}

{{/*
Redis port
*/}}
{{- define "bkci.redisPort" -}}
{{- if eq .Values.redis.enabled true -}}
6379
{{- else -}}
{{- .Values.externalRedis.port -}}
{{- end -}}
{{- end -}}

{{/*
Redis password
*/}}
{{- define "bkci.redisPassword" -}}
{{- if eq .Values.redis.enabled true -}}
{{- .Values.redis.auth.password -}}
{{- else -}}
{{- .Values.externalRedis.password -}}
{{- end -}}
{{- end -}}

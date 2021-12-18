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
Return the elasticsearch fullname
*/}}
{{- define "bkci.elasticsearch.fullname" -}}
{{- if .Values.elasticsearch.fullnameOverride -}}
{{ $name := .Values.elasticsearch.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- list $name "master" | join "-" -}}
{{- else -}}
{{- $name := default "elasticsearch" .Values.elasticsearch.nameOverride -}}
{{- printf "%s-%s-master" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the rabbitmq fullname
*/}}
{{- define "bkci.rabbitmq.fullname" -}}
{{- if .Values.rabbitmq.fullnameOverride -}}
{{- .Values.rabbitmq.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "rabbitmq" .Values.rabbitmq.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the influxdb fullname
*/}}
{{- define "bkci.influxdb.fullname" -}}
{{- if .Values.influxdb.fullnameOverride -}}
{{- .Values.influxdb.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "influxdb" .Values.influxdb.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}


{{/*
Return the mongodb fullname
*/}}
{{- define "bkci.mongodb.fullname" -}}
{{- if .Values.mongodb.fullnameOverride -}}
{{- .Values.mongodb.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "mongodb" .Values.mongodb.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
MySQL host
*/}}
{{- define "bkci.mysqlAddr" -}}
{{- if eq .Values.mysql.enabled true -}}
{{- include "bkci.mysql.fullname" . -}}:3306
{{- else -}}
{{- .Value.config.bkCiMysqlAddr -}}
{{- end -}}
{{- end -}}

{{/*
MySQL username
*/}}
{{- define "bkci.mysqlUsername" -}}
{{- if eq .Values.mysql.enabled true -}}
root
{{- else -}}
{{- .Values.config.bkCiMysqlUser -}}
{{- end -}}
{{- end -}}

{{/*
MySQL password
*/}}
{{- define "bkci.mysqlPassword" -}}
{{- if eq .Values.mysql.enabled true -}}
{{- .Values.mysql.auth.rootPassword -}}
{{- else -}}
{{- .Values.config.bkCiMysqlPassword -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch host
*/}}
{{- define "bkci.elasticsearchHost" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
{{- include "bkci.elasticsearch.fullname" . -}}
{{- else -}}
{{- .Value.config.bkCiEsRestAddr -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch port
*/}}
{{- define "bkci.elasticsearchPort" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
9200
{{- else -}}
{{- .Value.config.bkCiEsRestPort -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch username
*/}}
{{- define "bkci.elasticsearchUsername" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
{{- else -}}
{{- .Value.config.bkCiEsUser -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch password
*/}}
{{- define "bkci.elasticsearchPassword" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
{{- else -}}
{{- .Value.config.bkCiEsPassword -}}
{{- end -}}
{{- end -}}

{{/*
Redis host
*/}}
{{- define "bkci.redisHost" -}}
{{- if eq .Values.redis.enabled true -}}
{{- include "bkci.redis.fullname" . -}}
{{- else -}}
{{- .Value.config.bkCiRedisHost -}}
{{- end -}}
{{- end -}}

{{/*
Redis port
*/}}
{{- define "bkci.redisPort" -}}
{{- if eq .Values.redis.enabled true -}}
6379
{{- else -}}
{{- .Value.config.bkCiRedisPort -}}
{{- end -}}
{{- end -}}

{{/*
Redis password
*/}}
{{- define "bkci.redisPassword" -}}
{{- if eq .Values.redis.enabled true -}}
{{- .Values.redis.auth.password -}}
{{- else -}}
{{- .Value.config.bkCiRedisPort -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq addr
*/}}
{{- define "bkci.rabbitmqAddr" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- include "bkci.rabbitmq.fullname" . -}}
{{- else -}}
{{- .Value.config.bkCiRabbitmqAddr -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq user
*/}}
{{- define "bkci.rabbitmqUser" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- .Values.rabbitmq.auth.username -}}
{{- else -}}
{{- .Value.config.bkCiRabbitmqUser -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq password
*/}}
{{- define "bkci.rabbitmqPassword" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- .Values.rabbitmq.auth.password -}}
{{- else -}}
{{- .Value.config.bkCiRabbitmqPassword -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq vhost
*/}}
{{- define "bkci.rabbitmqVhost" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
default-vhost
{{- else -}}
{{- .Value.config.bkCiRabbitmqVhost -}}
{{- end -}}
{{- end -}}

{{/*
Influxdb host
*/}}
{{- define "bkci.influxdbHost" -}}
{{- if eq .Values.influxdb.enabled true -}}
{{- include "bkci.influxdb.fullname" . -}}
{{- else -}}
{{- .Value.external.influxdb.host -}}
{{- end -}}
{{- end -}}

{{/*
Influxdb port
*/}}
{{- define "bkci.influxdbPort" -}}
{{- if eq .Values.influxdb.enabled true -}}
8086
{{- else -}}
{{- .Values.external.influxdb.port  -}}
{{- end -}}
{{- end -}}

{{/*
influxdb username
*/}}
{{- define "bkci.influxdbUsername" -}}
{{- if eq .Values.influxdb.enabled true -}}
{{ .Values.influxdb.auth.admin.username }}
{{- else -}}
{{- .Values.external.influxdb.username -}}
{{- end -}}
{{- end -}}

{{/*
influxdb password
*/}}
{{- define "bkci.influxdbPassword" -}}
{{- if eq .Values.influxdb.enabled true -}}
{{ .Values.influxdb.auth.admin.password }}
{{- else -}}
{{- .Values.external.influxdb.password -}}
{{- end -}}
{{- end -}}

{{/*
Return the db_turbo mongodb connection uri
*/}}
{{- define "bkci.mongodb.turbo.turboUri" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- printf "mongodb://%s:%s@%s:27017/db_turbo" .Values.mongodb.auth.username .Values.mongodb.auth.password (include "bkci.mongodb.fullname" .) -}}
{{- else -}}
{{- .Values.external.mongodb.turbo.turboUrl -}}
{{- end -}}
{{- end -}}


{{/*
Return the db_quartz mongodb connection uri
*/}}
{{- define "bkci.mongodb.turbo.quartzUri" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- printf "mongodb://%s:%s@%s:27017/db_quartz" .Values.mongodb.auth.username .Values.mongodb.auth.password (include "bkci.mongodb.fullname" .) -}}
{{- else -}}
{{- .Values.external.mongodb.turbo.quartzUrl -}}
{{- end -}}
{{- end -}}
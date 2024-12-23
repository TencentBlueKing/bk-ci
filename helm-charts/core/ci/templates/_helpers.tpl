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
Elasticsearch host
*/}}
{{- define "bkci.elasticsearchHost" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
{{- include "bkci.elasticsearch.fullname" . -}}
{{- else -}}
{{- .Values.externalElasticsearch.host -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch port
*/}}
{{- define "bkci.elasticsearchPort" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
9200
{{- else -}}
{{- .Values.externalElasticsearch.port -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch username
*/}}
{{- define "bkci.elasticsearchUsername" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
""
{{- else -}}
{{- .Values.externalElasticsearch.username -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch password
*/}}
{{- define "bkci.elasticsearchPassword" -}}
{{- if eq .Values.elasticsearch.enabled true -}}
""
{{- else -}}
{{- .Values.externalElasticsearch.password -}}
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

{{/*
Rabbitmq addr
*/}}
{{- define "bkci.rabbitmqAddr" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- include "bkci.rabbitmq.fullname" . -}}
{{- else -}}
{{- .Values.externalRabbitmq.host -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq user
*/}}
{{- define "bkci.rabbitmqUser" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- .Values.rabbitmq.auth.username -}}
{{- else -}}
{{- .Values.externalRabbitmq.username -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq password
*/}}
{{- define "bkci.rabbitmqPassword" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
{{- .Values.rabbitmq.auth.password -}}
{{- else -}}
{{- .Values.externalRabbitmq.password -}}
{{- end -}}
{{- end -}}

{{/*
Rabbitmq vhost
*/}}
{{- define "bkci.rabbitmqVhost" -}}
{{- if eq .Values.rabbitmq.enabled true -}}
default-vhost
{{- else -}}
{{- .Values.externalRabbitmq.vhost -}}
{{- end -}}
{{- end -}}

{{/*
Influxdb host
*/}}
{{- define "bkci.influxdbHost" -}}
{{- if eq .Values.influxdb.enabled true -}}
{{- include "bkci.influxdb.fullname" . -}}
{{- else -}}
{{- .Values.externalInfluxdb.host -}}
{{- end -}}
{{- end -}}

{{/*
Influxdb port
*/}}
{{- define "bkci.influxdbPort" -}}
{{- if eq .Values.influxdb.enabled true -}}
8086
{{- else -}}
{{- .Values.externalInfluxdb.port  -}}
{{- end -}}
{{- end -}}

{{/*
influxdb username
*/}}
{{- define "bkci.influxdbUsername" -}}
{{- if eq .Values.influxdb.enabled true -}}
{{ .Values.influxdb.auth.admin.username }}
{{- else -}}
{{- .Values.externalInfluxdb.username -}}
{{- end -}}
{{- end -}}

{{/*
influxdb password
*/}}
{{- define "bkci.influxdbPassword" -}}
{{- if eq .Values.influxdb.enabled true -}}
{{ .Values.influxdb.auth.admin.password }}
{{- else -}}
{{- .Values.externalInfluxdb.password -}}
{{- end -}}
{{- end -}}

{{/*
Return the db_turbo mongodb connection uri
*/}}
{{- define "bkci.mongodb.turbo.turboUri" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- printf "mongodb://%s:%s@%s:27017/db_turbo" .Values.mongodb.auth.username .Values.mongodb.auth.password (include "bkci.mongodb.fullname" .) -}}
{{- else -}}
{{- .Values.externalMongodb.turbo.turboUrl -}}
{{- end -}}
{{- end -}}


{{/*
Return the db_quartz mongodb connection uri
*/}}
{{- define "bkci.mongodb.turbo.quartzUri" -}}
{{- if eq .Values.mongodb.enabled true -}}
{{- printf "mongodb://%s:%s@%s:27017/db_quartz" .Values.mongodb.auth.username .Values.mongodb.auth.password (include "bkci.mongodb.fullname" .) -}}
{{- else -}}
{{- .Values.externalMongodb.turbo.quartzUrl -}}
{{- end -}}
{{- end -}}

{{/*
bkci standard labels
*/}}
{{- define "bkci.labels.standard" -}}
helm.sh/chart: {{ include "common.names.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Labels to use on deploy.spec.selector.matchLabels and svc.spec.selector
*/}}
{{- define "bkci.labels.matchLabels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "bkci.names.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Return the bkci gateway image name
*/}}
{{- define "bkci-gateway.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.gatewayImage "global" .Values.global) }}
{{- end -}}

{{/*
Return the bkci frontend image name
*/}}
{{- define "bkci-frontend.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.frontendImage "global" .Values.global) }}
{{- end -}}

{{/*
Return the bkci backend image name
*/}}
{{- define "bkci-backend.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.backendImage "global" .Values.global) }}
{{- end -}}


{{/*
Return the bkci turbo image name
*/}}
{{- define "bkci-turbo.image" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.turbo.image "global" .Values.global) }}
{{- end -}}

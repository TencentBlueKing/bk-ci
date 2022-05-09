{{/*
Expand the name of the chart.
*/}}
{{- define "bktbs.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "bktbs.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "bktbs.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "bktbs.labels" -}}
helm.sh/chart: {{ include "bktbs.chart" . }}
{{ include "bktbs.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "bktbs.selectorLabels" -}}
app.kubernetes.io/name: {{ include "bktbs.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Return the proper Docker Image Registry Secret Names
*/}}
{{- define "bktbs.imagePullSecrets" -}}
{{- include "common.images.pullSecrets" (dict "images" (list .Values.server.image .Values.gateway.image .Values.dashboard.image) "global" .Values.global) -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "bktbs.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "bktbs.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name for MariaDB subchart
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "bktbs.mariadb.fullname" -}}
{{- if .Values.mariadb.fullnameOverride -}}
{{- .Values.mariadb.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "mariadb" .Values.mariadb.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the database Hostname
*/}}
{{- define "bktbs.mariadb.host" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" (include "bktbs.mariadb.fullname" .) -}}
{{- else -}}
    {{- printf "%s" .Values.externalDatabase.host -}}
{{- end -}}
{{- end -}}

{{/*
Return the Database Address
*/}}
{{- define "bktbs.mariadb.address" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s:3306" (include "bktbs.mariadb.host" .) -}}
{{- else -}}
    {{- printf "%s:%s" .Values.externalDatabase.host .Values.externalDatabase.port -}}
{{- end -}}
{{- end -}}

{{/*
Return the Database Database
*/}}
{{- define "bktbs.mariadb.database" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.auth.database -}}
{{- else -}}
    {{- printf "%s" .Values.externalDatabase.database -}}
{{- end -}}
{{- end -}}


{{/*
Return the Database Username
*/}}
{{- define "bktbs.mariadb.username" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.auth.username -}}
{{- else -}}
    {{- printf "%s" .Values.externalDatabase.username -}}
{{- end -}}
{{- end -}}

{{/*
Return the Database Password
*/}}
{{- define "bktbs.mariadb.password" -}}
{{- if .Values.mariadb.enabled }}
    {{- printf "%s" .Values.mariadb.auth.password -}}
{{- else -}}
    {{- printf "%s" .Values.externalDatabase.password -}}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified app name for Etcd subchart
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "bktbs.etcd.fullname" -}}
{{- if .Values.etcd.fullnameOverride -}}
{{- .Values.etcd.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "etcd" .Values.etcd.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the Etcd Address
*/}}
{{- define "bktbs.etcd.address" -}}
{{- if .Values.etcd.enabled }}
    {{- printf "http://%s:2379" (include "bktbs.etcd.fullname" .) -}}
{{- else -}}
    {{- printf "%s" .Values.externalEtcd.address -}}
{{- end -}}
{{- end -}}
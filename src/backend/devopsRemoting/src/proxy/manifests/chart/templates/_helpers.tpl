{{/*
Return the apisix-admin fullname
*/}}
{{- define "proxy.apisix.admin.fullname" -}}
{{- if .Values.apisix.fullnameOverride -}}
{{- .Values.apisix.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "apisix-admin" .Values.apisix.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the apisix-dashboard fullname
*/}}
{{- define "proxy.apisix.dashboard.fullname" -}}
{{- if .Values.apisix.dashboard.fullnameOverride -}}
{{- .Values.apisix.dashboard.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "dashboard" .Values.apisix.dashboard.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
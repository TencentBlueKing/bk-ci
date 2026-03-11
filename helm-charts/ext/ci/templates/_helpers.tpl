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

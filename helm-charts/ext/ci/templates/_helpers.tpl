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

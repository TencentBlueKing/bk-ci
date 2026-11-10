{{/*
获取 Secret 中某个 key 的值。
configured 非空，或 enabled 为 false 时，直接返回 configured（可能为空）；
否则复用已有 Secret 中的值，没有时随机生成，并在同一次渲染中保持一致。
参数：enabled、configured、secretName、key、length（默认 32）、context
*/}}
{{- define "bkci.secrets.getOrGenerate" -}}
{{- $ctx := .context -}}
{{- $configured := .configured | default "" | toString -}}
{{- if or $configured (not .enabled) -}}
{{- $configured -}}
{{- else -}}
{{- if not (hasKey $ctx.Values "bkciGeneratedSecrets") -}}
{{- $_ := set $ctx.Values "bkciGeneratedSecrets" dict -}}
{{- end -}}
{{- $cache := $ctx.Values.bkciGeneratedSecrets -}}
{{- $cacheKey := printf "%s/%s" .secretName .key -}}
{{- if not (hasKey $cache $cacheKey) -}}
{{- $existing := lookup "v1" "Secret" $ctx.Release.Namespace .secretName -}}
{{- if and $existing $existing.data (index $existing.data .key) -}}
{{- $_ := set $cache $cacheKey (index $existing.data .key | b64dec) -}}
{{- else -}}
{{- $_ := set $cache $cacheKey (randAlphaNum (.length | default 32 | int)) -}}
{{- end -}}
{{- end -}}
{{- index $cache $cacheKey -}}
{{- end -}}
{{- end -}}

{{/*
服务间调用白名单 token。values 中已填写则使用该值；
secret.devopsToken.enabled 为 true 时，未填写则复用已有 Secret 或随机生成。
*/}}
{{- define "bkci.devopsToken.secretName" -}}
{{- printf "%s-devops-token" (include "bkci.names.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "bkci.devopsToken" -}}
{{- if not .Values.config -}}
{{- $_ := set .Values "config" dict -}}
{{- end -}}
{{- include "bkci.secrets.getOrGenerate" (dict
  "enabled" .Values.secret.devopsToken.enabled
  "configured" .Values.config.bkCiDevopsToken
  "secretName" (include "bkci.devopsToken.secretName" .)
  "key" "token"
  "context" .) -}}
{{- end -}}

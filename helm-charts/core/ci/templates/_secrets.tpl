{{/*
获取 Secret 中某个 key 的值。
configured 非空，或 autoGenerate 为 false 时，直接返回 configured（可能为空）；
否则复用已有 Secret 中的值，没有时随机生成，并在同一次渲染中保持一致。
参数：autoGenerate、configured、secretName、key、length（默认 32）、context
*/}}
{{- define "bkci.secrets.getOrGenerate" -}}
{{- $ctx := .context -}}
{{- $configured := .configured | default "" | toString -}}
{{- if or $configured (not .autoGenerate) -}}
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
secret.devopsToken.autoGenerate 为 true 时，未填写则复用已有 Secret 或随机生成。
*/}}
{{- define "bkci.devopsToken.secretName" -}}
{{- printf "%s-devops-token" (include "bkci.names.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "bkci.devopsToken" -}}
{{- if not .Values.config -}}
{{- $_ := set .Values "config" dict -}}
{{- end -}}
{{- include "bkci.secrets.getOrGenerate" (dict
  "autoGenerate" .Values.secret.devopsToken.autoGenerate
  "configured" .Values.config.bkCiDevopsToken
  "secretName" (include "bkci.devopsToken.secretName" .)
  "key" "token"
  "context" .) -}}
{{- end -}}

{{/*
触发器 webhook 回调密钥，所有触发器共用一个 Secret，name 即 Secret 中的键名（小驼峰）。
values 中已填写 config.bkCi<Name>WebhookSecret 则使用该值；
secret.webhookSecrets.<name>.autoGenerate 为 true 时，未填写则复用已有 Secret 或随机生成。
密钥随注册请求保存到对端，变化会导致已注册的 webhook 全部校验失败，必须保持稳定。
用法：include "bkci.webhookSecret" (dict "name" "artifact" "context" .)
*/}}
{{- define "bkci.webhookSecret.secretName" -}}
{{- printf "%s-webhook-secrets" (include "bkci.names.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "bkci.webhookSecret" -}}
{{- $ctx := .context -}}
{{- if not $ctx.Values.config -}}
{{- $_ := set $ctx.Values "config" dict -}}
{{- end -}}
{{- $cfg := index ($ctx.Values.secret.webhookSecrets | default dict) .name | default dict -}}
{{- include "bkci.secrets.getOrGenerate" (dict
  "autoGenerate" (dig "autoGenerate" false $cfg)
  "configured" (index $ctx.Values.config (printf "bkCi%sWebhookSecret" (title .name)))
  "secretName" (include "bkci.webhookSecret.secretName" $ctx)
  "key" .name
  "context" $ctx) -}}
{{- end -}}

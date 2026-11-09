{{/*
获取或生成一个需要长期保持不变的密钥：
优先用户配置 .Values.config[valueKey]；其次读取已持久化的 Secret[key]；都没有才随机生成。
结果写回 .Values.config，保证同一次渲染中多处引用取到同一个值。
用法：include "bkci.secrets.manage" (dict "context" . "secret" "<secret名>" "key" "<键>" "valueKey" "<config参数名>")
可选参数：
  length   随机生成的长度，默认 32
  generate 都没有值时是否随机生成，默认 true；存量留空即不校验的来源（如 TAPD、Git）须设为 false，
           否则升级后会突然开启校验，导致存量 webhook 失效
*/}}
{{- define "bkci.secrets.manage" -}}
{{- $ctx := .context -}}
{{- $generate := true -}}
{{- if hasKey . "generate" -}}
{{- $generate = .generate -}}
{{- end -}}
{{- $val := index $ctx.Values.config .valueKey | default "" -}}
{{- if empty $val -}}
{{- $data := (lookup "v1" "Secret" $ctx.Release.Namespace .secret).data | default dict -}}
{{- if hasKey $data .key -}}
{{- $val = index $data .key | b64dec -}}
{{- else if $generate -}}
{{- $val = randAlphaNum (.length | default 32) -}}
{{- end -}}
{{- $_ := set $ctx.Values.config .valueKey $val -}}
{{- end -}}
{{- $val -}}
{{- end -}}

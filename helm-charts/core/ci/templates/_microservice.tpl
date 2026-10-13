{{/*
Standard Java microservice workload.
Call with (dict "name" $name "root" $).
*/}}
{{- define "bkci.ms.workload" -}}
{{- $name := .name -}}
{{- $root := .root -}}
{{- $svc := index $root.Values $name -}}
{{- $component := "microservice" -}}
{{- $fullname := printf "%s-%s" (include "bkci.names.fullname" $root) $name -}}
{{- $stateful := eq $name "websocket" -}}
{{- if eq $name "artifactory" -}}
{{- if eq $root.Values.config.bkCiArtifactoryRealm "local" -}}
{{- $stateful = true -}}
{{- end -}}
{{- end -}}
{{- $failureThreshold := 20 -}}
{{- if eq $name "store" -}}
{{- $failureThreshold = 30 -}}
{{- end -}}
{{- $agentPackage := or (eq $name "dispatch") (eq $name "environment") -}}
{{- if $stateful }}
apiVersion: {{ include "common.capabilities.statefulset.apiVersion" $root }}
kind: StatefulSet
{{- else }}
apiVersion: {{ include "common.capabilities.deployment.apiVersion" $root }}
kind: Deployment
{{- end }}
metadata:
  name: {{ $fullname }}
  labels: {{- include "bkci.labels.standard" $root | nindent 4 }}
    {{- if $root.Values.commonLabels }}
      {{- include "common.tplvalues.render" (dict "value" $root.Values.commonLabels "context" $root) | nindent 4 }}
    {{- end }}
    app.kubernetes.io/component: {{ $component }}
    app.kubernetes.io/name: {{ $name }}
spec:
  {{- if not $stateful }}
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 0
    type: RollingUpdate
  {{- end }}
  selector:
    matchLabels: {{- include "bkci.labels.matchLabels" $root | nindent 6 }}
      app.kubernetes.io/component: {{ $component }}
      app.kubernetes.io/name: {{ $name }}
  {{- if not $svc.autoscaling.enabled }}
  replicas: {{ $svc.replicas }}
  {{- if eq $name "websocket" }}
  serviceName: {{ $fullname }}
  {{- end }}
  {{- end }}
  {{- if and (eq $name "artifactory") $stateful }}
  serviceName: {{ $fullname }}
  {{- end }}
  template:
    metadata:
      labels: {{- include "bkci.labels.standard" $root | nindent 8 }}
        {{- if $svc.podLabels }}
        {{- include "common.tplvalues.render" (dict "value" $svc.podLabels "context" $root) | nindent 8 }}
        {{- end }}
        app.kubernetes.io/component: {{ $component }}
        app.kubernetes.io/name: {{ $name }}
    spec:
      serviceAccountName: {{ $root.Values.rbac.serviceAccount }}
      enableServiceLinks: false
      {{- if $svc.hostAliases }}
      hostAliases: {{- include "common.tplvalues.render" (dict "value" $svc.hostAliases "context" $root) | nindent 8 }}
      {{- end }}
      {{- if $svc.affinity }}
      affinity: {{- include "common.tplvalues.render" (dict "value" $svc.affinity "context" $root) | nindent 8 }}
      {{- else }}
      affinity:
        podAffinity: {{- include "common.affinities.pods" (dict "type" $svc.podAffinityPreset "context" $root) | nindent 10 }}
        podAntiAffinity: {{- include "common.affinities.pods" (dict "type" $svc.podAntiAffinityPreset "context" $root) | nindent 10 }}
        nodeAffinity: {{- include "common.affinities.nodes" (dict "type" $svc.nodeAffinityPreset.type "key" $svc.nodeAffinityPreset.key "values" $svc.nodeAffinityPreset.values) | nindent 10 }}
      {{- end }}
      {{- if $svc.nodeSelector }}
      nodeSelector: {{- include "common.tplvalues.render" (dict "value" $svc.nodeSelector "context" $root) | nindent 8 }}
      {{- end }}
      {{- if $svc.tolerations }}
      tolerations: {{- include "common.tplvalues.render" (dict "value" $svc.tolerations "context" $root) | nindent 8 }}
      {{- end }}
      {{- if $svc.priorityClassName }}
      priorityClassName: {{ $svc.priorityClassName | quote }}
      {{- end }}
      {{- if $svc.podSecurityContext.enabled }}
      securityContext: {{- omit $svc.podSecurityContext "enabled" | toYaml | nindent 8 }}
      {{- end }}
      {{- if and $root.Values.mysql.enabled (ne $name "ai") }}
      initContainers:
        - name: wait-for-mysql
          image: {{ include "common.images.image" (dict "imageRoot" $root.Values.backendImage "global" $root.Values.global) }}
          imagePullPolicy: {{ $root.Values.backendImage.pullPolicy }}
          {{ $mysqlData := split ":" (include "bkci.mysqlAddr" $root) }}
          command:
            - "/bin/sh"
            - "-c"
            - |
              while ! mysql -u{{- include "bkci.mysqlUsername" $root }} -p{{- include "bkci.mysqlPassword" $root }} -h{{ $mysqlData._0 }} -P{{ $mysqlData._1 }}; do
                echo "mysql connecting.."
                sleep 3
              done
              echo "MySQL is available";
      {{- end }}
      containers:
        - name: {{ $name }}
          image: {{ include "bkci-backend.image" $root }}
          imagePullPolicy: {{ $root.Values.backendImage.pullPolicy }}
          {{- if $svc.containerSecurityContext.enabled }}
          securityContext: {{- omit $svc.containerSecurityContext "enabled" | toYaml | nindent 12 }}
          {{- end }}
          {{- if $svc.resources }}
          resources: {{- toYaml $svc.resources | nindent 12 }}
          {{- end }}
          ports:
            - name: http
              containerPort: 80
          env:
            - name: MS_NAME
              value: {{ $name }}
            - name: NAMESPACE
              value: {{ $root.Release.Namespace }}
            - name: RELEASE_NAME
              value: {{ $root.Release.Name }}
            - name: CHART_NAME
              value: {{ $root.Chart.Name }}
            - name: MULTI_CLUSTER
              value: {{ $root.Values.multiCluster.enabled | quote }}
            - name: DEFAULT_NAMESPACE
              value: {{ $root.Values.multiCluster.defaultNamespace }}
            - name: JAVA_TOOL_OPTIONS
              value: {{ $root.Values.java.options }} -XX:InitialRAMPercentage={{ $svc.jvm.heapPct }} -XX:MaxRAMPercentage={{ $svc.jvm.heapPct }}
            - name: MALLOC_ARENA_MAX
              value: {{ $root.Values.java.mallocArenaMax | quote }}
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  apiVersion: v1
                  fieldPath: metadata.name
          workingDir: /data/workspace/{{ $name }}
          {{ if and $root.Values.entrypoint.override (eq $name "artifactory") }}
          command:
            - "/bin/bash"
            - "-c"
            - |
              if [ ! -d "{{ $root.Values.persistence.mountPath }}/artifactory/file" ]; then
                mkdir -p {{ $root.Values.persistence.mountPath }}/artifactory;
                cp -r -n /data/workspace/support-files/file {{ $root.Values.persistence.mountPath }}/artifactory/file;
              fi;
              cp /data/workspace/backend.bkci.sh .;
              mkdir -p {{ $root.Values.config.bkCiDataDir }};
              ln -s {{ $root.Values.persistence.mountPath }}/artifactory {{ $root.Values.config.bkCiDataDir }}/artifactory;
              /bin/bash backend.bkci.sh;
          {{ end }}
          {{ if and $root.Values.entrypoint.override $agentPackage }}
          command:
            - "/bin/bash"
            - "-c"
            - |
              cp /data/workspace/backend.bkci.sh .;
              mkdir -p {{ $root.Values.config.bkCiHome }};
              ln -s /data/workspace/agent-package {{ $root.Values.config.bkCiHome }}/agent-package;
              /bin/bash backend.bkci.sh;
          {{ end }}
          {{ if and $root.Values.entrypoint.override (not $agentPackage) (ne $name "artifactory") (ne $name "ai") }}
          command:
            - "/bin/bash"
            - "-c"
            - |
              cp /data/workspace/backend.bkci.sh .;
              /bin/bash backend.bkci.sh;
          {{ end }}
          livenessProbe:
            httpGet:
              path: /management/health/livenessState
              port: http
            initialDelaySeconds: {{ $svc.jvm.initSecs }}
            periodSeconds: 15
            timeoutSeconds: 10
            failureThreshold: {{ $failureThreshold }}
            successThreshold: 1
          readinessProbe:
            httpGet:
              path: /management/health/readinessState
              port: http
            initialDelaySeconds: {{ $svc.jvm.initSecs }}
            periodSeconds: 15
            timeoutSeconds: 10
            failureThreshold: {{ $failureThreshold }}
            successThreshold: 1
          volumeMounts:
            {{- if eq $name "artifactory" }}
            - name: storage
              mountPath: {{ $root.Values.persistence.mountPath }}
            {{- end }}
            - mountPath: /data/workspace/{{ $name }}/jvm
              name: log-volume
              subPathExpr: bkci/jvm/$(POD_NAME)
            {{- if and (eq $name "openapi") $svc.secret.enabled }}
            - mountPath: {{ $root.Values.config.bkCiOpenapiApiPubOuter | splitList "/" | initial | join "/" }}
              name: bk-key-volume
              readOnly: true
            {{- end }}
          lifecycle:
            preStop:
              exec:
                command:
                  - /bin/bash
                  - -c
                  {{- if eq $name "ai" }}
                  - sleep 20
                  {{- else }}
                  - sleep 20 && ps -ef|grep java|grep -v grep|awk '{print $2}'|xargs kill -15
                  {{- end }}
      volumes:
        {{- if eq $name "artifactory" }}
        - name: storage
          {{- if and $root.Values.persistence.enabled (eq $root.Values.config.bkCiArtifactoryRealm "local") }}
          persistentVolumeClaim:
            claimName: {{ if $root.Values.persistence.existingClaim }}{{ $root.Values.persistence.existingClaim }}{{- else }}{{ include "bkci.names.fullname" $root }}-storage{{- end }}
          {{- else }}
          emptyDir: {}
          {{- end }}
        {{- end }}
        - hostPath:
            path: /data
          name: log-volume
        {{- if and (eq $name "openapi") $svc.secret.enabled }}
        - name: bk-key-volume
          secret:
            secretName: openapi-bk-key
        {{- end }}
{{- end -}}

{{/*
ClusterIP service for a microservice, plus frontend and gateway.
*/}}
{{- define "bkci.ms.service" -}}
{{- $name := .name -}}
{{- $root := .root -}}
{{- $component := "microservice" -}}
{{- if or (eq $name "frontend") (eq $name "gateway") -}}
{{- $component = $name -}}
{{- end -}}
{{- $svcType := "ClusterIP" -}}
{{- if eq $name "gateway" -}}
{{- $svcType = $root.Values.gateway.type -}}
{{- end -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ printf "%s-%s" (include "bkci.names.fullname" $root) $name }}
  labels: {{- include "bkci.labels.standard" $root | nindent 4 }}
    app.kubernetes.io/name: {{ $name }}
    app.kubernetes.io/component: {{ $component }}
    {{- if $root.Values.commonLabels }}
      {{- include "common.tplvalues.render" (dict "value" $root.Values.commonLabels "context" $root) | nindent 4 }}
    {{- end }}
  {{- if $root.Values.commonAnnotations }}
  annotations: {{- include "common.tplvalues.render" (dict "value" $root.Values.commonAnnotations "context" $root) | nindent 4 }}
  {{- end }}
spec:
  type: {{ $svcType }}
  selector: {{- include "bkci.labels.matchLabels" $root | nindent 4 }}
    app.kubernetes.io/component: {{ $component }}
    app.kubernetes.io/name: {{ $name }}
  ports:
    - name: http
      port: 80
      targetPort: 80
      protocol: TCP
{{- end -}}

{{/*
HPA for a microservice. Websocket targets its StatefulSet; artifactory stays on Deployment.
*/}}
{{- define "bkci.ms.hpa" -}}
{{- $name := .name -}}
{{- $root := .root -}}
{{- $svc := index $root.Values $name -}}
{{- $component := "microservice" -}}
{{- if eq $name "gateway" -}}
{{- $component = "gateway" -}}
{{- end -}}
{{- $targetKind := "Deployment" -}}
{{- $targetAPI := include "common.capabilities.deployment.apiVersion" $root -}}
{{- if eq $name "websocket" -}}
{{- $targetKind = "StatefulSet" -}}
{{- $targetAPI = include "common.capabilities.statefulset.apiVersion" $root -}}
{{- end -}}
apiVersion: autoscaling/v2beta1
kind: HorizontalPodAutoscaler
metadata:
  name: {{ printf "%s-%s" (include "bkci.names.fullname" $root) $name }}
  labels: {{- include "bkci.labels.standard" $root | nindent 4 }}
    app.kubernetes.io/name: {{ $name }}
    app.kubernetes.io/component: {{ $component }}
    {{- if $root.Values.commonLabels }}
      {{- include "common.tplvalues.render" (dict "value" $root.Values.commonLabels "context" $root) | nindent 4 }}
    {{- end }}
  {{- if $root.Values.commonAnnotations }}
  annotations: {{- include "common.tplvalues.render" (dict "value" $root.Values.commonAnnotations "context" $root) | nindent 4 }}
  {{- end }}
spec:
  scaleTargetRef:
    apiVersion: {{ $targetAPI }}
    kind: {{ $targetKind }}
    name: {{ printf "%s-%s" (include "bkci.names.fullname" $root) $name }}
  minReplicas: {{ $svc.autoscaling.minReplicas }}
  maxReplicas: {{ $svc.autoscaling.maxReplicas }}
  metrics:
    {{- if $svc.autoscaling.targetCPU }}
    - type: Resource
      resource:
        name: cpu
        targetAverageUtilization: {{ $svc.autoscaling.targetCPU }}
    {{- end }}
    {{- if $svc.autoscaling.targetMemory }}
    - type: Resource
      resource:
        name: memory
        targetAverageUtilization: {{ $svc.autoscaling.targetMemory }}
    {{- end }}
{{- end -}}

{{/*
ConfigMap shell. Application yaml stays in bkci.<name>.yaml.
*/}}
{{- define "bkci.ms.configmap" -}}
{{- $name := .name -}}
{{- $root := .root -}}
kind: ConfigMap
apiVersion: v1
metadata:
  name: {{ printf "%s-%s" (include "bkci.names.fullname" $root) $name }}
  labels: {{- include "bkci.labels.standard" $root | nindent 4 }}
    app.kubernetes.io/component: {{ $name }}
    {{- if $root.Values.commonLabels }}
      {{- include "common.tplvalues.render" (dict "value" $root.Values.commonLabels "context" $root) | nindent 4 }}
    {{- end }}
  {{- if $root.Values.commonAnnotations }}
  annotations: {{- include "common.tplvalues.render" (dict "value" $root.Values.commonAnnotations "context" $root) | nindent 4 }}
  {{- end }}
data:
  application.yml: |-
    {{- include (printf "bkci.%s.yaml" $name) $root | nindent 4 -}}
{{- end -}}

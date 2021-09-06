{{- define "bkci.store.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_artifactory?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21918
server:
  port: {{ .Values.config.bkCiStoreApiPort }}

store:
  commentNotifyAdmin: admin
  profileUrlPrefix: {{ .Values.config.bkCiStoreUserAvatarsUrl }}
  atomDetailBaseUrl: /console/store/atomStore/detail/atom/
  templateDetailBaseUrl: /console/store/atomStore/detail/template/
  artifactoryServiceUrlPrefix: {{ .Values.config.bkCiPublicUrl }}/ms/artifactory/api
  ideAtomDetailBaseUrl: /console/store/atomStore/detail/ide/
  imageDetailBaseUrl: /console/store/atomStore/detail/image/
  serviceDetailBaseUrl: /console/store/atomStore/detail/service/
  baseImageDocsLink: /console/store/atomStore/detail/image/
  imageAdminUsers: admin
  buildResultBaseUrl: {{ .Values.config.bkCiPublicUrl }}/console/pipeline
  defaultImageSourceType: bkdevops
  defaultImageRepoUrl: {{ .Values.config.bkCiDockerRegistryUrl }}
  defaultImageRepoName: paas/bkdevops/docker-builder2.2
  defaultImageTag: v1
  defaultTicketId:
  defaultTicketProject:
  defaultImageRDType: SELF_DEVELOPED
  imageExecuteNullNotifyTplCode: IMAGE_EXECUTE_NULL_NOTIFY_TPL
  templateApproveSwitch: close
  imageApproveSwitch: close
  imageAgentTypes: "DOCKER"

logo:
  allowUploadLogoTypes: jpg,png,svg
  allowUploadLogoWidth: 200
  allowUploadLogoHeight: 200
  maxUploadLogoSize: 2097152

aes:
  # 部署前修改好，后续升级如果再修改，会导致历史数据读取不了，所以如果修改需要先解密重新入库
  aesKey: "J%k^yO{?vt}3tXpG"
  aesMock: "******"
  
statistics:
  timeSpanMonth: -3  
{{- end -}}
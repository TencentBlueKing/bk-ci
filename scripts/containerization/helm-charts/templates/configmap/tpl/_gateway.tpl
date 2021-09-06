{{- define "bkci.gateway.yaml" -}}
BK_CI_CONSUL_DEVNET_IP: {{ .Values.config.bkCiConsulDevnetIp | quote }}
BK_CI_INFLUXDB_USER: {{ include "bkci.influxdbUsername" . | quote}}
BK_PAAS_FQDN: {{ .Values.config.bkPaasFqdn | quote }}
BK_CI_FQDN: {{ .Values.config.bkCiFqdn | quote }}
BK_SSM_HOST: {{ .Values.config.bkSsmHost | quote }}
BK_CI_HOME: {{ .Values.config.bkCiHome | quote }}
BK_CI_INFLUXDB_PORT: {{ include "bkci.influxdbPort" . | quote}}
BK_CI_CONSUL_IP: {{ .Values.config.bkCiConsulIp | quote }}
BK_CODECC_HOME: {{ .Values.config.bkCodeccHome | quote }}
BK_CI_REDIS_PORT: {{ include "bkci.redisPort" . | quote}}
BK_CI_GATEWAY_SSM_TOKEN_URL: {{ .Values.config.bkCiGatewaySsmTokenUrl | quote }}
BK_CI_CONSUL_PORT: {{ .Values.config.bkCiConsulPort | quote }}
BK_CI_PAASCI_FQDN: {{ .Values.config.bkCiPaasciFqdn | quote }}
BK_CI_DATA_DIR: {{ .Values.config.bkCiDataDir | quote }}
BK_CI_CONSUL_DNS_PORT: {{ .Values.config.bkCiConsulDnsPort | quote }}
BK_CI_GATEWAY_CORS_ALLOW_LIST: {{ .Values.config.bkCiGatewayCorsAllowList | quote }}
BK_HTTP_SCHEMA: {{ .Values.config.bkHttpSchema | quote }}
BK_CI_HOST: {{ .Values.config.bkCiHost | quote }}
BK_CI_BKREPO_AUTHORIZATION: {{ .Values.config.bkCiBkrepoAuthorization | quote }}
BK_CI_JFROG_FQDN: {{ .Values.config.bkCiJfrogFqdn | quote }}
BK_CI_JFROG_USER: {{ .Values.config.bkCiJfrogUser | quote }}
BK_SSM_PORT: {{ .Values.config.bkSsmPort | quote }}
BK_CI_REDIS_DB: {{ .Values.config.bkCiRedisDb | quote }}
BK_CI_GATEWAY_DNS_ADDR: {{ .Values.config.bkCiGatewayDnsAddr | quote }}
BK_CI_CONSUL_DOMAIN: {{ .Values.config.bkCiConsulDomain | quote }}
BK_REPO_HOST: {{ .Values.config.bkRepoHost | quote }}
BK_CI_INFLUXDB_HOST: {{ include "bkci.influxdbHost" . | quote}}
BK_CI_HTTP_PORT: {{ .Values.config.bkCiHttpPort | quote }}
BK_CI_JFROG_HTTP_PORT: {{ .Values.config.bkCiJfrogHttpPort | quote }}
BK_CI_PAAS_LOGIN_URL: {{ .Values.config.bkCiPaasLoginUrl | quote }}
BK_PAAS_HTTPS_PORT: {{ .Values.config.bkPaasHttpsPort | quote }}
BK_CI_REDIS_PASSWORD: {{ include "bkci.redisPassword" . | quote}}
BK_CI_AUTH_PROVIDER: {{ .Values.config.bkCiAuthProvider | quote }}
BK_CI_CONSUL_DISCOVERY_TAG: {{ .Values.config.bkCiConsulDiscoveryTag | quote }}
BK_CI_HTTPS_PORT: {{ .Values.config.bkCiHttpsPort | quote }}
BK_CI_GATEWAY_REGION_NAME: {{ .Values.config.bkCiGatewayRegionName | quote }}
BK_CI_INFLUXDB_PASSWORD: {{ include "bkci.influxdbPassword" . | quote}}
BK_CI_APP_CODE: {{ .Values.config.bkCiAppCode | quote }}
BK_CI_REDIS_HOST: {{ include "bkci.redisHost" . | quote}}
BK_CI_ENV: {{ .Values.config.bkCiEnv | quote }}
BK_CI_CONSUL_HTTP_PORT: {{ .Values.config.bkCiConsulHttpPort | quote }}
BK_CI_LOGS_DIR: {{ .Values.config.bkCiLogsDir | quote }}
BK_CI_JFROG_PASSWORD: {{ .Values.config.bkCiJfrogPassword | quote }}
BK_CI_DOCKER_PORT: {{ .Values.config.bkCiDockerPort | quote }}
BK_CI_INFLUXDB_DB: {{ .Values.config.bkCiInfluxdbDb | quote }}
BK_CI_IAM_ENV: {{ .Values.config.bkCiIamEnv | quote }}
BK_CI_JOB_FQDN: {{ .Values.config.bkCiJobFqdn | quote }}
BK_CI_APP_TOKEN: {{ .Values.config.bkCiAppToken | quote }}
{{- end -}}
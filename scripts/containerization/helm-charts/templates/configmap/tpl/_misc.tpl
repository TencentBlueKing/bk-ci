{{- define "bkci.misc.yaml" -}}
# Misc Service Template 服务配置文件模板
spring:
  datasource:
      process:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      project:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_project?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      repository:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_repository?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      dispatch:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_dispatch?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      plugin:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_plugin?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      quality:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_quality?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      artifactory:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_artifactory?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}
      environment:
         url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_environment?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
         username: {{ include "bkci.mysqlUsername" . }}
         password: {{ include "bkci.mysqlPassword" . }}       

influxdb:
  server: http://{{ include "bkci.influxdbHost" . }}:{{ include "bkci.influxdbPort" . }}
  userName: {{ include "bkci.influxdbUsername" . }}
  password: {{ include "bkci.influxdbPassword" . }}
         
# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21927
server:
  port: {{ .Values.config.bkCiMiscApiPort }}
  
build:
  data:
    clear:
      switch: false
      maxEveryProjectHandleNum: 5
      monthRange: -1
      maxKeepNum: 10000  
      codeccDayRange: -14
      codeccMaxKeepNum: 14
      otherMonthRange: -1
      otherMaxKeepNum: 500
      clearChannelCodes: "BS,PREBUILD,CI,CODECC"
      maxThreadHandleProjectNum: 5
{{- end -}}
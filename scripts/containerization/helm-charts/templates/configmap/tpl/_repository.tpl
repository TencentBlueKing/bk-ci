{{- define "bkci.repository.yaml" -}}
# 服务配置文件模板
spring:
  datasource:
    url: jdbc:mysql://{{ include "bkci.mysqlAddr" . }}/devops_ci_repository?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true&sessionVariables=sql_mode=%27STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION%27
    username: {{ include "bkci.mysqlUsername" . }}
    password: {{ include "bkci.mysqlPassword" . }}

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21916
server:
  port: {{ .Values.config.bkCiRepositoryApiPort }}

repository:
  git: # git 管理私有Token，一般不用，用于插件库分组
    devopsPrivateToken: {{ .Values.config.bkCiRepositoryGitPrivateToken }}
    devopsGroupName: {{ .Values.config.bkCiRepositoryGitPluginGroupName }}

scm:
  #svn open api url，如果不需要则放着即可
  svn:
    apiKey: {{ .Values.config.bkCiRepositorySvnApiKey }}
    apiUrl: {{ .Values.config.bkCiRepositorySvnApiUrl }}  # 结尾一般为 /rest
    webhookApiUrl: {{ .Values.config.bkCiRepositorySvnWebhookUrl }}  # 一般为 SVN_API_URL/webhooks
    svnHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/codesvn/commit
  # git 仓库配置，如果不需要则放着即可
  git:
    url: {{ .Values.config.bkCiRepositoryGitUrl }}
    apiUrl: {{ .Values.config.bkCiRepositoryGitUrl }}/api/v3
    clientId: clientId
    clientSecret: Secret
    redirectUrl: {{ .Values.config.bkCiPublicUrl }}/console/codelib
    redirectAtomMarketUrl: {{ .Values.config.bkCiPublicUrl }}/console/store/atomList
    gitHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/codegit/commit
    public:  # TODO 无用配置, 待清理.
      account: devops
      email: devops@devops.com
      secret: devops123
  external:
    #gitlab v4.
    gitlab:
      apiUrl: {{ .Values.config.bkCiRepositoryGitlabUrl }}/api/v4
      gitlabHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/gitlab/commit
    github:
      clientId: {{ .Values.config.bkCiRepositoryGithubClientId }}
      clientSecret: {{ .Values.config.bkCiRepositoryGithubClientSecret }}
      callbackUrl: {{ .Values.config.bkCiPublicUrl }}/repository/api/external/github/oauth/callback
      redirectUrl: {{ .Values.config.bkCiPublicUrl }}/console/codelib
      appUrl: https://github.com/apps/{{ .Values.config.bkCiRepositoryGithubApp }}
      signSecret: {{ .Values.config.bkCiRepositoryGithubSignSecret }}
    tGit:
      apiUrl: https://git.tencent.com/api/v3
      tGitHookUrl: {{ .Values.config.bkCiPublicUrl }}/ms/process/api/external/scm/codetgit/commit
{{- end -}}
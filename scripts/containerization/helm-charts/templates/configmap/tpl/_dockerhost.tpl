{{- define "bkci.dockerhost.yaml" -}}
spring:
  cloud:
    consul:
      discovery:
        register: false
        enabled: false

# 服务器端口配置，在同一台机器上部署多个微服务，端口号要不同 21923
server:
  port: {{ .Values.config.bkCiDockerhostApiPort }}

dockerhost:
  mode: docker_build
  elasticity:
    cpuPeriod: 10000
    cpuQuota: 80000
    cpuThreshold: 80
    memReservation: 34359738368
    memThreshold: 80

# docker client 配置
dockerCli:
  dockerHost: unix:///var/run/docker.sock
  dockerConfig: /root/.docker
  apiVersion: 1.23
  # docker hub 配置
  registryUrl: {{ .Values.config.bkCiDockerRegistryUrl }}
  registryUsername: {{ .Values.config.bkCiDockerRegistryUser }}
  registryPassword: {{ .Values.config.bkCiDockerRegistryPassword }}

  # 以下一般不用修改
  volumeWorkspace: /data/devops/workspace
  volumeApps: /data/devops/apps/
  volumeInit: /data/init.sh
  volumeSleep: /data/devops/sleep.sh
  volumeLogs: /data/devops/logs
  volumeCodecc: /data/devops/codecc/
  volumeProjectShare: /data/devops/share
  volumeMavenRepo: /root/.m2/repository
  volumeNpmPrefix: /root/Downloads/npm/prefix
  volumeNpmCache: /root/Downloads/npm/cache
  volumeNpmRc: /root/.npmrc
  volumeCcache: /root/.ccache
  volumeGradleCache: /root/.gradle/caches
  volumeGolangCache: /root/go/pkg/mod
  volumeSbtCache: /root/.ivy2
  volumeSbt2Cache: /root/.cache
  volumeYarnCache: /usr/local/share/.cache/
  hostPathLinkDir: /tmp/bkci
  hostPathHosts: /etc/hosts

  # docker 母机上的配置，workspace用于存放构建的工作空间，可以以挂载的方式提供
  hostPathWorkspace: {{ .Values.config.bkCiDataDir }}/docker/workspace
  hostPathApps: {{ .Values.config.bkCiDataDir }}/docker/apps/
  hostPathInit: {{ .Values.config.bkCiHome }}/agent-package/script/init.sh
  hostPathSleep: {{ .Values.config.bkCiHome }}/agent-package/script/sleep.sh
  hostPathLogs: {{ .Values.config.bkCiLogsDir }}/docker
  hostPathCodecc: /data/devops/codecc
  hostPathProjectShare: {{ .Values.config.bkCiDataDir }}/docker/share/project
  hostPathMavenRepo: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/maven_repo
  hostPathNpmPrefix: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/npm_prefix
  hostPathNpmCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/npm_cache
  hostPathNpmRc: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/.npmrc
  hostPathCcache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/ccache/
  hostPathGradleCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/gradle_caches
  hostPathGolangCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/go_cache/
  hostPathSbtCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/.ivy2/
  hostPathSbt2Cache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/sbt_cache/
  hostPathYarnCache: {{ .Values.config.bkCiDataDir }}/docker/thirdparty/yarn_cache/
  # 需要共享的项目
  shareProjectCodeWhiteList:
  # docker.jar 存储路径
  dockerAgentPath: {{ .Values.config.bkCiHome }}/agent-package/worker-agent.jar
  downloadDockerAgentUrl: {{ .Values.config.bkCiPrivateUrl }}/ms/dispatch/gw/build/docker.jar
  # 定期下载
  downloadAgentCron: 0 0 3 * * ?
  landunEnv: prod
  clearLocalImageCron: 0 0 2 * * ?
  localImageCacheDays: 7
{{- end -}}
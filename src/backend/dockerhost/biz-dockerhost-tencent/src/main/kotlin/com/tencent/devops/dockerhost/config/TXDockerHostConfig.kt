package com.tencent.devops.dockerhost.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TXDockerHostConfig {

    @Value("\${dockerHost}")
    val dockerHost: String? = null

    @Value("\${dockerConfig}")
    var dockerConfig: String? = null

    @Value("\${apiVersion}")
    var apiVersion: String? = null

    @Value("\${registryUrl}")
    var registryUrl: String? = null

    @Value("\${registryUsername}")
    var registryUsername: String? = null

    @Value("\${registryPassword}")
    var registryPassword: String? = null

    @Value("\${volumeWorkspace}")
    var volumeWorkspace: String? = null

    @Value("\${volumeProjectShare}")
    var volumeProjectShare: String? = null

    @Value("\${volumeMavenRepo}")
    var volumeMavenRepo: String? = null

    @Value("\${volumeNpmPrefix}")
    var volumeNpmPrefix: String? = null

    @Value("\${volumeNpmCache}")
    var volumeNpmCache: String? = null

    @Value("\${volumeNpmRc}")
    var volumeNpmRc: String? = null

    @Value("\${volumeCcache}")
    var volumeCcache: String? = null

    @Value("\${volumeApps}")
    var volumeApps: String? = null

    @Value("\${volumeInit}")
    var volumeInit: String? = null

    @Value("\${volumeSleep}")
    var volumeSleep: String? = null

    @Value("\${volumeLogs}")
    var volumeLogs: String? = null

    @Value("\${volumeGradleCache}")
    var volumeGradleCache: String? = null

    @Value("\${hostPathWorkspace}")
    var hostPathWorkspace: String? = null

    @Value("\${hostPathProjectShare}")
    var hostPathProjectShare: String? = null

    @Value("\${hostPathMavenRepo}")
    var hostPathMavenRepo: String? = null

    @Value("\${hostPathNpmPrefix}")
    var hostPathNpmPrefix: String? = null

    @Value("\${hostPathNpmCache}")
    var hostPathNpmCache: String? = null

    @Value("\${hostPathNpmRc}")
    var hostPathNpmRc: String? = null

    @Value("\${hostPathCcache}")
    var hostPathCcache: String? = null

    @Value("\${hostPathApps}")
    var hostPathApps: String? = null

    @Value("\${hostPathInit}")
    var hostPathInit: String? = null

    @Value("\${hostPathSleep}")
    var hostPathSleep: String? = null

    @Value("\${hostPathLogs}")
    var hostPathLogs: String? = null

    @Value("\${hostPathGradleCache}")
    var hostPathGradleCache: String? = null

    @Value("\${hostPathLinkDir}")
    var hostPathLinkDir: String = "/tmp/bkci"

    @Value("\${hostPathHosts}")
    var hostPathHosts: String? = null

    @Value("\${shareProjectCodeWhiteList}")
    var shareProjectCodeWhiteList: String? = null

    @Value("\${memoryLimitBytes:2147483648}")
    var memory: Long = 2147483648L // 1024 * 1024 * 1024 * 2 Memory limit in bytes. 2048MB

    @Value("\${cpuPeriod:50000}")
    var cpuPeriod: Int = 50000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${cpuQuota:50000}")
    var cpuQuota: Int = 50000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dockerAgentPath}")
    var dockerAgentPath: String? = null

    @Value("\${downloadDockerAgentUrl}")
    var downloadDockerAgentUrl: String? = null

    @Value("\${downloadAgentCron}")
    var downloadAgentCron: String? = null

    @Value("\${landunEnv}")
    var landunEnv: String? = null

    @Value("\${localImageCacheDays}")
    var localImageCacheDays: Int = 7

    @Value("\${run.mode:#{null}}")
    var runMode: String? = null
}
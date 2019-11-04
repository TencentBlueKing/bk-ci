/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
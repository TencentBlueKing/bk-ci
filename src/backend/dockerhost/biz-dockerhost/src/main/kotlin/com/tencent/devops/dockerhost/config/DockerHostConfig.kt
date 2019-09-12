/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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
class DockerHostConfig {

    @Value("\${dockerCli.dockerHost:unix:///var/run/docker.sock}")
    val dockerHost: String? = null

    @Value("\${dockerCli.dockerConfig:/root/.docke}")
    var dockerConfig: String? = null

    @Value("\${dockerCli.apiVersion:1.23}")
    var apiVersion: String? = null

    @Value("\${dockerCli.registryUrl}")
    var registryUrl: String? = null

    @Value("\${dockerCli.registryUsername}")
    var registryUsername: String? = null

    @Value("\${dockerCli.registryPassword}")
    var registryPassword: String? = null

    @Value("\${dockerCli.volumeWorkspace:/data/devops/workspace}")
    var volumeWorkspace: String? = null

    @Value("\${dockerCli.volumeProjectShare:/data/devops/share}")
    var volumeProjectShare: String? = null

    @Value("\${dockerCli.volumeMavenRepo:/root/.m2/repository}")
    var volumeMavenRepo: String? = null

    @Value("\${dockerCli.volumeNpmPrefix:/root/Downloads/npm/prefix}")
    var volumeNpmPrefix: String? = null

    @Value("\${dockerCli.volumeNpmCache:root/Downloads/npm/cache}")
    var volumeNpmCache: String? = null

    @Value("\${dockerCli.volumeNpmRc://root/.npmrc}")
    var volumeNpmRc: String? = null

    @Value("\${dockerCli.volumeCcache:/root/.ccache}")
    var volumeCcache: String? = null

    @Value("\${dockerCli.volumeApps:/data/devops/apps/}")
    var volumeApps: String? = null

    @Value("\${dockerCli.volumeCodecc}")
    var volumeCodecc: String? = null

    @Value("\${dockerCli.volumeInit:/data/init.sh}")
    var volumeInit: String? = null

    @Value("\${dockerCli.volumeSleep:/data/sleep.sh}")
    var volumeSleep: String? = null

    @Value("\${dockerCli.volumeLogs:/data/logs/}")
    var volumeLogs: String? = null

    @Value("\${dockerCli.volumeGradleCache:/root/.gradle/caches}")
    var volumeGradleCache: String? = null

    @Value("\${dockerCli.hostPathWorkspace}")
    var hostPathWorkspace: String? = null

    @Value("\${dockerCli.hostPathProjectShare}")
    var hostPathProjectShare: String? = null

    @Value("\${dockerCli.hostPathMavenRepo}")
    var hostPathMavenRepo: String? = null

    @Value("\${dockerCli.hostPathNpmPrefix}")
    var hostPathNpmPrefix: String? = null

    @Value("\${dockerCli.hostPathNpmCache}")
    var hostPathNpmCache: String? = null

    @Value("\${dockerCli.hostPathNpmRc}")
    var hostPathNpmRc: String? = null

    @Value("\${dockerCli.hostPathCcache}")
    var hostPathCcache: String? = null

    @Value("\${dockerCli.hostPathApps}")
    var hostPathApps: String? = null

    @Value("\${dockerCli.hostPathCodecc}")
    var hostPathCodecc: String? = null

    @Value("\${dockerCli.hostPathInit}")
    var hostPathInit: String? = null

    @Value("\${dockerCli.hostPathSleep}")
    var hostPathSleep: String? = null

    @Value("\${dockerCli.hostPathLogs}")
    var hostPathLogs: String? = null

    @Value("\${dockerCli.hostPathGradleCache}")
    var hostPathGradleCache: String? = null

    @Value("\${dockerCli.shareProjectCodeWhiteList:}")
    var shareProjectCodeWhiteList: String? = null

    @Value("\${dockerCli.memoryLimitBytes:2147483648}")
    var memory: Long = 2147483648L // 1024 * 1024 * 1024 * 2 Memory limit in bytes. 2048MB

    @Value("\${dockerCli.cpuPeriod:50000}")
    var cpuPeriod: Int = 50000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dockerCli.cpuQuota:50000}")
    var cpuQuota: Int = 50000 // Limit the CPU CFS (Completely Fair Scheduler) period
}
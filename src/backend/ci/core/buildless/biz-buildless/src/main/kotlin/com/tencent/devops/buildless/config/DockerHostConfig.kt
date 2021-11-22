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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.buildless.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DockerHostConfig {

    @Value("\${dockerCli.dockerConfig:/root/.docke}")
    var dockerConfig: String? = null

    @Value("\${dockerCli.apiVersion:1.23}")
    var apiVersion: String? = null

    @Value("\${dockerCli.volumeApps:/data/devops/apps/}")
    var volumeApps: String? = null

    @Value("\${dockerCli.volumeInit:/data/init.sh}")
    var volumeInit: String? = null

    @Value("\${dockerCli.volumeSleep:/data/sleep.sh}")
    var volumeSleep: String? = null

    @Value("\${dockerCli.hostPathApps:#{null}}")
    var hostPathApps: String? = null

    @Value("\${dockerCli.hostPathInit:#{null}}")
    var hostPathInit: String? = null

    @Value("\${dockerCli.hostPathSleep:#{null}}")
    var hostPathSleep: String? = null

    @Value("\${dockerCli.memoryLimitBytes:34359738368}")
    var memory: Long = 34359738368L // 1024 * 1024 * 1024 * 32 Memory limit in bytes. 32G

    @Value("\${dockerCli.cpuPeriod:10000}")
    var cpuPeriod: Int = 10000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dockerCli.cpuQuota:160000}")
    var cpuQuota: Int = 160000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dockerCli.dockerAgentPath}")
    var dockerAgentPath: String? = null

    @Value("\${dockerCli.downloadDockerAgentUrl}")
    var downloadDockerAgentUrl: String? = null

    @Value("\${gateway:#{null}")
    var gateway: String? = null
}

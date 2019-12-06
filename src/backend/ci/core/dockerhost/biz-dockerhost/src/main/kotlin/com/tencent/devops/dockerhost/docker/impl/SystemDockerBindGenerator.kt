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

package com.tencent.devops.dockerhost.docker.impl

import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.docker.DockerBindGenerator
import com.tencent.devops.dockerhost.docker.annotation.BindGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

@BindGenerator(description = "默认Docker Bind生成器")
@Component
class SystemDockerBindGenerator @Autowired constructor(private val dockerHostConfig: DockerHostConfig) :
    DockerBindGenerator {

//    private val etcHosts = "/etc/hosts"

    private val whiteList = mutableSetOf<String>()

    private val whiteListLocker = ReentrantLock()

    override fun generateBinds(dockerHostBuildInfo: DockerHostBuildInfo): List<Bind> {
        with(dockerHostBuildInfo) {

            val binds = mutableListOf(
                Bind(getMavenRepoPath(), Volume(dockerHostConfig.volumeMavenRepo)),
                Bind(getNpmPrefixPath(), Volume(dockerHostConfig.volumeNpmPrefix)),
                Bind(getNpmCachePath(), Volume(dockerHostConfig.volumeNpmCache)),
                Bind(getCcachePath(), Volume(dockerHostConfig.volumeCcache)),
                Bind(dockerHostConfig.hostPathApps, Volume(dockerHostConfig.volumeApps), AccessMode.ro),
                Bind(dockerHostConfig.hostPathInit, Volume(dockerHostConfig.volumeInit), AccessMode.ro),
//                Bind(etcHosts, Volume(etcHosts), AccessMode.ro),
                Bind(getLogsPath(), Volume(dockerHostConfig.volumeLogs)),
                Bind(getGradlePath(), Volume(dockerHostConfig.volumeGradleCache)),
                Bind(getWorkspace(), Volume(dockerHostConfig.volumeWorkspace))
            )

            if (enableProjectShare(projectId)) {
                binds.add(Bind(getProjectShareDir(projectId), Volume(dockerHostConfig.volumeProjectShare)))
            }

            return binds
        }
    }

    private fun DockerHostBuildInfo.getGradlePath(): String {
        return "${dockerHostConfig.hostPathGradleCache}/$pipelineId/$vmSeqId/"
    }

    private fun DockerHostBuildInfo.getLogsPath(): String {
        return "${dockerHostConfig.hostPathLogs}/$buildId/$vmSeqId/"
    }

    private fun DockerHostBuildInfo.getCcachePath(): String {
        return "${dockerHostConfig.hostPathCcache}/$pipelineId/$vmSeqId/"
    }

    private fun DockerHostBuildInfo.getNpmCachePath(): String {
        return "${dockerHostConfig.hostPathNpmCache}/$pipelineId/$vmSeqId/"
    }

    private fun DockerHostBuildInfo.getNpmPrefixPath(): String {
        return "${dockerHostConfig.hostPathNpmPrefix}/$pipelineId/$vmSeqId/"
    }

    private fun DockerHostBuildInfo.getMavenRepoPath(): String {
        return "${dockerHostConfig.hostPathMavenRepo}/$pipelineId/$vmSeqId/"
    }

    private fun DockerHostBuildInfo.getWorkspace(): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/$vmSeqId/"
    }

    private fun getProjectShareDir(projectCode: String): String {
        return "${dockerHostConfig.hostPathProjectShare}/$projectCode/"
    }

    private fun enableProjectShare(projectCode: String): Boolean {

        if (dockerHostConfig.shareProjectCodeWhiteList.isNullOrBlank()) {
            return false
        }

        if (whiteList.isEmpty()) {
            try {
                whiteListLocker.tryLock(1000, TimeUnit.MILLISECONDS)
                if (whiteList.isEmpty()) {
                    whiteList.addAll(dockerHostConfig.shareProjectCodeWhiteList!!.split(",").map { it.trim() })
                }
            } catch (ingored: InterruptedException) {
            } finally {
                whiteListLocker.unlock()
            }
        }

        return whiteList.contains(projectCode)
    }
}
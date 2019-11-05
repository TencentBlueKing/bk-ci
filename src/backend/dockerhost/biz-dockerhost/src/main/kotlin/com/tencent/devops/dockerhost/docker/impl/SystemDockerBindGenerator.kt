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

    private val etcHosts = "/etc/hosts"

    private val whiteList = mutableSetOf<String>()

    private val whiteListLocker = ReentrantLock()

    override fun generateBinds(dockerHostBuildInfo: DockerHostBuildInfo): List<Bind> {
        val volumeWs = Volume(dockerHostConfig.volumeWorkspace)
        val volumeProjectShare = Volume(dockerHostConfig.volumeProjectShare)
        val volumeMavenRepo = Volume(dockerHostConfig.volumeMavenRepo)
        val volumeNpmPrefix = Volume(dockerHostConfig.volumeNpmPrefix)
        val volumeNpmCache = Volume(dockerHostConfig.volumeNpmCache)
        val volumeCcache = Volume(dockerHostConfig.volumeCcache)
        val volumeApps = Volume(dockerHostConfig.volumeApps)
        val volumeInit = Volume(dockerHostConfig.volumeInit)
        val volumeLogs = Volume(dockerHostConfig.volumeLogs)
        val volumeGradleCache = Volume(dockerHostConfig.volumeGradleCache)
        val volumeHosts = Volume(etcHosts)

        val binds = mutableListOf(
            Bind(getMavenRepoPath(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeMavenRepo),
            Bind(getNpmPrefixPath(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeNpmPrefix),
            Bind(getNpmCachePath(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeNpmCache),
            Bind(getCcachePath(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeCcache),
            Bind(dockerHostConfig.hostPathApps, volumeApps, AccessMode.ro),
            Bind(dockerHostConfig.hostPathInit, volumeInit, AccessMode.ro),
            Bind(etcHosts, volumeHosts, AccessMode.ro),
            Bind(getLogsPath(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeLogs),
            Bind(getGradlePath(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeGradleCache),
            Bind(getWorkspace(dockerHostBuildInfo.pipelineId, dockerHostBuildInfo.vmSeqId), volumeWs)
        )

        if (enableProjectShare(dockerHostBuildInfo.projectId)) {
            binds.add(Bind(getProjectShareDir(dockerHostBuildInfo.projectId), volumeProjectShare))
        }
        return binds
    }

    private fun getGradlePath(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathGradleCache}/$pipelineId/$vmSeqId/"
    }

    private fun getLogsPath(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathLogs}/$pipelineId/$vmSeqId/"
    }

    private fun getCcachePath(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathCcache}/$pipelineId/$vmSeqId/"
    }

    private fun getNpmCachePath(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathNpmCache}/$pipelineId/$vmSeqId/"
    }

    private fun getNpmPrefixPath(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathNpmPrefix}/$pipelineId/$vmSeqId/"
    }

    private fun getMavenRepoPath(pipelineId: String, vmSeqId: Int): String {
        return "${dockerHostConfig.hostPathMavenRepo}/$pipelineId/$vmSeqId/"
    }

    private fun getWorkspace(pipelineId: String, vmSeqId: Int): String {
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
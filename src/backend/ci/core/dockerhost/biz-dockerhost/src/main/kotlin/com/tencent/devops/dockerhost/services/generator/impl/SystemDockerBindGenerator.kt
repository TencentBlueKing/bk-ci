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

package com.tencent.devops.dockerhost.services.generator.impl

import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.services.container.ContainerHandlerContext
import com.tencent.devops.dockerhost.services.generator.DockerBindGenerator
import com.tencent.devops.dockerhost.services.generator.annotation.BindGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@BindGenerator(description = "默认Docker Bind生成器")
@Component
class SystemDockerBindGenerator @Autowired constructor(private val dockerHostConfig: DockerHostConfig) :
    DockerBindGenerator {
    override fun generateBinds(handlerContext: ContainerHandlerContext): List<Bind> {
        with(handlerContext) {
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
                Bind(getGolangPath(), Volume(dockerHostConfig.volumeGolangCache)),
                Bind(getSbtPath(), Volume(dockerHostConfig.volumeSbtCache)),
                Bind(getSbt2Path(), Volume(dockerHostConfig.volumeSbt2Cache)),
                Bind(getYarnPath(), Volume(dockerHostConfig.volumeYarnCache)),
                Bind(dockerHostConfig.hostPathCommonEnv, Volume(dockerHostConfig.volumeCommonEnv), AccessMode.rw)
            )

            if (qpcUniquePath.isNullOrBlank()) {
                binds.add(Bind(getWorkspace(), Volume(dockerHostConfig.volumeWorkspace)))
            }

            if (enableProjectShare(specialProjectList, projectId)) {
                binds.add(Bind(getProjectShareDir(projectId), Volume(dockerHostConfig.volumeProjectShare)))
            }

            return binds
        }
    }

    private fun ContainerHandlerContext.getGradlePath(): String {
        return "${dockerHostConfig.hostPathGradleCache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getGolangPath(): String {
        return "${dockerHostConfig.hostPathGolangCache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getSbtPath(): String {
        return "${dockerHostConfig.hostPathSbtCache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getSbt2Path(): String {
        return "${dockerHostConfig.hostPathSbt2Cache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getYarnPath(): String {
        return "${dockerHostConfig.hostPathYarnCache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getLogsPath(): String {
        return "${dockerHostConfig.hostPathLogs}/$buildId/$vmSeqId/"
    }

    private fun ContainerHandlerContext.getCcachePath(): String {
        return "${dockerHostConfig.hostPathCcache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getNpmCachePath(): String {
        return "${dockerHostConfig.hostPathNpmCache}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getNpmPrefixPath(): String {
        return "${dockerHostConfig.hostPathNpmPrefix}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getMavenRepoPath(): String {
        return "${dockerHostConfig.hostPathMavenRepo}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun ContainerHandlerContext.getWorkspace(): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun getProjectShareDir(projectCode: String): String {
        return "${dockerHostConfig.hostPathProjectShare}/$projectCode/"
    }

    private fun enableProjectShare(
        specialProjectList: String?,
        projectId: String
    ): Boolean {
        if (!specialProjectList.isNullOrBlank()) {
            return specialProjectList.split(",").map { it.trim() }.contains(projectId)
        }

        if (dockerHostConfig.shareProjectCodeWhiteList.isNullOrBlank()) {
            return dockerHostConfig.shareProjectCodeWhiteList!!
                .split(",")
                .map { it.trim() }
                .contains(projectId)
        }

        return false
    }

    private fun getTailPath(vmSeqId: Int, poolNo: Int): String {
        return if (poolNo > 1) {
            "$vmSeqId" + "_$poolNo"
        } else {
            vmSeqId.toString()
        }
    }
}

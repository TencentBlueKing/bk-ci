/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.github.dockerjava.api.model.Driver
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.VolumeOptions
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.services.generator.DockerMountGenerator
import com.tencent.devops.dockerhost.services.container.ContainerHandlerContext
import com.tencent.devops.dockerhost.services.generator.annotation.MountGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@MountGenerator(description = "默认Docker Mount生成器")
@Component
class SystemDockerMountGenerator @Autowired constructor(private val dockerHostConfig: DockerHostConfig) :
    DockerMountGenerator {

    override fun generateMounts(handlerContext: ContainerHandlerContext): List<Mount> {
        with(handlerContext) {
            val mountList = mutableListOf<Mount>()
            if (qpcUniquePath != null && qpcUniquePath.isNotBlank()) {
                mountList.add(getBazelOverlayfsMount(
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo
                ))

                val upperDir = "${getWorkspace(buildId, vmSeqId, poolNo, dockerHostConfig.hostPathWorkspace!!)}upper"
                val workDir = "${getWorkspace(buildId, vmSeqId, poolNo, dockerHostConfig.hostPathWorkspace!!)}work"
                val lowerDir = "${dockerHostConfig.hostPathOverlayfsCache}/$qpcUniquePath"

                mountList.add(mountOverlayfs(
                    lowerDir = lowerDir,
                    upperDir = upperDir,
                    workDir = workDir,
                    targetPath = dockerHostConfig.volumeWorkspace
                ))
            }

            return mountList
        }
    }

    private fun getBazelOverlayfsMount(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int
    ): Mount {
        return mountOverlayfs(
            lowerDir = "${dockerHostConfig.bazelLowerPath}",
            upperDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.bazelUpperPath!!)}upper",
            workDir = "${getWorkspace(pipelineId, vmSeqId, poolNo, dockerHostConfig.bazelUpperPath!!)}work",
            targetPath = dockerHostConfig.bazelContainerPath
        )
    }

    private fun mountOverlayfs(
        lowerDir: String,
        upperDir: String,
        workDir: String,
        targetPath: String?
    ): Mount {
        if (!File(upperDir).exists()) {
            File(upperDir).mkdirs()
        }

        if (!File(workDir).exists()) {
            File(workDir).mkdirs()
        }

        if (!File(lowerDir).exists()) {
            File(lowerDir).mkdirs()
        }

        return Mount().withType(MountType.VOLUME)
            .withTarget(targetPath)
            .withVolumeOptions(
                VolumeOptions().withDriverConfig(
                    Driver().withName("local").withOptions(
                        mapOf(
                            "type" to "overlay",
                            "device" to "overlay",
                            "o" to "lowerdir=$lowerDir,upperdir=$upperDir,workdir=$workDir"
                        )
                    )
                )
            )
    }

    private fun getWorkspace(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int,
        path: String
    ): String {
        return "$path/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun getTailPath(vmSeqId: Int, poolNo: Int): String {
        return if (poolNo > 1) {
            "$vmSeqId" + "_$poolNo"
        } else {
            vmSeqId.toString()
        }
    }
}

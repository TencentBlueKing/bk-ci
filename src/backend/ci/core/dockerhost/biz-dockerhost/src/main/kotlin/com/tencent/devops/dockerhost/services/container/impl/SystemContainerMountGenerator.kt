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

package com.tencent.devops.dockerhost.services.container.impl

import com.github.dockerjava.api.model.Driver
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.VolumeOptions
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.services.container.DockerMountGenerator
import com.tencent.devops.dockerhost.services.container.ContainerHandlerContext
import com.tencent.devops.dockerhost.services.container.annotation.ContainerMountGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.File

@ContainerMountGenerator(description = "默认Docker Mount生成器")
@Component
class SystemContainerMountGenerator @Autowired constructor(private val dockerHostConfig: DockerHostConfig) :
    DockerMountGenerator {

    override fun generateMounts(handlerContext: ContainerHandlerContext): List<Mount> {
        with(handlerContext) {
            if (qpcUniquePath != null && qpcUniquePath.isNotBlank()) {
                val upperDir = "${getWorkspace(pipelineId, vmSeqId, poolNo)}upper"
                val workDir = "${getWorkspace(pipelineId, vmSeqId, poolNo)}work"
                val lowerDir = "${dockerHostConfig.hostPathOverlayfsCache}/$qpcUniquePath"

                if (!File(upperDir).exists()) {
                    File(upperDir).mkdirs()
                }

                if (!File(workDir).exists()) {
                    File(workDir).mkdirs()
                }

                if (!File(lowerDir).exists()) {
                    File(lowerDir).mkdirs()
                }

                val mount = Mount().withType(MountType.VOLUME)
                    .withTarget(dockerHostConfig.volumeWorkspace)
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

                return listOf(mount)
            }
        }

        return emptyList()
    }

    private fun getWorkspace(
        pipelineId: String,
        vmSeqId: Int,
        poolNo: Int
    ): String {
        return "${dockerHostConfig.hostPathWorkspace}/$pipelineId/${getTailPath(vmSeqId, poolNo)}/"
    }

    private fun getTailPath(vmSeqId: Int, poolNo: Int): String {
        return if (poolNo > 1) {
            "$vmSeqId" + "_$poolNo"
        } else {
            vmSeqId.toString()
        }
    }
}

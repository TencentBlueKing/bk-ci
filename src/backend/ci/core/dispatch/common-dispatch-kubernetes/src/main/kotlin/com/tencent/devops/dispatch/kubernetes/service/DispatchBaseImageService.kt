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

package com.tencent.devops.dispatch.kubernetes.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.dispatch.kubernetes.pojo.CheckDockerImageRequest
import com.tencent.devops.dispatch.kubernetes.pojo.CheckDockerImageResponse
import com.tencent.devops.dispatch.kubernetes.pojo.Credential
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.image.InspectImageResp
import com.tencent.devops.dispatch.kubernetes.service.factory.ContainerServiceFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DispatchBaseImageService @Autowired constructor(
    private val containerServiceFactory: ContainerServiceFactory
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchBaseImageService::class.java)
    }

    fun checkDockerImage(
        userId: String,
        checkDockerImageRequestList: List<CheckDockerImageRequest>
    ): List<CheckDockerImageResponse> {
        val imageInspectList = mutableListOf<CheckDockerImageResponse>()
        checkDockerImageRequestList.parallelStream().forEach {
            // 拉镜像
            val taskId = containerServiceFactory.load("").inspectDockerImage(
                userId = userId,
                pool = Pool(
                    container = it.imageName,
                    credential = Credential(
                        user = it.registryUser ?: "",
                        password = it.registryPwd ?: ""
                    )
                )
            )

            val taskResult = containerServiceFactory.load("").waitTaskFinish(userId, taskId, false)
            if (taskResult.status == DispatchBuildTaskStatusEnum.SUCCEEDED) {
                logger.info("CheckDockerImage $userId pull ${it.imageName} success.")
                val inspectImageResp = JsonUtil.to(taskResult.msg ?: "", InspectImageResp::class.java)
                imageInspectList.add(
                    CheckDockerImageResponse(
                        errorCode = 0,
                        errorMessage = "",
                        arch = inspectImageResp.arch,
                        author = inspectImageResp.author,
                        created = inspectImageResp.created,
                        id = inspectImageResp.id,
                        os = inspectImageResp.os,
                        osVersion = inspectImageResp.osVersion,
                        parent = inspectImageResp.parent,
                        size = inspectImageResp.size
                    )
                )
            } else {
                imageInspectList.add(
                    CheckDockerImageResponse(
                        errorCode = -1,
                        errorMessage = taskResult.msg,
                        arch = "",
                        author = "",
                        created = "",
                        id = "",
                        os = "",
                        osVersion = "",
                        parent = "",
                        size = 0
                    )
                )

                return@forEach
            }
        }

        return imageInspectList
    }
}

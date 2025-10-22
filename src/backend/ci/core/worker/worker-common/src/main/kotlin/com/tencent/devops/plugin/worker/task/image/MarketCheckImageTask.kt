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

package com.tencent.devops.plugin.worker.task.image

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.market.MarketCheckImageElement
import com.tencent.devops.image.pojo.CheckDockerImageRequest
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.docker.DockerSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import java.io.File
import org.slf4j.LoggerFactory

@TaskClassType(classTypes = [MarketCheckImageElement.classType])
class MarketCheckImageTask : ITask() {

    private val logger = LoggerFactory.getLogger(MarketCheckImageTask::class.java)

    private val dockerApi = ApiFactory.create(DockerSDKApi::class)

    @Suppress("ALL")
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("MarketCheckImageTask buildTask: $buildTask,buildVariables: $buildVariables")
        LoggerService.addNormalLine("begin check image")
        val buildVariableMap = buildTask.buildVariable!!
        val imageCode = buildVariableMap["imageCode"]
        val imageName = buildVariableMap["imageName"]!!
        val registryHost = buildVariableMap["registryHost"]
        val registryUser = buildVariableMap["registryUser"]
        val registryPwd = buildVariableMap["registryPwd"]
        val userId = buildVariableMap[PIPELINE_START_USER_ID] ?: ""
        val checkImageResult = dockerApi.checkDockerImage(
            userId = userId,
            checkDockerImageRequestList = arrayOf(
                CheckDockerImageRequest(
                    imageName = imageName,
                    registryHost = registryHost ?: "",
                    registryUser = registryUser,
                    registryPwd = registryPwd
                )
            )
        )
        val checkImageResponse =
        if (checkImageResult.isOk() && checkImageResult.data!!.isNotEmpty()) {
            checkImageResult.data!![0]
        } else {
            LoggerService.addErrorLine(JsonUtil.toJson(checkImageResult))
            throw TaskExecuteException(
                errorMsg = "checkImage fail: ${checkImageResult.message}",
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER
            )
        }
        if (checkImageResponse.errorCode == -1) {
            LoggerService.addErrorLine(JsonUtil.toJson(checkImageResponse))
            throw TaskExecuteException(
                errorMsg = "checkImage fail: ${checkImageResponse.errorMessage}",
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER
            )
        }
        val imageVersion = buildVariableMap["version"]
        // 获取镜像大小上送至商店
        val updateImageResult = dockerApi.updateImageInfo(
            userId = userId,
            projectCode = buildVariables.projectId,
            imageCode = imageCode!!,
            version = imageVersion!!,
            imageBaseInfoUpdateRequest = ImageBaseInfoUpdateRequest(
                imageSize = checkImageResponse.size.toString()
            )
        )
        logger.info("MarketCheckImageTask updateImageResult: $updateImageResult")
        if (updateImageResult.isNotOk()) {
            LoggerService.addErrorLine(JsonUtil.toJson(updateImageResult))
            throw TaskExecuteException(
                errorMsg = "updateImage fail: ${updateImageResult.message}",
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER
            )
        }
        LoggerService.addNormalLine("check image success")
    }
}

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

package com.tencent.devops.plugin.worker.task.image

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.pojo.element.market.MarketCheckImageElement
import com.tencent.devops.dockerhost.pojo.CheckImageRequest
import com.tencent.devops.dockerhost.pojo.CheckImageResponse
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.docker.DockerSDKApi
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_IP
import com.tencent.devops.dockerhost.utils.ENV_DOCKER_HOST_PORT
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

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
        val imageName = buildVariableMap["imageName"]
        val imageType = buildVariableMap["imageType"]
        val registryUser = buildVariableMap["registryUser"]
        val registryPwd = buildVariableMap["registryPwd"]
        val checkImageRequest = CheckImageRequest(imageType, imageName!!, registryUser, registryPwd)
        val dockerHostIp = System.getenv(ENV_DOCKER_HOST_IP)
        val dockerHostPort = System.getenv(ENV_DOCKER_HOST_PORT)
        val path = "/api/docker/build/image/buildIds/${buildTask.buildId}/" +
                "check?containerId=${buildVariables.containerId}&containerHashId=${buildVariables.containerHashId}"
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            JsonUtil.toJson(checkImageRequest)
        )
        val url = "http://$dockerHostIp:$dockerHostPort$path"
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val response = OkhttpUtils.doLongHttp(request)
        val responseContent = response.body?.string()
        if (!response.isSuccessful) {
            logger.warn(
                "Fail to request($request) with code ${response.code} ," +
                        " message ${response.message} and response ($responseContent)"
            )
            LoggerService.addErrorLine(response.message)
            throw TaskExecuteException(
                errorMsg = "checkImage fail: message ${response.message} and response ($responseContent)",
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER
            )
        }
        val checkImageResult = JsonUtil.to(responseContent!!, object : TypeReference<Result<CheckImageResponse?>>() {
        })
        LoggerService.addNormalLine("checkImageResult: $checkImageResult")
        if (checkImageResult.isNotOk()) {
            LoggerService.addErrorLine(JsonUtil.toJson(checkImageResult))
            throw TaskExecuteException(
                errorMsg = "checkImage fail: ${checkImageResult.message}",
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                errorType = ErrorType.USER
            )
        }
        val imageVersion = buildVariableMap["version"]
        // 获取镜像大小上送至商店
        val checkImageResponse = checkImageResult.data
        val userId = buildVariableMap[PIPELINE_START_USER_ID]
        val updateImageResult = dockerApi.updateImageInfo(
            userId = userId!!,
            projectCode = buildVariables.projectId,
            imageCode = imageCode!!,
            version = imageVersion!!,
            imageBaseInfoUpdateRequest = ImageBaseInfoUpdateRequest(
                imageSize = checkImageResponse?.size.toString()
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

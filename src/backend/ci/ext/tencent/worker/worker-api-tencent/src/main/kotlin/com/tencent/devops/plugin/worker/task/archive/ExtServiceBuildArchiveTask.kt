/*
 *
 *  * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *  *
 *  * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *  *
 *  * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *  *
 *  * A copy of the MIT License is included in this file.
 *  *
 *  *
 *  * Terms of the MIT License:
 *  * ---------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 *  * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 *  * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  * Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.devops.plugin.worker.task.archive

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.element.market.ExtServiceBuildArchiveElement
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.dockerhost.pojo.CheckImageResponse
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

@TaskClassType(classTypes = [ExtServiceBuildArchiveElement.classType])
class ExtServiceBuildArchiveTask : ITask() {

    private val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)

    private val logger = LoggerFactory.getLogger(ExtServiceBuildArchiveTask::class.java)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("ExtServiceBuildArchiveTask buildTask: $buildTask,buildVariables: $buildVariables")
        val buildId = buildTask.buildId
        LoggerService.addNormalLine("buildId:$buildId begin archive extService package")
        val buildVariableMap = buildTask.buildVariable!!
        val serviceCode = buildVariableMap["serviceCode"] ?: throw TaskExecuteException(
            errorMsg = "param [serviceCode] is empty",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
        val serviceVersion = buildVariableMap["version"] ?: throw TaskExecuteException(
            errorMsg = "param [version] is empty",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
        val taskParams = buildTask.params ?: mapOf()
        val filePath = taskParams["filePath"] ?: throw TaskExecuteException(
            errorMsg = "param [filePath] is empty",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
        val destPath = taskParams["destPath"] ?: throw TaskExecuteException(
            errorMsg = "param [destPath] is empty",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
        //  开始上传扩展服务执行包到蓝盾新仓库
        val file = File(filePath)
        val mediaType = MediaType.parse("application/octet-stream")
        val fileBody = RequestBody.create(mediaType, file)
        val uploadFileRequest = Request.Builder().url("${HomeHostUtil.bkrepoApiUrl()}/bk-extension/generic-local/$destPath")
            .header("Authorization", Credentials.basic("bk_extension", "blueking"))
            .header("X-BKREPO-OVERWRITE", "true")
            .put(fileBody)
            .build()
        OkhttpUtils.doLongHttp(uploadFileRequest).use { response ->
            val responseContent = response.body()?.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($uploadFileRequest) with code ${response.code()} , message ${response.message()} and response ($responseContent)")
                LoggerService.addRedLine(response.message())
                throw TaskExecuteException(
                    errorMsg = "archive extService package fail: message ${response.message()} and response ($responseContent)",
                    errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
                )
            }
        }
        // 开始构建扩展服务的镜像并把镜像推送到新仓库(基础镜像是否只能用蓝盾提供的？)
        val dockerBuildParam = DockerBuildParam(
            repoAddr = "docker.dev.bkrepo.oa.com",
            imageName = "bk-extension/docker-local/$serviceCode",
            imageTag = serviceVersion,
            userName = "bk_extension",
            password = "blueking"
        )
        val dockerHostIp = System.getenv("docker_host_ip")
        val path =
            "/api/dockernew/${buildVariables.projectId}/${buildVariables.pipelineId}/${buildVariables.vmSeqId}/${buildTask.buildId}?elementId=${buildTask.elementId}"
        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JsonUtil.toJson(dockerBuildParam)
        )
        val url = "http://$dockerHostIp$path"
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val response = OkhttpUtils.doLongHttp(request)
        val responseContent = response.body()?.string()
        if (!response.isSuccessful) {
            logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response ($responseContent)")
            LoggerService.addRedLine(response.message())
            throw TaskExecuteException(
                errorMsg = "dockerBuildAndPushImage fail: message ${response.message()} and response ($responseContent)",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
        val dockerBuildAndPushImageResult =
            JsonUtil.to(responseContent!!, object : TypeReference<Result<CheckImageResponse?>>() {
            })
        LoggerService.addNormalLine("dockerBuildAndPushImageResult: $dockerBuildAndPushImageResult")
        if (dockerBuildAndPushImageResult.isNotOk()) {
            LoggerService.addRedLine(JsonUtil.toJson(dockerBuildAndPushImageResult))
            throw TaskExecuteException(
                errorMsg = "dockerBuildAndPushImage fail: ${dockerBuildAndPushImageResult.message}",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
        LoggerService.addNormalLine("dockerBuildAndPushImage success")
    }
}

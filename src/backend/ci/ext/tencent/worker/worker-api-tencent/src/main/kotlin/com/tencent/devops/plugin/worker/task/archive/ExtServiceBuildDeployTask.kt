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
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.element.market.ExtServiceBuildDeployElement
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.DockerRunParam
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

@TaskClassType(classTypes = [ExtServiceBuildDeployElement.classType])
class ExtServiceBuildDeployTask : ITask() {

    private val archiveApi = ApiFactory.create(ArchiveSDKApi::class)

    private val logger = LoggerFactory.getLogger(ExtServiceBuildDeployTask::class.java)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("ExtServiceBuildDeployTask buildTask: $buildTask,buildVariables: $buildVariables")
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
        val extServiceImageInfo = buildVariableMap["extServiceImageInfo"] ?: throw TaskExecuteException(
            errorMsg = "param [extServiceImageInfo] is empty",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
        val taskParams = buildTask.params ?: mapOf()
        val packageName = taskParams["packageName"] ?: throw TaskExecuteException(
            errorMsg = "param [packageName] is empty",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
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
        val file = File(workspace, filePath)
        val uploadFileUrl =
            "/ms/artifactory/api/build/artifactories/ext/services/projects/${buildVariables.projectId}/services/$serviceCode/versions/$serviceVersion/archive?destPath=$destPath"
        val userId = ParameterUtils.getListValueByKey(buildVariables.variablesWithType, PIPELINE_START_USER_ID) ?: throw TaskExecuteException(
            errorMsg = "user basic info error, please check environment.",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
        )
        val headers = mapOf(AUTH_HEADER_USER_ID to userId)
        val uploadResult = archiveApi.uploadFile(
            url = uploadFileUrl,
            destPath = destPath,
            file = file,
            headers = headers
        )
        logger.info("ExtServiceBuildDeployTask uploadResult: $uploadResult")
        val uploadFlag = uploadResult.data
        if (uploadFlag == null || !uploadFlag) {
            throw TaskExecuteException(
                errorMsg = "upload file:${file.name} fail",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
        // 开始构建扩展服务的镜像并把镜像推送到新仓库
        val extServiceImageInfoMap = JsonUtil.toMap(extServiceImageInfo)
        val repoAddr = extServiceImageInfoMap["repoAddr"] as String
        val imageName = extServiceImageInfoMap["imageName"] as String
        val imageTag = extServiceImageInfoMap["imageTag"] as String
        val userName = extServiceImageInfoMap["userName"] as String
        val password = extServiceImageInfoMap["password"] as String
        val dockerBuildParam = DockerBuildParam(
            repoAddr = repoAddr,
            imageName = imageName,
            imageTag = imageTag,
            userName = userName,
            password = password,
            args = listOf("packageName=$packageName", "filePath=$filePath")
        )
        val dockerHostIp = System.getenv("docker_host_ip")
        val projectId = buildVariables.projectId
        val pipelineId = buildVariables.pipelineId
        val vmSeqId = buildVariables.vmSeqId
        val dockerBuildAndPushImagePath =
            "/api/dockernew/build/$projectId/$pipelineId/$vmSeqId/$buildId?elementId=${buildTask.elementId}&syncFlag=true"
        val dockerBuildAndPushImageBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JsonUtil.toJson(dockerBuildParam)
        )
        val dockerBuildAndPushImageUrl = "http://$dockerHostIp$dockerBuildAndPushImagePath"
        val request = Request.Builder()
            .url(dockerBuildAndPushImageUrl)
            .post(dockerBuildAndPushImageBody)
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
            JsonUtil.to(responseContent!!, object : TypeReference<Result<Boolean>>() {
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
        // 开始部署扩展服务
        LoggerService.addNormalLine("start deploy extService:$serviceCode(version:$serviceVersion)")
        val dockerRunParam = DockerRunParam(
            imageName = "$repoAddr/$imageName:$imageTag",
            registryUser = userName,
            registryPwd = password,
            command = listOf(),
            env = null
        )
        val dockerRunPath =
            "/api/docker/run/$projectId/$pipelineId/$vmSeqId/$buildId"
        val dockerRunBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JsonUtil.toJson(dockerRunParam)
        )
        // todo 启动docker服务的服务器IP算法待完善
        val dockerRunUrl = "http://$dockerHostIp$dockerRunPath"
        val dockerRunRequest = Request.Builder()
            .url(dockerRunUrl)
            .post(dockerRunBody)
            .build()
        val dockerRunResponse = OkhttpUtils.doLongHttp(dockerRunRequest)
        val dockerRunResponseContent = dockerRunResponse.body()?.string()
        if (!dockerRunResponse.isSuccessful) {
            logger.warn("Fail to request($dockerRunRequest) with code ${dockerRunResponse.code()} , message ${dockerRunResponse.message()} and response ($dockerRunResponseContent)")
            LoggerService.addRedLine(dockerRunResponse.message())
            throw TaskExecuteException(
                errorMsg = "dockerRun fail: message ${dockerRunResponse.message()} and response ($dockerRunResponseContent)",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
        val dockerRunResult =
            JsonUtil.to(dockerRunResponseContent!!, object : TypeReference<Result<Boolean>>() {
            })
        LoggerService.addNormalLine("dockerRunResult: $dockerRunResult")
        if (dockerRunResult.isNotOk()) {
            LoggerService.addRedLine(JsonUtil.toJson(dockerRunResult))
            throw TaskExecuteException(
                errorMsg = "dockerRun fail: ${dockerRunResult.message}",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
        LoggerService.addNormalLine("deploy extService:$serviceCode(version:$serviceVersion) success")
    }
}

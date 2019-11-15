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

package com.tencent.devops.plugin.worker.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.log.Ansi
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.archive.element.BuildPushDockerImageElement
import com.tencent.devops.dockerhost.pojo.DockerBuildParam
import com.tencent.devops.dockerhost.pojo.Status
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.env.AgentEnv.isDockerEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.task.script.CommandFactory
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.io.File

@TaskClassType(classTypes = [BuildPushDockerImageElement.classType])
class BuildPushDockerImageTask : ITask() {

    private val api = ApiFactory.create(ArchiveSDKApi::class)

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        LoggerService.addNormalLine("Start to execute the docker push image task($taskParams)")
        val imageName = taskParams["imageName"]
        val imageTag = taskParams["imageTag"]
        val buildDir = taskParams["buildDir"]
        val dockerFile = taskParams["dockerFile"]
        val projectId = buildVariables.projectId
        val buildId = buildVariables.buildId

        val responseMap = api.dockerBuildCredential(projectId)
        logger.info("responseMap is $responseMap")
        var repoAddr = responseMap["domain"] as String + ":" + responseMap["docker_port"] as String
        repoAddr = repoAddr.substring(7, repoAddr.length)
        val userName = responseMap["user"] as String
        val password = responseMap["password"] as String

        logger.info("Get the docker build host($repoAddr) user $userName password $password")
        LoggerService.addNormalLine("Get the docker build host($repoAddr)user $userName password $password")

        if (isDockerEnv()) {
            // docker 则需要调用母机进行docker build
            logger.info("Start docker build, $imageName:$imageTag")
            LoggerService.addNormalLine("启动构建镜像，镜像名称：$imageName:$imageTag")
            startDockerBuild(buildVariables, imageName, imageTag, buildDir, dockerFile, repoAddr, userName, password, buildTask.elementId)

            Thread.sleep(2 * 1000)
            // 轮询状态
            LoggerService.addNormalLine("启动构建镜像成功，等待构建镜像结束，镜像名称：$imageName:$imageTag")
            var status = getDockerBuildStatus(buildVariables)
            while (status.first == Status.RUNNING.name) {
                logger.info("Wait for docker build finish...")
                Thread.sleep(2 * 1000)
                status = getDockerBuildStatus(buildVariables)
            }
            if (status.first == Status.FAILURE.name) {
                logger.info("Docker build failed, msg: ${status.second}")
                LoggerService.addNormalLine("构建镜像失败，错误详情：${status.second}")
                throw RuntimeException("failed to build docker image.")
            } else {
                LoggerService.addNormalLine("构建镜像成功！")
            }
        } else {
            // worker直接执行命令即可
            val loginScript = "sudo docker login $repoAddr --username $userName --password $password"

            logger.info("Start to build the docker images.")
            val command = CommandFactory.create(BuildScriptType.SHELL.name)
            val runtimeVariables = buildVariables.variables
            command.execute(buildId, loginScript, taskParams, runtimeVariables, projectId, workspace, buildVariables.buildEnvs)

            LoggerService.addNormalLine("Start to build the docker image. imageName:$imageName; imageTag:$imageTag")
            val buildScript = "sudo docker build --pull -f $dockerFile -t $repoAddr/paas/$projectId/$imageName:$imageTag $buildDir"
            try {
                command.execute(buildId, buildScript, taskParams, runtimeVariables, projectId, workspace, buildVariables.buildEnvs)
            } catch (t: RuntimeException) {
                logger.warn("Fail to execute docker build command", t)
                LoggerService.addNormalLine(Ansi().fgRed().a("Dockerfile第一行请确认使用$repoAddr").reset().toString())
                throw t
            }

            LoggerService.addNormalLine("Start to push the docker image. imageName:$imageName; imageTag:$imageTag")
            val pushScript = "sudo docker push $repoAddr/paas/$projectId/$imageName:$imageTag"
            command.execute(buildId, pushScript, taskParams, runtimeVariables, projectId, workspace, buildVariables.buildEnvs)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDockerBuildStatus(buildVariables: BuildVariables): Pair<String, Any?> {
        val dockerHostIp = System.getenv("docker_host_ip")
        val dockerHostPort = System.getenv("docker_host_port")
        val url = "http://$dockerHostIp:$dockerHostPort/api/docker/build/${buildVariables.vmSeqId}/${buildVariables.buildId}"
        logger.info("request url: $url")

        val request = Request.Builder().url(url)
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get start docker build")
                LoggerService.addNormalLine(Ansi().fgRed().a("启动构建镜像失败！请联系【蓝盾助手】").reset().toString())
                throw RuntimeException("failed to get docker build status")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody)
            if (responseData["status"] == 0) {
                val map = responseData["data"] as Map<String, Any>
                return Pair(map["first"] as String, map["second"])
            } else {
                LoggerService.addNormalLine(Ansi().fgRed().a("查询构建镜像状态失败！请联系【蓝盾助手】").reset().toString())
                throw RuntimeException("failed to get docker build status")
            }
        }
    }

    private fun startDockerBuild(buildVariables: BuildVariables, imageName: String?, imageTag: String?, buildDir: String?, dockerFile: String?, repoAddr: String, userName: String, password: String, elementId: String?) {
        val dockerHostIp = System.getenv("docker_host_ip")
        val dockerHostPort = System.getenv("docker_host_port")
        val url = "http://$dockerHostIp:$dockerHostPort/api/docker/build/${buildVariables.projectId}/${buildVariables.pipelineId}/${buildVariables.vmSeqId}/${buildVariables.buildId}/$elementId"
        val dockerbuildParam = DockerBuildParam(imageName!!, imageTag!!, buildDir, dockerFile, repoAddr, userName, password)
        val requestBody = jacksonObjectMapper().writeValueAsString(dockerbuildParam)
        logger.info("request url: $url")
        logger.info("request body: $requestBody")

        val request = Request.Builder().url(url)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestBody))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            logger.info("responseBody: $responseBody")
            if (!response.isSuccessful) {
                logger.error("failed to get start docker build")
                LoggerService.addNormalLine(Ansi().fgRed().a("启动构建失败！请联系【蓝盾助手】").reset().toString())
                throw RuntimeException("failed to get tstack token")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildPushDockerImageTask::class.java)
    }
}
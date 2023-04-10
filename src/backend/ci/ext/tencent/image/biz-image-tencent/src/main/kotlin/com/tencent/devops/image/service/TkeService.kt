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

package com.tencent.devops.image.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.dockerjava.api.model.AuthConfig
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.image.config.DockerConfig
import com.tencent.devops.image.constants.ImageMessageCode.BK_FAILED_REGISTER_IMAGE
import com.tencent.devops.image.constants.ImageMessageCode.BK_SOURCE_IMAGE
import com.tencent.devops.image.constants.ImageMessageCode.BK_SUCCESSFUL_REGISTRATION_IMAGE
import com.tencent.devops.image.constants.ImageMessageCode.BK_TARGET_IMAGE
import com.tencent.devops.image.pojo.PushImageTask
import com.tencent.devops.image.pojo.enums.TaskStatus
import com.tencent.devops.image.pojo.tke.TkePushImageParam
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors

/**
 * 腾讯内部TKE接口(乱)
 *  注意不要在里面留下敏感信息，请到配置文件中写
 */
@Service
class TkeService @Autowired constructor(
    private val dockerConfig: DockerConfig,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val buildLogPrinter: BuildLogPrinter
) {
    // 从配置文件中读取相关配置
    @Value("\${esb.appCode:#{null}}")
    val appCode: String = ""

    @Value("\${esb.appSecret:#{null}}")
    val appSecret: String = ""

    @Value("\${tke.caller:#{null}}")
    val caller: String = ""

    @Value("\${tke.appId:#{null}}")
    val appId: String = ""

    @Value("\${tke.importImageUrl:#{null}}")
    val importImageUrl: String = ""

    @Value("\${tke.apiKey:#{null}}")
    val apiKey: String = ""

    @Value("\${tke.repoName:#{null}}")
    val repoName = ""

    companion object {
        private val logger = LoggerFactory.getLogger(TkeService::class.java)
        private val executorService = Executors.newFixedThreadPool(8)
        private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
    }

    private val dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost(dockerConfig.dockerHost)
        .withDockerConfig(dockerConfig.dockerConfig)
        .withApiVersion(dockerConfig.apiVersion)
        .withRegistryUrl(dockerConfig.imagePrefix)
        .withRegistryUsername(dockerConfig.registryUsername)
        .withRegistryPassword(SecurityUtil.decrypt(dockerConfig.registryPassword!!))
        .build()

    fun pushTkeImage(pushImageParam: TkePushImageParam): PushImageTask? {
        val taskId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val task = PushImageTask(
            taskId = taskId,
            projectId = pushImageParam.projectId,
            operator = pushImageParam.userId,
            createdTime = now.timestamp(),
            updatedTime = now.timestamp(),
            taskStatus = TaskStatus.RUNNING.name,
            taskMessage = ""
        )
        setRedisTask(taskId, task)
        executorService.execute { syncPushImage(pushImageParam, task) }
        return task
    }

    private val dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build()

    private fun buildTkeImageTaskKey(taskId: String): String {
        return "image.pushTkeImageTask_$taskId"
    }

    fun getPushTkeImageTask(taskId: String): PushImageTask? {
        val task = redisOperation.get(buildTkeImageTaskKey(taskId)) ?: return null
        try {
            return objectMapper.readValue(task, PushImageTask::class.java)
        } catch (t: Throwable) {
            logger.warn("covert tkeImageTask failed, task: $task", t)
        }
        return null
    }

    private fun setRedisTask(taskId: String, task: PushImageTask) {
        redisOperation.set(buildTkeImageTaskKey(taskId), objectMapper.writeValueAsString(task), 3600)
    }

    private fun syncPushImage(pushImageParam: TkePushImageParam, task: PushImageTask) {
        logger.info("[${pushImageParam.buildId}]|push TKE image, taskId: ${task.taskId}, pushImageParam: ${pushImageParam.outStr()}")

        val fromImage =
            "${dockerConfig.imagePrefix}/paas/${pushImageParam.projectId}/${pushImageParam.srcImageName}:${pushImageParam.srcImageTag}"
        buildLogPrinter.addLine(
            buildId = pushImageParam.buildId,
            message = I18nUtil.getCodeLanMessage(
                messageCode = BK_SOURCE_IMAGE,
                params = arrayOf(fromImage)
            ),
            tag = pushImageParam.taskId,
            jobId = pushImageParam.containerId,
            executeCount = pushImageParam.executeCount ?: 1
        )
        val toImageRepo = if (pushImageParam.verifyOa) {
            "${pushImageParam.repoAddress}/${pushImageParam.targetImageName}"
        } else {
            "${pushImageParam.repoAddress}/${pushImageParam.userName}/${pushImageParam.targetImageName}"
        }
        try {
            pullFromDockerRegistry(fromImage)
            logger.info("[${pushImageParam.buildId}]|Pull image success, image name and tag: $fromImage")
            dockerClient.tagImageCmd(fromImage, toImageRepo, pushImageParam.targetImageTag).exec()
            logger.info("[${pushImageParam.buildId}]|Tag image success, image name and tag: $toImageRepo:${pushImageParam.targetImageTag}")
            buildLogPrinter.addLine(
                buildId = pushImageParam.buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_TARGET_IMAGE,
                    params = arrayOf(toImageRepo, pushImageParam.targetImageTag)
                ),
                tag = pushImageParam.taskId,
                jobId = pushImageParam.containerId,
                executeCount = pushImageParam.executeCount ?: 1
            )
            pushImageToTke(pushImageParam)
            logger.info("Push image success, image name and tag: $toImageRepo:${pushImageParam.targetImageTag}")
            // 到tke平台上注册这个镜像，主要为了携带caller 、ciInstId、moduleId、imageUrl等字段
            imageImport(pushImageParam)

            task.apply {
                taskStatus = TaskStatus.SUCCESS.name
                updatedTime = LocalDateTime.now().timestamp()
            }
            setRedisTask(task.taskId, task)
        } catch (e: Throwable) {
            logger.error("[${pushImageParam.buildId}]|push TKE image error", e)
            task.apply {
                taskStatus = TaskStatus.FAILED.name
                updatedTime = LocalDateTime.now().timestamp()
                taskMessage = e.message!!
            }
            setRedisTask(task.taskId, task)
        } finally {
            try {
                dockerClient.removeImageCmd(fromImage).exec()
                logger.info("[${pushImageParam.buildId}]|Remove local source image success: $fromImage")
            } catch (e: Throwable) {
                logger.error("[${pushImageParam.buildId}]|Docker rmi failed, msg: ${e.message}")
            }
            try {
                dockerClient.removeImageCmd("$toImageRepo:${pushImageParam.targetImageTag}").exec()
                logger.info("[${pushImageParam.buildId}]|Remove local source image success: $toImageRepo:${pushImageParam.targetImageTag}")
            } catch (e: Throwable) {
                logger.error("[${pushImageParam.buildId}]|Docker rmi failed, msg: ${e.message}")
            }
        }
    }

    /**
     * API: 看配置文件里面注释有写，这里会开源成敏感信息
     */
    private fun imageImport(pushImageParam: TkePushImageParam) {
        try {
            val url = importImageUrl
            val requestData = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "params" to mapOf(
                    "content" to mapOf(
                        "type" to "Json",
                        "version" to "1.0",
                        "requestInfo" to mapOf(
                            "operator" to pushImageParam.userId,
                            "caller" to caller,
                            "ciInstId" to pushImageParam.buildId,
                            "appId" to appId
                        ),
                        "requestItem" to mapOf(
                            "method" to "image_db_import",
                            "data" to mapOf(
                                "repoName" to repoName,
                                "imageTag" to pushImageParam.targetImageTag,
                                "comment" to "",
                                "mode" to 0,
                                "buildType" to 0,
                                "dockerFile" to "",
                                "moduleId" to pushImageParam.cmdbId,
                                "isGpu" to 0,
                                "imageUrl" to "$repoName/${pushImageParam.userName}/${pushImageParam.targetImageName.removePrefix(
                                    "/"
                                )}",
                                "codeUrl" to pushImageParam.codeUrl
                            )
                        )
                    )
                )
            )
            val requestBody = ObjectMapper().writeValueAsString(requestData)
            val request = Request.Builder().url(url).post(RequestBody.create(JSON, requestBody))
                .addHeader("apikey", apiKey).build()
            logger.info("[${pushImageParam.buildId}]|requestUrl: $url")
            logger.info("[${pushImageParam.buildId}]|requestBody: $requestBody")
            OkhttpUtils.doHttp(request).use { res ->
                val responseBody = res.body!!.string()
                logger.info("[${pushImageParam.buildId}]|responseBody: $responseBody")

                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseBody!!)
                val code = responseData["code"] as String
                if (NumberUtils.isDigits(code) && code.toInt() == 0) {
                    logger.error("[${pushImageParam.buildId}]|Import docker image success")
                    buildLogPrinter.addLine(
                        buildId = pushImageParam.buildId,
                        message = I18nUtil.getCodeLanMessage(
                            messageCode = BK_SUCCESSFUL_REGISTRATION_IMAGE
                        ),
                        tag = pushImageParam.taskId,
                        jobId = pushImageParam.containerId,
                        executeCount = pushImageParam.executeCount ?: 1
                    )
                } else {
                    val msg = responseData["msg"]
                    logger.error("[${pushImageParam.buildId}]|Import docker image failed, msg:$msg")
                    buildLogPrinter.addRedLine(
                        buildId = pushImageParam.buildId,
                        message = I18nUtil.getCodeLanMessage(
                            messageCode = BK_FAILED_REGISTER_IMAGE
                        ) + "$msg",
                        tag = pushImageParam.taskId,
                        jobId = pushImageParam.containerId,
                        executeCount = pushImageParam.executeCount ?: 1
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("[${pushImageParam.buildId}]|Import docker image failed exception:", e)
            buildLogPrinter.addRedLine(
                buildId = pushImageParam.buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_FAILED_REGISTER_IMAGE
                ) + "${e.message}",
                tag = pushImageParam.taskId,
                jobId = pushImageParam.containerId,
                executeCount = pushImageParam.executeCount ?: 1
            )
        }
    }

    private fun pullFromDockerRegistry(image: String) {
        dockerClient.pullImageCmd(image).exec(PullImageResultCallback()).awaitCompletion()
    }

    private fun pushImageToTke(pushImageParam: TkePushImageParam) {
        val image = if (pushImageParam.verifyOa) {
            "${pushImageParam.repoAddress}/${pushImageParam.targetImageName}:${pushImageParam.targetImageTag}"
        } else {
            "${pushImageParam.repoAddress}/${pushImageParam.userName}/${pushImageParam.targetImageName}:${pushImageParam.targetImageTag}"
        }

        val tkeDockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerConfig.dockerHost)
            .withDockerConfig(dockerConfig.dockerConfig)
            .withApiVersion(dockerConfig.apiVersion)
            .withRegistryUrl(pushImageParam.repoAddress)
            .withRegistryUsername(pushImageParam.userName)
            .withRegistryPassword(pushImageParam.password)
            .build()
        val tkeDockerClient = DockerClientBuilder.getInstance(tkeDockerClientConfig).build()

        try {
            val authConfig = AuthConfig()
                .withUsername(pushImageParam.userName)
                .withPassword(pushImageParam.password)
                .withRegistryAddress(pushImageParam.repoAddress)
            tkeDockerClient.pushImageCmd(image).withAuthConfig(authConfig).exec(PushImageResultCallback())
                .awaitCompletion()
        } finally {
            try {
                tkeDockerClient.close()
            } catch (e: Exception) {
                //
            }
        }
    }
}

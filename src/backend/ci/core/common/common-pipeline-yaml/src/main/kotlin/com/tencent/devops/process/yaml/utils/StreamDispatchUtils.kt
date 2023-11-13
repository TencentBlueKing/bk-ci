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

package com.tencent.devops.process.yaml.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.devops.common.api.constant.CommonMessageCode.BUILD_RESOURCE_NOT_EXIST
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.pojo.ThirdPartyContainerInfo
import com.tencent.devops.process.yaml.v2.models.job.Container
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import com.tencent.devops.common.pipeline.type.agent.Credential as thirdPartDockerCredential

@Suppress("ALL")
object StreamDispatchUtils {

    private val logger = LoggerFactory.getLogger(StreamDispatchUtils::class.java)

    fun getBaseOs(job: Job, context: Map<String, String>? = null): VMBaseOS {
        val poolName = EnvUtils.parseEnv(job.runsOn.poolName, context ?: mapOf())
        // 公共构建机池
        if (poolName == JobRunsOnType.DOCKER.type) {
            return VMBaseOS.LINUX
        } else if (poolName.startsWith("macos")) {
            return VMBaseOS.MACOS
        }

        // agentSelector 也要支持占位符
        if (job.runsOn.agentSelector.isNullOrEmpty()) {
            return VMBaseOS.ALL
        }
        return when (EnvUtils.parseEnv(job.runsOn.agentSelector!![0], context ?: mapOf())) {
            "linux" -> VMBaseOS.LINUX
            "macos" -> VMBaseOS.MACOS
            "windows" -> VMBaseOS.WINDOWS
            else -> VMBaseOS.LINUX
        }
    }

    fun getBuildEnv(job: Job, context: Map<String, String>? = null): Map<String, String>? {
        return if (job.runsOn.selfHosted == false) {
            job.runsOn.needs?.map { it ->
                it.key to EnvUtils.parseEnv(it.value, context ?: mapOf())
            }?.toMap()
        } else {
            null
        }
    }

    @Throws(
        JsonProcessingException::class,
        ParamBlankException::class,
        CustomException::class
    )
    fun getDispatchType(
        job: Job,
        defaultImage: String,
        context: Map<String, String>? = null,
        containsMatrix: Boolean? = false,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType {

        val poolName = EnvUtils.parseEnv(job.runsOn.poolName, context ?: mapOf())
        val workspace = EnvUtils.parseEnv(job.runsOn.workspace, context ?: mapOf())

        // 第三方构建机
        if (job.runsOn.selfHosted == true) {
            if (job.runsOn.container == null) {
                return ThirdPartyAgentEnvDispatchType(
                    envProjectId = null,
                    envName = poolName,
                    workspace = workspace,
                    agentType = AgentType.NAME,
                    dockerInfo = null
                )
            }

            val info = parseRunsOnContainer(
                job = job,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            )

            val dockerInfo = ThirdPartyAgentDockerInfo(
                image = info.image,
                credential = thirdPartDockerCredential(
                    user = info.userName,
                    password = info.password,
                    credentialId = info.credId,
                    acrossTemplateId = info.acrossTemplateId,
                    jobId = job.id
                ),
                options = info.options,
                imagePullPolicy = info.imagePullPolicy
            )

            return ThirdPartyAgentEnvDispatchType(
                envProjectId = null,
                envName = poolName,
                workspace = workspace,
                agentType = AgentType.NAME,
                dockerInfo = dockerInfo
            )
        }

        // macos构建机
        if (poolName.startsWith("macos")) {
            // 外部版暂时不支持macos构建机，遇到直接报错
            throw CustomException(
                Response.Status.BAD_REQUEST,
                I18nUtil.getCodeLanMessage(messageCode = BUILD_RESOURCE_NOT_EXIST, params = arrayOf("macos"))
            )
        }

        // 公共docker构建机
        if (poolName == "docker") {
            var image = defaultImage
            var credentialId = ""
            var env: Map<String, String>?

            if (job.runsOn.container != null) {
                try {
                    val container = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(job.runsOn.container!!),
                        Container::class.java
                    )

                    image = EnvUtils.parseEnv(container.image, context ?: mapOf())
                    env = job.env
                } catch (e: Exception) {
                    val container = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(job.runsOn.container!!),
                        Container2::class.java
                    )

                    image = EnvUtils.parseEnv(container.image, context ?: mapOf())
                    credentialId = EnvUtils.parseEnv(container.credentials, context ?: mapOf())
                    env = job.env
                }
            }

            return DockerDispatchType(
                dockerBuildVersion = image,
                credentialId = credentialId,
                imageType = ImageType.THIRD
            )
        }

        if (containsMatrix == true) {
            return DockerDispatchType(defaultImage)
        } else {
            throw CustomException(
                Response.Status.NOT_FOUND, I18nUtil.getCodeLanMessage(
                    messageCode = BUILD_RESOURCE_NOT_EXIST,
                    params = arrayOf("public")
                )
            )
        }
    }

    /**
     * 解析 jobs.runsOn.container
     * 注：因为要蓝盾也要支持所以环境变量替换会在蓝盾层面去做
     * @return image,username,password
     */
    fun parseRunsOnContainer(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): ThirdPartyContainerInfo {
        return try {
            val container = YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(job.runsOn.container!!),
                Container::class.java
            )

            ThirdPartyContainerInfo(
                image = container.image,
                userName = container.credentials?.username,
                password = container.credentials?.password,
                credId = null,
                acrossTemplateId = null,
                options = container.options,
                imagePullPolicy = container.imagePullPolicy
            )
        } catch (e: Exception) {
            val container = YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(job.runsOn.container!!),
                Container2::class.java
            )

            ThirdPartyContainerInfo(
                image = container.image,
                userName = null,
                password = null,
                credId = container.credentials,
                acrossTemplateId = buildTemplateAcrossInfo?.templateId,
                options = container.options,
                imagePullPolicy = container.imagePullPolicy
            )
        }
    }
}

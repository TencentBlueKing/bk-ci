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
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.job.Container
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.ws.rs.core.Response
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
        client: Client,
        objectMapper: ObjectMapper,
        job: Job,
        projectCode: String,
        defaultImage: String,
        resources: Resources? = null,
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

            val (image, userName, password) = parseRunsOnContainer(
                client = client,
                job = job,
                projectCode = projectCode,
                context = context,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            )

            val dockerInfo = ThirdPartyAgentDockerInfo(
                image = image,
                credential = if (userName.isBlank() || password.isBlank()) {
                    null
                } else {
                    thirdPartDockerCredential(
                        user = userName,
                        password = password
                    )
                },
                envs = job.env
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
            throw CustomException(Response.Status.BAD_REQUEST, "MACOS构建资源暂不支持，请检查yml配置.")
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
            throw CustomException(Response.Status.NOT_FOUND, "公共构建资源池不存在，请检查yml配置.")
        }
    }

    /**
     * 解析 jobs.runsOn.container
     * @return image,username,password
     */
    fun parseRunsOnContainer(
        client: Client,
        job: Job,
        projectCode: String,
        context: Map<String, String>?,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): Triple<String, String, String> {
        return try {
            val container = YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(job.runsOn.container!!),
                Container::class.java
            )

            Triple(
                EnvUtils.parseEnv(container.image, context ?: mapOf()),
                EnvUtils.parseEnv(container.credentials?.username, context ?: mapOf()),
                EnvUtils.parseEnv(container.credentials?.password, context ?: mapOf())
            )
        } catch (e: Exception) {
            val container = YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(job.runsOn.container!!),
                Container2::class.java
            )

            var user = ""
            var password = ""
            if (!container.credentials.isNullOrEmpty()) {
                val ticketsMap = getTicket(client, projectCode, container, context, buildTemplateAcrossInfo)
                user = ticketsMap["v1"] as String
                password = ticketsMap["v2"] as String
            }

            Triple(
                EnvUtils.parseEnv(container.image, context ?: mapOf()),
                user,
                password
            )
        }
    }

    private fun getTicket(
        client: Client,
        projectCode: String,
        container: Container2,
        context: Map<String, String>?,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): MutableMap<String, String> {
        val ticketsMap = try {
            getCredential(
                client = client,
                projectId = projectCode,
                credentialId = EnvUtils.parseEnv(container.credentials, context ?: mapOf()),
                type = CredentialType.USERNAME_PASSWORD
            )
        } catch (ignore: Exception) {
            // 没有跨项目的模板引用就直接扔出错误
            if (buildTemplateAcrossInfo == null) {
                throw ignore
            }
            getCredential(
                client = client,
                projectId = buildTemplateAcrossInfo.targetProjectId,
                credentialId = EnvUtils.parseEnv(container.credentials, context ?: mapOf()),
                type = CredentialType.USERNAME_PASSWORD,
                acrossProject = true
            )
        }
        return ticketsMap
    }

    private fun getCredential(
        client: Client,
        projectId: String,
        credentialId: String,
        type: CredentialType,
        acrossProject: Boolean = false
    ): MutableMap<String, String> {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val decoder = Base64.getDecoder()
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId, credentialId,
            encoder.encodeToString(pair.publicKey)
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            throw RuntimeException(
                "Fail to get the credential($credentialId) of project($projectId), " +
                        "because of ${credentialResult.message}"
            )
        }

        val credential = credentialResult.data!!
        if (type != credential.credentialType) {
            throw ParamBlankException(
                "Fail to get the credential($credentialId) of project($projectId), " +
                        "expect:${type.name}, but real:${credential.credentialType.name}"
            )
        }

        if (acrossProject && !credential.allowAcrossProject) {
            throw RuntimeException(
                "Fail to get the credential($credentialId) of project($projectId), " +
                        "not allow across project use"
            )
        }

        val ticketMap = mutableMapOf<String, String>()
        val v1 = String(
            DHUtil.decrypt(
                decoder.decode(credential.v1),
                decoder.decode(credential.publicKey),
                pair.privateKey
            )
        )
        ticketMap["v1"] = v1

        if (credential.v2 != null && credential.v2!!.isNotEmpty()) {
            val v2 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v2),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v2"] = v2
        }

        if (credential.v3 != null && credential.v3!!.isNotEmpty()) {
            val v3 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v3),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v3"] = v3
        }

        if (credential.v4 != null && credential.v4!!.isNotEmpty()) {
            val v4 = String(
                DHUtil.decrypt(
                    decoder.decode(credential.v4),
                    decoder.decode(credential.publicKey),
                    pair.privateKey
                )
            )
            ticketMap["v4"] = v4
        }

        return ticketMap
    }
}

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

package com.tencent.devops.process.yaml.modelCreate.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.image.PoolType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.pipeline.type.windows.WindowsDispatchType
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelCreate.pojo.enums.DispatchBizType
import com.tencent.devops.process.yaml.utils.StreamDispatchUtils
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.ResourcesPools
import com.tencent.devops.process.yaml.v2.models.job.Container
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response
import com.tencent.devops.common.pipeline.type.agent.Credential as thirdPartDockerCredential

@Suppress("NestedBlockDepth", "ComplexMethod")
object TXStreamDispatchUtils {

    private val logger = LoggerFactory.getLogger(TXStreamDispatchUtils::class.java)

    fun getBaseOs(job: Job, context: Map<String, String>? = null): VMBaseOS {
        val poolName = EnvUtils.parseEnv(job.runsOn.poolName, context ?: mapOf())
        // 公共构建机池
        if (poolName == JobRunsOnType.DOCKER.type || poolName == JobRunsOnType.DEV_CLOUD.type) {
            return VMBaseOS.LINUX
        } else if (poolName.startsWith("macos")) {
            return VMBaseOS.MACOS
        } else if (poolName.startsWith("windows")) {
            return VMBaseOS.WINDOWS
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
        bizType: DispatchBizType,
        resources: Resources? = null,
        context: Map<String, String>? = null,
        containsMatrix: Boolean? = false,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType {

        val poolName = EnvUtils.parseEnv(job.runsOn.poolName, context ?: mapOf())
        val workspace = EnvUtils.parseEnv(job.runsOn.workspace, context ?: mapOf())
        val xcode = EnvUtils.parseEnv(job.runsOn.xcode, context ?: mapOf())

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

            val info = StreamDispatchUtils.parseRunsOnContainer(
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
                )

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

            return MacOSDispatchType(
                macOSEvn = "${poolName.removePrefix("macos-")}:$xcode",
                systemVersion = poolName.removePrefix("macos-"),
                xcodeVersion = xcode
            )
        }

        // windows公共构建机
        if (poolName.startsWith("windows")) {
            return WindowsDispatchType(
                env = "",
                systemVersion = poolName
            )
        }

        // 公共docker构建机
        if (poolName == "docker" && bizType != DispatchBizType.PRECI) {
            // 在构建机类型有差异的地方根据业务场景区分
            when (bizType) {
                DispatchBizType.RDS -> {
                    return PublicDevCloudDispathcType(
                        image = defaultImage,
                        imageType = ImageType.THIRD,
                        performanceConfigId = "0"
                    )
                }

                else -> {}
            }

            val dockerVMContainerPool = makeContainerPool(
                BuildType.DOCKER_VM,
                client,
                job,
                projectCode,
                defaultImage,
                context,
                buildTemplateAcrossInfo
            )

            return GitCIDispatchType(objectMapper.writeValueAsString(dockerVMContainerPool))
        }

        if (bizType == DispatchBizType.PRECI) {
            when (poolName) {
                JobRunsOnType.LOCAL.type -> {
                    return ThirdPartyAgentIDDispatchType(
                        displayName = "",
                        workspace = "",
                        agentType = AgentType.ID,
                        dockerInfo = null
                    )
                }

                JobRunsOnType.DEV_CLOUD.type -> {
                    return PoolType.DockerOnDevCloud.toDispatchType(
                        makeContainerPool(
                            BuildType.DEVCLOUD,
                            client,
                            job,
                            projectCode,
                            defaultImage,
                            context,
                            buildTemplateAcrossInfo
                        )
                    )
                }

                JobRunsOnType.DOCKER.type -> {
                    return PoolType.DockerOnVm.toDispatchType(
                        makeContainerPool(
                            BuildType.DOCKER_VM,
                            client,
                            job,
                            projectCode,
                            defaultImage,
                            context,
                            buildTemplateAcrossInfo
                        )
                    )
                }

                else -> {}
            }
        }

        if (containsMatrix == true) {
            return when (bizType) {
                DispatchBizType.RDS, DispatchBizType.PRECI -> PublicDevCloudDispathcType(
                    image = defaultImage,
                    imageType = ImageType.THIRD,
                    performanceConfigId = "0"
                )

                else -> GitCIDispatchType(defaultImage)
            }
        } else {
            throw CustomException(Response.Status.NOT_FOUND, "公共构建资源池不存在，请检查yml配置.")
        }
    }

    private fun makeContainerPool(
        buildType: BuildType,
        client: Client,
        job: Job,
        projectCode: String,
        defaultImage: String,
        context: Map<String, String>? = null,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): Pool {
        var containerPool = Pool(
            container = defaultImage,
            credential = Credential(
                user = "",
                password = ""
            ),
            macOS = null,
            third = null,
            env = job.env,
            buildType = buildType
        )

        if (job.runsOn.container != null) {
            try {
                val container = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(job.runsOn.container!!),
                    Container::class.java
                )

                containerPool = Pool(
                    container = EnvUtils.parseEnv(container.image, context ?: mapOf()),
                    credential = Credential(
                        user = EnvUtils.parseEnv(container.credentials?.username, context ?: mapOf()),
                        password = EnvUtils.parseEnv(container.credentials?.password, context ?: mapOf())
                    ),
                    macOS = null,
                    third = null,
                    env = job.env,
                    buildType = buildType
                )
            } catch (e: Exception) {
                val container = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(job.runsOn.container!!),
                    Container2::class.java
                )

                containerPool = Pool(
                    container = EnvUtils.parseEnv(container.image, context ?: mapOf()),
                    credential = Credential(
                        user = "",
                        password = "",
                        credentialId = EnvUtils.parseEnv(container.credentials, context ?: mapOf()),
                        fromRemote = if (buildTemplateAcrossInfo != null) Credential.Remote(
                            targetProjectId = buildTemplateAcrossInfo.targetProjectId,
                            templateId = buildTemplateAcrossInfo.templateId,
                            jobId = job.id ?: ""
                        ) else null
                    ),
                    macOS = null,
                    third = null,
                    env = job.env,
                    buildType = buildType
                )
            }
        }

        return containerPool
    }

    private fun getEnvName(client: Client, poolName: String, pools: List<ResourcesPools>?): String {
        if (pools.isNullOrEmpty()) {
            return poolName
        }

        pools.filter { !it.from.isNullOrBlank() && !it.name.isNullOrBlank() }.forEach label@{
            if (it.name == poolName) {
                try {
                    val repoNameAndPool = it.from!!.split("@")
                    if (repoNameAndPool.size != 2 || repoNameAndPool[0].isBlank() || repoNameAndPool[1].isBlank()) {
                        return@label
                    }

                    val gitProjectInfo =
                        client.getScm(ServiceGitCiResource::class).getGitCodeProjectInfo(repoNameAndPool[0]).data
                    val result = "git_${gitProjectInfo!!.id}@${repoNameAndPool[1]}"
                    return result
                } catch (e: Exception) {
                    logger.warn("Get projectInfo from git failed, pools: $pools envName: $poolName. exception:", e)
                    return poolName
                }
            }
        }
        logger.info("Get envName from Resource.pools no match.pools: $pools envName: $poolName")
        return poolName
    }
}

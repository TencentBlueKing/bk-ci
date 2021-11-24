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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.v2.Container2
import com.tencent.devops.common.ci.v2.IfType
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.Resources
import com.tencent.devops.common.ci.v2.ResourcesPools
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
class ModelContainer @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper
) {
    @Value("\${container.defaultImage:#{null}}")
    val defaultImage: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(ModelContainer::class.java)
    }

    fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        projectCode: String,
        finalStage: Boolean = false,
        resources: Resources? = null
    ) {
        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = getBaseOs(job),
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = job.timeoutMinutes ?: 900,
            buildEnv = if (job.runsOn.selfHosted == false) {
                job.runsOn.needs
            } else {
                null
            },
            customBuildEnv = job.env,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            jobControlOption = getJobControlOption(job, finalStage),
            dispatchType = getDispatchType(job, projectCode, resources)
        )
        containerList.add(vmContainer)
    }

    fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        finalStage: Boolean = false
    ) {

        containerList.add(
            NormalContainer(
                jobId = job.id,
                containerId = null,
                id = job.id,
                name = job.name ?: "Job-${jobIndex + 1}",
                elements = elementList,
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = false,
                jobControlOption = getJobControlOption(job, finalStage),
                mutexGroup = null
            )
        )
    }

    private fun getJobControlOption(
        job: Job,
        finalStage: Boolean = false
    ): JobControlOption {
        return if (!job.ifField.isNullOrBlank()) {
            if (finalStage) {
                JobControlOption(
                    timeout = job.timeoutMinutes,
                    runCondition = when (job.ifField) {
                        IfType.SUCCESS.name -> JobRunCondition.PREVIOUS_STAGE_SUCCESS
                        IfType.FAILURE.name -> JobRunCondition.PREVIOUS_STAGE_FAILED
                        IfType.CANCELLED.name -> JobRunCondition.PREVIOUS_STAGE_CANCEL
                        else -> JobRunCondition.STAGE_RUNNING
                    },
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    continueWhenFailed = job.continueOnError
                )
            } else {
                JobControlOption(
                    timeout = job.timeoutMinutes,
                    runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = job.ifField.toString(),
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    continueWhenFailed = job.continueOnError
                )
            }
        } else {
            JobControlOption(
                timeout = job.timeoutMinutes,
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn,
                continueWhenFailed = job.continueOnError
            )
        }
    }

    private fun getBaseOs(job: Job): VMBaseOS {
        // 公共构建机池
        if (job.runsOn.poolName == JobRunsOnType.DOCKER.type) {
            return VMBaseOS.LINUX
        } else if (job.runsOn.poolName.startsWith("macos")) {
            return VMBaseOS.MACOS
        }

        if (job.runsOn.agentSelector.isNullOrEmpty()) {
            return VMBaseOS.ALL
        }
        return when (job.runsOn.agentSelector!![0]) {
            "linux" -> VMBaseOS.LINUX
            "macos" -> VMBaseOS.MACOS
            "windows" -> VMBaseOS.WINDOWS
            else -> VMBaseOS.LINUX
        }
    }

    @Throws(
        JsonProcessingException::class,
        ParamBlankException::class,
        CustomException::class
    )
    @Suppress("NestedBlockDepth")
    fun getDispatchType(
        job: Job,
        projectCode: String,
        resources: Resources? = null
    ): DispatchType {
        // macos构建机
        if (job.runsOn.poolName.startsWith("macos")) {
            return MacOSDispatchType(
                macOSEvn = "Catalina10.15.4:12.2",
                systemVersion = "Catalina10.15.4",
                xcodeVersion = "12.2"
            )
        }

        // 第三方构建机
        if (job.runsOn.selfHosted == true) {
            val envName = getEnvName(job.runsOn.poolName, resources?.pools)
            return ThirdPartyAgentEnvDispatchType(
                envName = envName,
                workspace = job.runsOn.workspace,
                agentType = AgentType.NAME
            )
        }

        // 公共docker构建机
        if (job.runsOn.poolName == "docker") {
            var containerPool = Pool(
                container = defaultImage ?: "http://mirrors.tencent.com/ci/tlinux3_ci:0.1.1.0",
                credential = Credential(
                    user = "",
                    password = ""
                ),
                macOS = null,
                third = null,
                env = job.env,
                buildType = BuildType.DOCKER_VM
            )

            if (job.runsOn.container != null) {
                try {
                    val container = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(job.runsOn.container!!),
                        com.tencent.devops.common.ci.v2.Container::class.java
                    )

                    containerPool = Pool(
                        container = container.image,
                        credential = Credential(
                            user = container.credentials?.username ?: "",
                            password = container.credentials?.password ?: ""
                        ),
                        macOS = null,
                        third = null,
                        env = job.env,
                        buildType = BuildType.DOCKER_VM
                    )
                } catch (e: Exception) {
                    val container = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(job.runsOn.container!!),
                        Container2::class.java
                    )

                    var user = ""
                    var password = ""
                    if (!container.credentials.isNullOrEmpty()) {
                        val ticketsMap = GitCommonUtils.getCredential(
                            client = client,
                            projectId = projectCode,
                            credentialId = container.credentials ?: "",
                            type = CredentialType.USERNAME_PASSWORD
                        )
                        user = ticketsMap["v1"] as String
                        password = ticketsMap["v2"] as String
                    }

                    containerPool = Pool(
                        container = container.image,
                        credential = Credential(
                            user = user,
                            password = password
                        ),
                        macOS = null,
                        third = null,
                        env = job.env,
                        buildType = BuildType.DOCKER_VM
                    )
                }
            }

            return GitCIDispatchType(objectMapper.writeValueAsString(containerPool))
        }

        throw CustomException(Response.Status.NOT_FOUND, "公共构建资源池不存在，请检查yml配置.")
    }

    @Suppress("NestedBlockDepth")
    private fun getEnvName(poolName: String, pools: List<ResourcesPools>?): String {
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
                    logger.info("Get envName from Resource.pools success. envName: $result")
                    return result
                } catch (e: Exception) {
                    logger.error("Get projectInfo from git failed, envName: $poolName. exception:", e)
                    return poolName
                }
            }
        }
        logger.info("Get envName from Resource.pools no match. envName: $poolName")
        return poolName
    }
}

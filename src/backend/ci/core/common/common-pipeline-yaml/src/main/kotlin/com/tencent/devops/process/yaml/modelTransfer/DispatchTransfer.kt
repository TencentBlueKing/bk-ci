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

package com.tencent.devops.process.yaml.modelTransfer

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.Credential
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.modelTransfer.inner.TransferCreator
import com.tencent.devops.process.yaml.v3.models.image.Pool
import com.tencent.devops.process.yaml.v3.models.image.PoolImage
import com.tencent.devops.process.yaml.v3.models.image.PoolType
import com.tencent.devops.process.yaml.v3.models.job.Container3
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnPoolType
import com.tencent.devops.process.yaml.v3.models.job.RunsOn
import com.tencent.devops.process.yaml.v3.utils.StreamDispatchUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
class DispatchTransfer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    final val inner: TransferCreator,
    val transferCache: TransferCacheService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchTransfer::class.java)
    }

    private val defaultRunsOn = JSONObject(
        RunsOn(
            container = Container3(
                image = inner.defaultImage,
                imageType = ImageType.BKSTORE.name
            )
        )
    )

    fun makeDispatchType(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType {
        // linux构建机
        dispatcherLinux(job, buildTemplateAcrossInfo)?.let { return it }
        // 第三方构建机
        dispatcherThirdPartyAgent(job, buildTemplateAcrossInfo)?.let { return it }
        // windows构建机
        dispatcherWindows(job)?.let { return it }
        // macos构建机
        dispatcherMacos(job)?.let { return it }
        // 转换失败
        throw CustomException(
            Response.Status.BAD_REQUEST,
            MessageUtil.getMessageByLocale(
                messageCode = CommonMessageCode.PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        )
    }

    fun makeRunsOn(
        job: VMBuildContainer
    ): RunsOn? {
        val dispatchType = job.dispatchType
        if (dispatchType == null) {
            logger.warn("job.dispatchType can not be null")
            return null
        }
        val runsOn = dispatch2RunsOn(dispatchType) ?: RunsOn(
            selfHosted = null,
            poolName = I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_AUTOMATIC_EXPORT_NOT_SUPPORTED
            ),
            container = null,
            agentSelector = null
        )
        if (dispatchType is ThirdPartyAgentEnvDispatchType || dispatchType is ThirdPartyAgentIDDispatchType) {
            runsOn.agentSelector = when (job.baseOS) {
                VMBaseOS.WINDOWS -> listOf("windows")
                VMBaseOS.LINUX -> listOf("linux")
                VMBaseOS.MACOS -> listOf("macos")
                else -> null
            }
        }
        runsOn.needs = job.buildEnv?.ifEmpty { null }
        runsOn.queueTimeoutMinutes = job.jobControlOption?.prepareTimeout?.nullIfDefault(DEFAULT_JOB_PREPARE_TIMEOUT)
        if (JSONObject(runsOn).similar(defaultRunsOn)) {
            return null
        }
        return runsOn
    }

    fun dispatcherLinux(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType? {
        // 公共docker构建机
        if (job.runsOn.poolName == "docker") {
            val info = if (job.runsOn.container != null) {
                StreamDispatchUtils.parseRunsOnContainer(
                    job = job,
                    buildTemplateAcrossInfo = buildTemplateAcrossInfo
                )
            } else null
            val image = (info?.image ?: inner.defaultImage).split(":")
            return PoolType.DockerOnVm.toDispatchType(
                Pool(
                    credentialId = getDockerInfo(job, buildTemplateAcrossInfo)?.credential?.credentialId,
                    image = PoolImage(
                        imageCode = image.getOrElse(0) { "" },
                        imageVersion = image.getOrElse(1) { "" },
                        imageType = info?.imageType
                    )
                )
            )
        }
        return null
    }

    fun dispatcherMacos(
        job: Job
    ): DispatchType? {
        return null
    }

    fun dispatcherWindows(
        job: Job
    ): DispatchType? {
        return null
    }

    fun dispatcherThirdPartyAgent(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType? {
        // 第三方构建机
        if (job.runsOn.selfHosted == true) {
            return PoolType.SelfHosted.toDispatchType(
                with(job.runsOn) {
                    Pool(
                        envName = if (poolType == JobRunsOnPoolType.ENV_NAME.name) poolName else null,
                        workspace = workspace,
                        envId = if (poolType == JobRunsOnPoolType.ENV_ID.name) poolName else null,
                        agentId = if (poolType == JobRunsOnPoolType.AGENT_ID.name) poolName else null,
                        agentName = if (poolType == JobRunsOnPoolType.AGENT_NAME.name) poolName else null,
                        dockerInfo = getDockerInfo(job, buildTemplateAcrossInfo)
                    )
                }
            )
        }
        return null
    }

    private fun getDockerInfo(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): ThirdPartyAgentDockerInfo? {
        return if (job.runsOn.container != null) {
            val info = StreamDispatchUtils.parseRunsOnContainer(
                job = job,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            )
            ThirdPartyAgentDockerInfo(
                image = info.image,
                credential = Credential(
                    user = info.userName,
                    password = info.password,
                    credentialId = info.credId,
                    acrossTemplateId = info.acrossTemplateId,
                    jobId = job.id
                ),
                options = info.options,
                imagePullPolicy = info.imagePullPolicy
            )
        } else null
    }

    fun dispatch2RunsOn(dispatcher: DispatchType) =
        PoolType.SelfHosted.toRunsOn(dispatcher)
            ?: PoolType.DockerOnVm.toRunsOn(dispatcher)
}

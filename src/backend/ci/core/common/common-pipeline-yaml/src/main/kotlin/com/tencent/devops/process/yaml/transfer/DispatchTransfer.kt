/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.yaml.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.Credential
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoStoreImage
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.v3.models.image.Pool
import com.tencent.devops.process.yaml.v3.models.image.PoolImage
import com.tencent.devops.process.yaml.v3.models.image.PoolType
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.RunsOn
import com.tencent.devops.process.yaml.v3.utils.StreamDispatchUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DispatchTransfer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val inner: TransferCreator
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchTransfer::class.java)
        val LINUX_TYPE = setOf("docker", "linux")
        val MACOS_TYPE = setOf("macos-11.4", "macos-12.4", "macos-latest", "macos")
        val WINDOWS_TYPE = setOf("windows-2016", "windows")
    }

    fun makeDispatchType(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): Pair<DispatchType, VMBaseOS> {
        // linux构建机
        dispatcherLinux(job, buildTemplateAcrossInfo)?.let { return Pair(it, VMBaseOS.LINUX) }
        // 第三方构建机
        dispatcherThirdPartyAgent(job, buildTemplateAcrossInfo)?.let { return Pair(it, getBaseOs(job)) }
        // windows构建机
        dispatcherWindows(job)?.let { return Pair(it, VMBaseOS.WINDOWS) }
        // macos构建机
        dispatcherMacos(job)?.let { return Pair(it, VMBaseOS.MACOS) }
        // 转换失败
        throw PipelineTransferException(
            CommonMessageCode.DISPATCH_NOT_SUPPORT_TRANSFER,
            arrayOf("job: ${job.name}")
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
        val runsOn = dispatch2RunsOn(dispatchType) ?: throw PipelineTransferException(
            CommonMessageCode.DISPATCH_NOT_SUPPORT_TRANSFER,
            arrayOf(dispatchType.buildType().name)
        )
        if (dispatchType is ThirdPartyAgentEnvDispatchType || dispatchType is ThirdPartyAgentIDDispatchType) {
            runsOn.agentSelector = when (job.baseOS) {
                VMBaseOS.WINDOWS -> listOf("windows")
                VMBaseOS.LINUX -> listOf("linux")
                VMBaseOS.MACOS -> listOf("macos")
                else -> null
            }
        }
        if (dispatchType is ThirdPartyAgentEnvDispatchType) {
            runsOn.singleNodeConcurrency = job.jobControlOption?.singleNodeConcurrency
            runsOn.allNodeConcurrency = job.jobControlOption?.allNodeConcurrency
        }
        runsOn.needs = job.buildEnv?.ifEmpty { null }
        runsOn.queueTimeoutMinutes = job.jobControlOption?.prepareTimeout?.nullIfDefault(DEFAULT_JOB_PREPARE_TIMEOUT)
        return runsOn
    }

    fun dispatcherLinux(
        job: Job,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ): DispatchType? {
        // 公共docker构建机
        if (job.runsOn.checkLinux()) {
            val info = if (job.runsOn.container != null) {
                StreamDispatchUtils.parseRunsOnContainer(
                    job = job,
                    buildTemplateAcrossInfo = buildTemplateAcrossInfo
                )
            } else null
            return PoolType.DockerOnVm.toDispatchType(
                Pool(
                    credentialId = getDockerInfo(job, buildTemplateAcrossInfo)?.credential?.credentialId,
                    image = PoolImage(
                        imageCode = info?.imageCode ?: inner.defaultImageCode,
                        imageVersion = info?.imageVersion ?: inner.defaultImageVersion,
                        imageType = info?.imageType
                    ),
                    performanceConfigId = job.runsOn.hwSpec
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
                        envName = poolName,
                        workspace = workspace,
                        agentName = nodeName,
                        dockerInfo = getDockerInfo(job, buildTemplateAcrossInfo),
                        lockResourceWith = lockResourceWith,
                        envProjectId = envProjectId
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
                image = info.image ?: "",
                credential = Credential(
                    user = info.userName,
                    password = info.password,
                    credentialId = info.credId,
                    acrossTemplateId = info.acrossTemplateId,
                    jobId = job.id,
                    credentialProjectId = null
                ),
                options = info.options,
                imagePullPolicy = info.imagePullPolicy,
                imageType = info.imageType,
                storeImage = if (info.imageType == ImageType.BKSTORE && info.imageCode != null) {
                    ThirdPartyAgentDockerInfoStoreImage(
                        imageName = null,
                        imageCode = info.imageCode,
                        imageVersion = info.imageVersion
                    )
                } else {
                    null
                }
            )
        } else null
    }

    fun dispatch2RunsOn(dispatcher: DispatchType) =
        PoolType.SelfHosted.toRunsOn(dispatcher)
            ?: PoolType.DockerOnVm.toRunsOn(dispatcher)

    private fun getBaseOs(job: Job): VMBaseOS {
        val poolName = job.runsOn.poolName
        when {
            LINUX_TYPE.contains(poolName) -> return VMBaseOS.LINUX
            MACOS_TYPE.contains(poolName) -> return VMBaseOS.MACOS
            WINDOWS_TYPE.contains(poolName) -> return VMBaseOS.WINDOWS
        }

        val selector = job.runsOn.agentSelector?.get(0)
        return when {
            selector == null -> VMBaseOS.ALL
            LINUX_TYPE.contains(selector) -> VMBaseOS.LINUX
            MACOS_TYPE.contains(selector) -> VMBaseOS.MACOS
            WINDOWS_TYPE.contains(selector) -> VMBaseOS.WINDOWS
            else -> VMBaseOS.LINUX
        }
    }
}

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

package com.tencent.devops.process.yaml.modelCreate

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.matrix.MatrixConfig.Companion.MATRIX_CONTEXT_KEY_PREFIX
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelCreate.inner.TXInnerModelCreator
import com.tencent.devops.process.yaml.modelCreate.pojo.PreCIDispatchInfo
import com.tencent.devops.process.yaml.modelCreate.pojo.RdsDispatchInfo
import com.tencent.devops.process.yaml.modelCreate.pojo.enums.DispatchBizType
import com.tencent.devops.process.yaml.modelCreate.utils.TXStreamDispatchUtils
import com.tencent.devops.process.yaml.modelTransfer.TransferCacheService
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class TXModelContainer @Autowired(required = false) constructor(
    client: Client,
    objectMapper: ObjectMapper,
    @Autowired(required = false)
    inner: TXInnerModelCreator?,
    transferCache: TransferCacheService
) : ModelContainer(
    client, objectMapper, inner, transferCache
) {
    override fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        projectCode: String,
        finalStage: Boolean,
        jobEnable: Boolean,
        resources: Resources?,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ) {
        doSomeCheck(job, TXStreamDispatchUtils.getBaseOs(job))
        val defaultImage = inner!!.defaultImage
        val containsMatrix = JsonUtil.toJson(job.runsOn).contains("\${{ $MATRIX_CONTEXT_KEY_PREFIX")
        val dispatchInfo = (inner as TXInnerModelCreator).getDispatchInfo(
            name = "dispatchInfo_${job.id}",
            job = job,
            projectCode = projectCode,
            defaultImage = defaultImage,
            resources = resources
        )

        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            mutexGroup = getMutexGroup(job.mutex),
            baseOS = TXStreamDispatchUtils.getBaseOs(job),
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = job.timeoutMinutes ?: 900,
            buildEnv = TXStreamDispatchUtils.getBuildEnv(job),
            customBuildEnv = job.env,
            jobControlOption = getJobControlOption(
                job = job, jobEnable = jobEnable, finalStage = finalStage
            ),
            dispatchType = TXStreamDispatchUtils.getDispatchType(
                client = client,
                objectMapper = objectMapper,
                job = job,
                projectCode = projectCode,
                defaultImage = defaultImage,
                bizType = when (dispatchInfo) {
                    is RdsDispatchInfo -> DispatchBizType.RDS
                    is PreCIDispatchInfo -> DispatchBizType.PRECI
                    else -> DispatchBizType.STREAM
                },
                resources = resources,
                containsMatrix = containsMatrix,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            ),
            matrixGroupFlag = job.strategy != null,
            matrixControlOption = getMatrixControlOption(
                job,
                if (containsMatrix) {
                    dispatchInfo
                } else {
                    null
                }
            )
        )

        containerList.add(vmContainer)
    }

    override fun addYamlVMBuildContainer(job: VMBuildContainer, steps: List<PreStep>?): PreJob {
        return PreJob(
            name = job.name,
            runsOn = getRunsOn(job),
            container = null,
            services = null,
            ifField = when (job.jobControlOption?.runCondition) {
                JobRunCondition.CUSTOM_CONDITION_MATCH -> job.jobControlOption?.customCondition
                JobRunCondition.CUSTOM_VARIABLE_MATCH -> ModelCommon.customVariableMatch(
                    job.jobControlOption?.customVariables
                )
                JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> ModelCommon.customVariableMatchNotRun(
                    job.jobControlOption?.customVariables
                )
                else -> null
            },
            steps = steps,
            timeoutMinutes = job.jobControlOption?.timeout.nullIfDefault(VariableDefault.DEFAULT_JOB_TIME_OUT),
            env = null,
            continueOnError = job.jobControlOption?.continueWhenFailed
                .nullIfDefault(VariableDefault.DEFAULT_CONTINUE_WHEN_FAILED),
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            dependOn = if (!job.jobControlOption?.dependOnId.isNullOrEmpty()) {
                job.jobControlOption?.dependOnId
            } else null
        )
    }

    override fun getRunsOn(
        job: VMBuildContainer
    ): RunsOn = when (val dispatchType = job.dispatchType) {
        is ThirdPartyAgentEnvDispatchType -> {
            RunsOn(
                selfHosted = true,
                poolName = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_AUTOMATIC_EXPORT_NOT_SUPPORTED
                ),
                container = null,
                agentSelector = listOf(job.baseOS.name.toLowerCase()),
                needs = job.buildEnv
            )
        }
        is DockerDispatchType -> {
            val (containerImage, credentials) = getImageNameAndCredentials(
                dispatchType
            )
            RunsOn(
                selfHosted = null,
                poolName = JobRunsOnType.DOCKER.type,
                container = Container2(
                    image = containerImage,
                    credentials = credentials,
                    options = null,
                    imagePullPolicy = null
                ),
                agentSelector = null,
                needs = job.buildEnv
            )
        }
        is PublicDevCloudDispathcType -> {
            val (containerImage, credentials) = getImageNameAndCredentials(
                dispatchType
            )
            RunsOn(
                selfHosted = null,
                poolName = JobRunsOnType.DOCKER.type,
                container = Container2(
                    image = containerImage,
                    credentials = credentials,
                    options = null,
                    imagePullPolicy = null
                ),
                agentSelector = null,
                needs = job.buildEnv
            )
        }
        is MacOSDispatchType -> {
            RunsOn(
                selfHosted = null,
                poolName = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_BUILD_CLUSTERS_THROUGH
                ) + I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_NOTE_DEFAULT_XCODE_VERSION
                ),
                container = null,
                agentSelector = null
            )
        }
        else -> {
            RunsOn(
                selfHosted = null,
                poolName = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_AUTOMATIC_EXPORT_NOT_SUPPORTED
                ),
                container = null,
                agentSelector = null
            )
        }
    }
}

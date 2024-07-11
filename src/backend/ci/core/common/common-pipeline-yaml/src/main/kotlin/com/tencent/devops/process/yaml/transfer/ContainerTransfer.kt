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

package com.tencent.devops.process.yaml.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.IfType
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.utils.TransferUtil
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_CONTINUE_WHEN_FAILED
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_JOB_MAX_QUEUE_MINUTES
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_MUTEX_QUEUE_LENGTH
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_MUTEX_TIMEOUT_MINUTES
import com.tencent.devops.process.yaml.transfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.transfer.inner.TransferCreator
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.v3.models.job.Container3
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnPoolType
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v3.models.job.Mutex
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.job.RunsOn
import com.tencent.devops.process.yaml.v3.models.job.Strategy
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("NestedBlockDepth", "ComplexMethod")
class ContainerTransfer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val transferCache: TransferCacheService,
    val dispatchTransfer: DispatchTransfer,
    val inner: TransferCreator
) {

    private val defaultRunsOn = JSONObject(
        RunsOn(
            selfHosted = null,
            poolType = null,
            poolName = JobRunsOnType.DOCKER.type,
            container = Container3(
                imageCode = inner.defaultImageCode,
                imageVersion = inner.defaultImageVersion
            )
        )
    )

    fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        projectCode: String,
        userId: String,
        finalStage: Boolean = false,
        jobEnable: Boolean = true,
        resources: Resources? = null,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ) {
        val buildEnv = if (job.runsOn.selfHosted == false) job.runsOn.needs?.ifEmpty { null } else null
        val (dispatchType, baseOS) = kotlin.runCatching {
            dispatchTransfer.makeDispatchType(
                job = job,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            )
        }.onFailure {
            if (it is OperationException) {
                throw PipelineTransferException(
                    CommonMessageCode.YAML_NOT_VALID,
                    arrayOf("${it.message}")
                )
            }
        }.getOrThrow()
        if (dispatchType is StoreDispatchType && dispatchType.imageType == ImageType.BKSTORE) {
            val imageName = transferCache.getStoreImageDetail(
                userId, dispatchType.imageCode!!, dispatchType.imageVersion
            )?.name
            dispatchType.imageName = imageName
        }
        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            mutexGroup = getMutexModel(job.mutex),
            baseOS = baseOS,
            vmNames = setOf(),
            maxQueueMinutes = DEFAULT_JOB_MAX_QUEUE_MINUTES,
            maxRunningMinutes = job.timeoutMinutes?.toIntOrNull() ?: VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES,
            buildEnv = buildEnv,
            customEnv = ModelCreateUtil.getCustomEnv(job.env),
            jobControlOption = getJobControlOption(
                job = job, jobEnable = jobEnable, finalStage = finalStage
            ),
            dispatchType = dispatchType,
            matrixGroupFlag = job.strategy != null,
            matrixControlOption = getMatrixControlOption(job)
        ).apply {
            nfsSwitch = buildEnv != null
        }
        containerList.add(vmContainer)
    }

    fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        jobEnable: Boolean = true,
        finalStage: Boolean = false
    ) {

        containerList.add(
            NormalContainer(
                jobId = job.id,
                name = job.name ?: "Job-${jobIndex + 1}",
                elements = elementList,
                jobControlOption = getJobControlOption(
                    job = job, jobEnable = jobEnable, finalStage = finalStage
                ),
                mutexGroup = getMutexModel(job.mutex),
                matrixGroupFlag = job.strategy != null,
                matrixControlOption = getMatrixControlOption(job)
            )
        )
    }

    fun addYamlNormalContainer(
        job: NormalContainer,
        steps: List<PreStep>?
    ): PreJob {
        return PreJob(
            name = job.name,
            runsOn = RunsOn(
                selfHosted = null,
                poolName = JobRunsOnType.AGENT_LESS.type,
                poolType = null
            ),
            mutex = getMutexYaml(job.mutexGroup),
            container = null,
            ifField = when (job.jobControlOption?.runCondition) {
                JobRunCondition.CUSTOM_CONDITION_MATCH -> job.jobControlOption?.customCondition
                JobRunCondition.CUSTOM_VARIABLE_MATCH -> TransferUtil.customVariableMatch(
                    job.jobControlOption?.customVariables
                )

                JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> TransferUtil.customVariableMatchNotRun(
                    job.jobControlOption?.customVariables
                )

                else -> null
            },
            steps = steps,
            timeoutMinutes = makeJobTimeout(job.jobControlOption),
            env = null,
            continueOnError = job.jobControlOption?.continueWhenFailed.nullIfDefault(DEFAULT_CONTINUE_WHEN_FAILED),
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            // 蓝盾这边是自定义Job ID
            dependOn = when (job.jobControlOption?.dependOnType) {
                DependOnType.ID -> job.jobControlOption?.dependOnId?.ifEmpty { null }
                DependOnType.NAME -> job.jobControlOption?.dependOnName?.ifBlank { null }?.split(",")
                else -> null
            }
        )
    }

    fun addYamlVMBuildContainer(
        userId: String,
        projectId: String,
        job: VMBuildContainer,
        steps: List<PreStep>?
    ): PreJob {
        return PreJob(
            name = job.name,
            runsOn = dispatchTransfer.makeRunsOn(job)?.fix(
                jobId = job.jobId.toString(),
                userId = userId,
                projectId = projectId,
                buildType = job.dispatchType?.buildType()
            ),
            container = null,
            services = null,
            mutex = getMutexYaml(job.mutexGroup),
            ifField = when (job.jobControlOption?.runCondition) {
                JobRunCondition.CUSTOM_CONDITION_MATCH -> job.jobControlOption?.customCondition
                JobRunCondition.CUSTOM_VARIABLE_MATCH -> TransferUtil.customVariableMatch(
                    job.jobControlOption?.customVariables
                )

                JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> TransferUtil.customVariableMatchNotRun(
                    job.jobControlOption?.customVariables
                )

                else -> null
            },
            steps = steps,
            timeoutMinutes = makeJobTimeout(job.jobControlOption),
            env = job.customEnv?.associateBy({ it.key ?: "" }) {
                it.value
            }?.ifEmpty { null },
            continueOnError = job.jobControlOption?.continueWhenFailed.nullIfDefault(DEFAULT_CONTINUE_WHEN_FAILED),
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            dependOn = when (job.jobControlOption?.dependOnType) {
                DependOnType.ID -> job.jobControlOption?.dependOnId?.ifEmpty { null }
                DependOnType.NAME -> job.jobControlOption?.dependOnName?.ifBlank { null }?.split(",")
                else -> null
            }
        )
    }

    private fun RunsOn.fix(jobId: String, userId: String, projectId: String, buildType: BuildType?): RunsOn? {
        /*修正私有构建机数据*/
        when (poolType) {
            JobRunsOnPoolType.AGENT_ID.name -> {
                nodeName = transferCache.getThirdPartyAgent(
                    poolType = JobRunsOnPoolType.AGENT_ID,
                    userId = userId,
                    projectId = projectId,
                    value = nodeName
                ) ?: throw PipelineTransferException(
                    CommonMessageCode.DISPATCH_NOT_SUPPORT_TRANSFER,
                    arrayOf("agentId: $nodeName")
                )
            }

            JobRunsOnPoolType.ENV_ID.name -> {
                poolName = transferCache.getThirdPartyAgent(
                    poolType = JobRunsOnPoolType.ENV_ID,
                    userId = userId,
                    projectId = envProjectId ?: projectId,
                    value = poolName
                ) ?: throw PipelineTransferException(
                    CommonMessageCode.DISPATCH_NOT_SUPPORT_TRANSFER,
                    arrayOf("envId: $poolName")
                )
            }
        }

        /*修正docker配额数据*/
        if (hwSpec != null && buildType != null) {
            kotlin.run {
                val res = transferCache.getDockerResource(userId, projectId, buildType)
                // hwSpec为0和1时为特殊值，表示默认配置Basic
                if (res?.default == hwSpec || hwSpec == "0" || hwSpec == "1") {
                    hwSpec = null
                    return@run
                }
                val hw = res?.dockerResourceOptionsMaps?.find {
                    it.id == hwSpec
                }
                hwSpec = hw?.dockerResourceOptionsShow?.description ?: throw PipelineTransferException(
                    CommonMessageCode.DISPATCH_NOT_SUPPORT_TRANSFER,
                    arrayOf("jobId:$jobId resource type not support transfer.[poolName:$poolName,hwSpec:$hwSpec]")
                )
            }
        }
        if (JSONObject(this).similar(defaultRunsOn)) {
            return null
        }
        return this
    }

    private fun makeJobTimeout(controlOption: JobControlOption?): String? {
        return controlOption?.timeoutVar.nullIfDefault(
            VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES.toString()
        ) ?: controlOption?.timeout.nullIfDefault(VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES)?.toString()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMatrixControlOption(job: Job): MatrixControlOption? {

        val strategy = job.strategy ?: return null

        with(strategy) {
            if (matrix is Map<*, *>) {
                val yaml = matrix as MutableMap<String, Any>
                val include = if ("include" in yaml.keys && yaml["include"] != null) {
                    YamlUtil.toYaml(yaml["include"]!!)
                } else {
                    null
                }
                val exclude = if ("exclude" in yaml.keys && yaml["exclude"] != null) {
                    YamlUtil.toYaml(yaml["exclude"]!!)
                } else {
                    null
                }
                val json = matrix
                json.remove("include")
                json.remove("exclude")

                return MatrixControlOption(
                    strategyStr = YamlUtil.toYaml(json),
                    includeCaseStr = include,
                    excludeCaseStr = exclude,
                    fastKill = fastKill,
                    maxConcurrency = maxParallel
                )
            } else {
                return MatrixControlOption(
                    strategyStr = matrix.toString(),
                    fastKill = fastKill,
                    maxConcurrency = maxParallel
                )
            }
        }
    }

    private fun getJobControlOption(
        job: Job,
        jobEnable: Boolean = true,
        finalStage: Boolean = false
    ): JobControlOption {
        val timeout = job.timeoutMinutes?.toIntOrNull() ?: VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES
        val timeoutVar = job.timeoutMinutes ?: VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES.toString()

        val dependOnName = job.dependOn?.joinToString(",")

        return if (finalStage) {
            JobControlOption(
                timeout = timeout,
                timeoutVar = timeoutVar,
                runCondition = when (job.ifField) {
                    IfType.SUCCESS.name -> JobRunCondition.PREVIOUS_STAGE_SUCCESS
                    IfType.FAILURE.name -> JobRunCondition.PREVIOUS_STAGE_FAILED
                    IfType.CANCELLED.name, IfType.CANCELED.name -> JobRunCondition.PREVIOUS_STAGE_CANCEL
                    else -> JobRunCondition.STAGE_RUNNING
                },
                dependOnType = DependOnType.NAME,
                dependOnName = dependOnName,
                prepareTimeout = job.runsOn.queueTimeoutMinutes ?: VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT,
                continueWhenFailed = job.continueOnError ?: DEFAULT_CONTINUE_WHEN_FAILED
            )
        } else {
            val runCondition = kotlin.run {
                if (!job.ifField.isNullOrBlank()) JobRunCondition.CUSTOM_CONDITION_MATCH else null
            } ?: JobRunCondition.STAGE_RUNNING
            JobControlOption(
                enable = jobEnable,
                timeout = timeout,
                timeoutVar = timeoutVar,
                runCondition = runCondition,
                customCondition = if (runCondition == JobRunCondition.CUSTOM_CONDITION_MATCH) {
                    job.ifField
                } else {
                    null
                },
                dependOnType = DependOnType.NAME,
                dependOnName = dependOnName,
                prepareTimeout = job.runsOn.queueTimeoutMinutes ?: VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT,
                continueWhenFailed = job.continueOnError ?: DEFAULT_CONTINUE_WHEN_FAILED
            )
        }
    }

    private fun getMutexModel(resource: Mutex?): MutexGroup? {
        if (resource == null) {
            return null
        }
        return MutexGroup(
            enable = true,
            mutexGroupName = resource.label,
            queueEnable = resource.queueLength != null,
            queue = resource.queueLength ?: DEFAULT_MUTEX_QUEUE_LENGTH,
            timeout = resource.timeoutMinutes?.toIntOrNull() ?: DEFAULT_MUTEX_TIMEOUT_MINUTES,
            timeoutVar = resource.timeoutMinutes ?: DEFAULT_MUTEX_TIMEOUT_MINUTES.toString()
        )
    }

    private fun getMutexYaml(resource: MutexGroup?): Mutex? {
        if (resource?.mutexGroupName.isNullOrBlank()) {
            return null
        }
        return Mutex(
            label = resource?.mutexGroupName!!,
            queueLength = if (resource.queueEnable) {
                resource.queue
            } else {
                null
            },
            timeoutMinutes = if (resource.queueEnable) {
                resource.timeoutVar.nullIfDefault(DEFAULT_MUTEX_TIMEOUT_MINUTES.toString())
                    ?: resource.timeout.nullIfDefault(DEFAULT_MUTEX_TIMEOUT_MINUTES)?.toString()
            } else {
                null
            }
        )
    }

    private fun getMatrixFromJob(
        matrixControlOption: MatrixControlOption?
    ): Strategy? {
        if (matrixControlOption == null) {
            return null
        }
        return Strategy(
            matrix = matrixControlOption.convertMatrixToYamlConfig() ?: return null,
            fastKill = matrixControlOption.fastKill,
            maxParallel = matrixControlOption.maxConcurrency
        )
    }
}

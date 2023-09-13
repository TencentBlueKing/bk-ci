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
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelCreate.ModelCommon
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_CONTINUE_WHEN_FAILED
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_JOB_MAX_QUEUE_MINUTES
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MUTEX_QUEUE_ENABLE
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MUTEX_QUEUE_LENGTH
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MUTEX_TIMEOUT_MINUTES
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.v3.models.IfType
import com.tencent.devops.process.yaml.v3.models.Resources
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v3.models.job.Mutex
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.job.RunsOn
import com.tencent.devops.process.yaml.v3.models.job.Strategy
import com.tencent.devops.process.yaml.v3.models.step.PreStep
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ContainerTransfer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    val transferCache: TransferCacheService,
    val dispatchTransfer: DispatchTransfer
) {

    fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        projectCode: String,
        finalStage: Boolean = false,
        jobEnable: Boolean = true,
        resources: Resources? = null,
        buildTemplateAcrossInfo: BuildTemplateAcrossInfo?
    ) {
        val buildEnv = if (job.runsOn.selfHosted == false) job.runsOn.needs?.ifEmpty { null } else null
        val (dispatchType, baseOS) = dispatchTransfer.makeDispatchType(
            job = job,
            buildTemplateAcrossInfo = buildTemplateAcrossInfo
        )
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
            customBuildEnv = job.env,
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
                JobRunCondition.CUSTOM_VARIABLE_MATCH -> ModelCommon.customVariableMatch(
                    job.jobControlOption?.customVariables
                )

                JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> ModelCommon.customVariableMatchNotRun(
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
                DependOnType.ID -> job.jobControlOption?.dependOnId
                DependOnType.NAME -> job.jobControlOption?.dependOnName?.split(",")
                else -> null
            }?.ifEmpty { null },
            dependOnType = when (job.jobControlOption?.dependOnType) {
                DependOnType.ID -> null
                DependOnType.NAME -> DependOnType.NAME.name
                else -> null
            }
        )
    }

    fun addYamlVMBuildContainer(
        job: VMBuildContainer,
        steps: List<PreStep>?
    ): PreJob {
        return PreJob(
            name = job.name,
            runsOn = dispatchTransfer.makeRunsOn(job),
            container = null,
            services = null,
            mutex = getMutexYaml(job.mutexGroup),
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
            timeoutMinutes = makeJobTimeout(job.jobControlOption),
            env = null,
            continueOnError = job.jobControlOption?.continueWhenFailed.nullIfDefault(DEFAULT_CONTINUE_WHEN_FAILED),
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            dependOn = when (job.jobControlOption?.dependOnType) {
                DependOnType.ID -> job.jobControlOption?.dependOnId
                DependOnType.NAME -> job.jobControlOption?.dependOnName?.split(",")
                else -> null
            }?.ifEmpty { null },
            dependOnType = when (job.jobControlOption?.dependOnType) {
                DependOnType.ID -> null
                DependOnType.NAME -> DependOnType.NAME.name
                else -> null
            }
        )
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

        val dependOnType = DependOnType.parse(job.dependOnType)
        val dependOnId = if (dependOnType == DependOnType.ID) {
            job.dependOn
        } else {
            null
        }
        val dependOnName = if (dependOnType == DependOnType.NAME) {
            job.dependOn?.joinToString(",")
        } else {
            null
        }
        return if (!job.ifField.isNullOrBlank()) {
            var customVariables: List<NameAndValue>? = null
            if (finalStage) {
                JobControlOption(
                    timeout = timeout,
                    timeoutVar = timeoutVar,
                    runCondition = when (job.ifField) {
                        IfType.SUCCESS.name -> JobRunCondition.PREVIOUS_STAGE_SUCCESS
                        IfType.FAILURE.name -> JobRunCondition.PREVIOUS_STAGE_FAILED
                        IfType.CANCELLED.name, IfType.CANCELED.name -> JobRunCondition.PREVIOUS_STAGE_CANCEL
                        else -> JobRunCondition.STAGE_RUNNING
                    },
                    dependOnType = dependOnType,
                    dependOnId = dependOnId,
                    dependOnName = dependOnName,
                    prepareTimeout = job.runsOn.queueTimeoutMinutes ?: VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT,
                    continueWhenFailed = job.continueOnError
                )
            } else {
                val runCondition = ModelCommon.revertCustomVariableMatch(job.ifField)?.let {
                    customVariables = it
                    JobRunCondition.CUSTOM_VARIABLE_MATCH
                } ?: ModelCommon.revertCustomVariableNotMatch(job.ifField)?.let {
                    customVariables = it
                    JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
                } ?: kotlin.run {
                    if (!job.ifField.isNullOrBlank()) JobRunCondition.CUSTOM_CONDITION_MATCH else null
                } ?: JobRunCondition.STAGE_RUNNING
                JobControlOption(
                    enable = jobEnable,
                    timeout = timeout,
                    timeoutVar = timeoutVar,
                    runCondition = runCondition,
                    customCondition = if (runCondition == JobRunCondition.CUSTOM_CONDITION_MATCH) {
                        ModelCreateUtil.removeIfBrackets(job.ifField)
                    } else {
                        null
                    },
                    dependOnType = dependOnType,
                    dependOnId = dependOnId,
                    dependOnName = dependOnName,
                    prepareTimeout = job.runsOn.queueTimeoutMinutes ?: VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT,
                    continueWhenFailed = job.continueOnError,
                    customVariables = customVariables
                )
            }
        } else {
            JobControlOption(
                enable = jobEnable,
                timeout = timeout,
                timeoutVar = timeoutVar,
                dependOnType = dependOnType,
                dependOnId = dependOnId,
                dependOnName = dependOnName,
                prepareTimeout = job.runsOn.queueTimeoutMinutes ?: VariableDefault.DEFAULT_JOB_PREPARE_TIMEOUT,
                continueWhenFailed = job.continueOnError
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
            queueEnable = resource.queueEnable ?: DEFAULT_MUTEX_QUEUE_ENABLE,
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
            queueEnable = resource.queueEnable.nullIfDefault(DEFAULT_MUTEX_QUEUE_ENABLE),
            queueLength = resource.queue.nullIfDefault(DEFAULT_MUTEX_QUEUE_LENGTH),
            timeoutMinutes = resource.timeoutVar.nullIfDefault(DEFAULT_MUTEX_TIMEOUT_MINUTES.toString())
                ?: resource.timeout.nullIfDefault(DEFAULT_MUTEX_TIMEOUT_MINUTES)?.toString()
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

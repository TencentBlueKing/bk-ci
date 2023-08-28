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
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelCreate.ModelCommon
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_CONTINUE_WHEN_FAILED
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_JOB_MAX_QUEUE_MINUTES
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_JOB_MAX_RUNNING_MINUTES
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_JOB_TIME_OUT
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MUTEX_QUEUE_ENABLE
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MUTEX_QUEUE_LENGTH
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MUTEX_TIMEOUT_MINUTES
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.job.Mutex
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.models.job.Strategy
import com.tencent.devops.process.yaml.v2.models.step.PreStep
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
        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            mutexGroup = getMutexModel(job.mutex),
            baseOS = getBaseOs(job),
            vmNames = setOf(),
            maxQueueMinutes = DEFAULT_JOB_MAX_QUEUE_MINUTES,
            maxRunningMinutes = job.timeoutMinutes ?: DEFAULT_JOB_MAX_RUNNING_MINUTES,
            buildEnv = if (job.runsOn.selfHosted == false) job.runsOn.needs else null,
            customBuildEnv = job.env,
            jobControlOption = getJobControlOption(
                job = job, jobEnable = jobEnable, finalStage = finalStage
            ),
            dispatchType = dispatchTransfer.makeDispatchType(
                job = job,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            ),
            matrixGroupFlag = job.strategy != null,
            matrixControlOption = getMatrixControlOption(job)
        )
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
                id = job.id,
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
            timeoutMinutes = job.jobControlOption?.timeout.nullIfDefault(DEFAULT_JOB_TIME_OUT),
            env = null,
            continueOnError = job.jobControlOption?.continueWhenFailed.nullIfDefault(DEFAULT_CONTINUE_WHEN_FAILED),
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            // 蓝盾这边是自定义Job ID
            dependOn = if (!job.jobControlOption?.dependOnId.isNullOrEmpty()) {
                job.jobControlOption?.dependOnId
            } else null
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
            timeoutMinutes = job.jobControlOption?.timeout.nullIfDefault(DEFAULT_JOB_TIME_OUT),
            env = null,
            continueOnError = job.jobControlOption?.continueWhenFailed.nullIfDefault(DEFAULT_CONTINUE_WHEN_FAILED),
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            dependOn = if (!job.jobControlOption?.dependOnId.isNullOrEmpty()) {
                job.jobControlOption?.dependOnId
            } else null
        )
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
        val timeout = setUpTimeout(job)
        return if (!job.ifField.isNullOrBlank()) {
            if (finalStage) {
                JobControlOption(
                    timeout = timeout,
                    timeoutVar = timeout.toString(),
                    runCondition = when (job.ifField) {
                        IfType.SUCCESS.name -> JobRunCondition.PREVIOUS_STAGE_SUCCESS
                        IfType.FAILURE.name -> JobRunCondition.PREVIOUS_STAGE_FAILED
                        IfType.CANCELLED.name, IfType.CANCELED.name -> JobRunCondition.PREVIOUS_STAGE_CANCEL
                        else -> JobRunCondition.STAGE_RUNNING
                    },
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    prepareTimeout = job.runsOn.queueTimeoutMinutes,
                    continueWhenFailed = job.continueOnError
                )
            } else {
                JobControlOption(
                    enable = jobEnable,
                    timeout = timeout,
                    timeoutVar = timeout.toString(),
                    runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = ModelCreateUtil.removeIfBrackets(job.ifField),
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    prepareTimeout = job.runsOn.queueTimeoutMinutes,
                    continueWhenFailed = job.continueOnError
                )
            }
        } else {
            JobControlOption(
                enable = jobEnable,
                timeout = timeout,
                timeoutVar = timeout.toString(),
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn,
                prepareTimeout = job.runsOn.queueTimeoutMinutes,
                continueWhenFailed = job.continueOnError
            )
        }
    }

    private fun setUpTimeout(job: Job) = (job.timeoutMinutes ?: DEFAULT_JOB_TIME_OUT)

    private fun getMutexModel(resource: Mutex?): MutexGroup? {
        if (resource == null) {
            return null
        }
        return MutexGroup(
            enable = true,
            mutexGroupName = resource.label,
            queueEnable = resource.queueEnable ?: DEFAULT_MUTEX_QUEUE_ENABLE,
            queue = resource.queueLength ?: DEFAULT_MUTEX_QUEUE_LENGTH,
            timeout = resource.timeoutMinutes ?: DEFAULT_MUTEX_TIMEOUT_MINUTES
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
            timeoutMinutes = resource.timeout.nullIfDefault(DEFAULT_MUTEX_TIMEOUT_MINUTES)
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

    companion object {
        val LINUX_TYPE = setOf("docker", "linux")
        val MACOS_TYPE = setOf("macos-11.4", "macos-12.4", "macos-latest", "macos")
        val WINDOWS_TYPE = setOf("windows-2016", "windows")
    }
}

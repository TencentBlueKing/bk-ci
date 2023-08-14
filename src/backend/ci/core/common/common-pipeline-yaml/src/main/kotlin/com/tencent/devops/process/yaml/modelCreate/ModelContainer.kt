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
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_ENV_NOT_YET_SUPPORTED
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.matrix.DispatchInfo
import com.tencent.devops.common.pipeline.matrix.MatrixConfig.Companion.MATRIX_CONTEXT_KEY_PREFIX
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.yaml.modelCreate.inner.InnerModelCreator
import com.tencent.devops.process.yaml.modelTransfer.TransferCacheService
import com.tencent.devops.process.yaml.pojo.StreamDispatchInfo
import com.tencent.devops.process.yaml.utils.ModelCreateUtil
import com.tencent.devops.process.yaml.utils.StreamDispatchUtils
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.job.Mutex
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.models.job.Strategy
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response

@Component
class ModelContainer @Autowired(required = false) constructor(
    val client: Client,
    val objectMapper: ObjectMapper,
    @Autowired(required = false)
    val inner: InnerModelCreator?,
    val transferCache: TransferCacheService
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
        doSomeCheck(job, StreamDispatchUtils.getBaseOs(job))
        val defaultImage = inner!!.defaultImage
        val dispatchInfo = if (JsonUtil.toJson(job.runsOn).contains("\${{ $MATRIX_CONTEXT_KEY_PREFIX")) {
            StreamDispatchInfo(
                name = "dispatchInfo_${job.id}",
                job = job,
                projectCode = projectCode,
                defaultImage = defaultImage,
                resources = resources
            )
        } else null
        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            mutexGroup = getMutexGroup(job.mutex),
            baseOS = StreamDispatchUtils.getBaseOs(job),
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = job.timeoutMinutes ?: 900,
            buildEnv = StreamDispatchUtils.getBuildEnv(job),
            customBuildEnv = job.env,
            jobControlOption = getJobControlOption(
                job = job, jobEnable = jobEnable, finalStage = finalStage
            ),
            dispatchType = StreamDispatchUtils.getDispatchType(
                job = job,
                defaultImage = defaultImage,
                containsMatrix = dispatchInfo != null,
                buildTemplateAcrossInfo = buildTemplateAcrossInfo
            ),
            matrixGroupFlag = job.strategy != null,
            matrixControlOption = getMatrixControlOption(job, dispatchInfo)
        )
        containerList.add(vmContainer)
    }

    fun doSomeCheck(job: Job, os: VMBaseOS) {
        if (os == VMBaseOS.ALL) {
            // all 不检查
            return
        }
        // 检查挂载版本是否支持(此处只检查未使用上下文的方式, 使用了上下文就将在引擎执行时检查)
        job.runsOn.needs?.forEach { env ->
            if (env.value.startsWith("$")) return@forEach
            client.get(ServiceContainerAppResource::class).getBuildEnv(
                name = env.key,
                version = env.value,
                os = os.name.lowercase()
            ).data ?: throw CustomException(
                // 说明用户填写的name或version不对，直接抛错
                Response.Status.BAD_REQUEST,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_ENV_NOT_YET_SUPPORTED,
                    params = arrayOf(env.key, env.value)
                )
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getMatrixControlOption(job: Job, dispatchInfo: DispatchInfo?): MatrixControlOption? {

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
                    maxConcurrency = maxParallel,
                    customDispatchInfo = dispatchInfo
                )
            } else {
                return MatrixControlOption(
                    strategyStr = matrix.toString(),
                    fastKill = fastKill,
                    maxConcurrency = maxParallel,
                    customDispatchInfo = dispatchInfo
                )
            }
        }
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
                jobControlOption = getJobControlOption(
                    job = job, jobEnable = jobEnable, finalStage = finalStage
                ),
                mutexGroup = getMutexGroup(job.mutex),
                matrixGroupFlag = job.strategy != null,
                matrixControlOption = getMatrixControlOption(job, null)
            )
        )
    }

    fun getJobControlOption(
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

    private fun setUpTimeout(job: Job) = (job.timeoutMinutes ?: 480)

    fun getMutexGroup(resource: Mutex?): MutexGroup? {
        if (resource == null) {
            return null
        }
        return MutexGroup(
            enable = true,
            mutexGroupName = resource.label,
            queueEnable = true,
            queue = resource.queueLength ?: 0,
            timeout = resource.timeoutMinutes ?: 10
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
                container = null
            ),
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
            timeoutMinutes = job.jobControlOption?.timeout,
            env = null,
            continueOnError = if (job.jobControlOption?.continueWhenFailed == true) true else null,
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
            timeoutMinutes = job.jobControlOption?.timeout,
            env = null,
            continueOnError = if (job.jobControlOption?.continueWhenFailed == true) true else null,
            strategy = if (job.matrixGroupFlag == true) {
                getMatrixFromJob(job.matrixControlOption)
            } else null,
            dependOn = if (!job.jobControlOption?.dependOnId.isNullOrEmpty()) {
                job.jobControlOption?.dependOnId
            } else null
        )
    }

    fun getRunsOn(
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
            // todo 凭据是否处理
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

    fun getImageNameAndCredentials(
        dispatchType: StoreDispatchType
    ): Pair<String, String?> {
        try {
            when (dispatchType.imageType) {
                ImageType.BKSTORE -> {
                    val imageRepoInfo = transferCache.getStoreImageInfo(
                        imageCode = dispatchType.imageCode ?: "",
                        imageVersion = dispatchType.imageVersion
                    ) ?: return Pair(dispatchType.imageCode ?: "", null)
                    val completeImageName = if (ImageType.BKDEVOPS == imageRepoInfo.sourceType) {
                        // 蓝盾项目源镜像
                        "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                    } else {
                        // 第三方源镜像
                        // dockerhub镜像名称不带斜杠前缀
                        if (imageRepoInfo.repoUrl.isBlank()) {
                            imageRepoInfo.repoName
                        } else {
                            "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                        }
                    } + ":" + imageRepoInfo.repoTag
                    return if (imageRepoInfo.publicFlag) {
                        Pair(completeImageName, null)
                    } else Pair(
                        completeImageName, imageRepoInfo.ticketId
                    )
                }
                ImageType.BKDEVOPS -> {
                    // 针对非商店的旧数据处理
                    return if (dispatchType.value != DockerVersion.TLINUX1_2.value &&
                        dispatchType.value != DockerVersion.TLINUX2_2.value
                    ) {
                        dispatchType.dockerBuildVersion = "bkdevops/" + dispatchType.value
                        Pair("bkdevops/" + dispatchType.value, null)
                    } else {
                        Pair(
                            MessageUtil.getMessageByLocale(
                                messageCode = ProcessMessageCode.BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE,
                                language = I18nUtil.getLanguage()
                            ), null
                        )
                    }
                }
                else -> {
                    return if (dispatchType.credentialId.isNullOrBlank()) {
                        Pair(dispatchType.value, null)
                    } else Pair(
                        dispatchType.value, dispatchType.credentialId
                    )
                }
            }
        } catch (e: Exception) {
            return Pair(
                MessageUtil.getMessageByLocale(
                    messageCode = ProcessMessageCode.BK_ENTER_URL_ADDRESS_IMAGE,
                    language = I18nUtil.getLanguage()
                ), null
            )
        }
    }

    fun getMatrixFromJob(
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

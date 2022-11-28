package com.tencent.devops.process.service.pipelineExport

import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.exsi.ESXiDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.process.pojo.JobPipelineExportV2YamlConflictMapBaseItem
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.models.job.Strategy
import org.slf4j.LoggerFactory

object ExportJob {
    private val logger = LoggerFactory.getLogger(ExportJob::class.java)

    @Suppress("ComplexMethod", "LongMethod")
    fun getV2JobFromStage(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext,
        stage: Stage,
        pipelineExportV2YamlConflictMapItem: PipelineExportV2YamlConflictMapItem
    ): Map<String, PreJob>? {
        val jobs = mutableMapOf<String, PreJob>()
        stage.containers.forEach {
            val jobKey = if (!it.jobId.isNullOrBlank()) {
                it.jobId!!
            } else if (!it.id.isNullOrBlank()) {
                "job_${it.id!!}"
            } else {
                "unknown_job"
            }
            pipelineExportV2YamlConflictMapItem.job =
                JobPipelineExportV2YamlConflictMapBaseItem(
                    id = it.id,
                    name = it.name,
                    jobId = it.jobId
                )
            when (it.getClassType()) {
                NormalContainer.classType -> {
                    val job = it as NormalContainer
                    val timeoutMinutes = job.jobControlOption?.timeout ?: 480
                    jobs[jobKey] = PreJob(
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
                            JobRunCondition.CUSTOM_VARIABLE_MATCH -> {
                                val ifString =
                                    ExportCondition.parseNameAndValueWithAnd(
                                        context = context,
                                        nameAndValueList = job.jobControlOption?.customVariables,
                                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                    )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                                val ifString = ExportCondition.parseNameAndValueWithOr(
                                    context = context,
                                    nameAndValueList = job.jobControlOption?.customVariables,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            else -> null
                        },
                        steps = ExportStep.getV2StepFromJob(
                            allInfo = allInfo,
                            context = context,
                            job = job,
                            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                        ),
                        timeoutMinutes = if (timeoutMinutes < 480) timeoutMinutes else null,
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
                VMBuildContainer.classType -> {
                    val job = it as VMBuildContainer
                    val timeoutMinutes = job.jobControlOption?.timeout ?: 480

                    // 编译环境的相关映射处理
                    val runsOn = when (val dispatchType = getDispatchType(job)) {
                        is ThirdPartyAgentEnvDispatchType -> {
                            RunsOn(
                                selfHosted = true,
                                poolName = "### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###",
                                container = null,
                                agentSelector = listOf(job.baseOS.name.toLowerCase()),
                                needs = job.buildEnv
                            )
                        }
                        is DockerDispatchType -> {
                            val (containerImage, credentials) = allInfo.getImageNameAndCredentials(
                                allInfo.userId,
                                allInfo.pipelineInfo.projectId,
                                allInfo.pipelineInfo.pipelineId,
                                dispatchType
                            )
                            RunsOn(
                                selfHosted = null,
                                poolName = JobRunsOnType.DOCKER.type,
                                container = Container2(
                                    image = containerImage,
                                    credentials = credentials
                                ),
                                agentSelector = null,
                                needs = job.buildEnv
                            )
                        }
                        is PublicDevCloudDispathcType -> {
                            val (containerImage, credentials) = allInfo.getImageNameAndCredentials(
                                allInfo.userId,
                                allInfo.pipelineInfo.projectId,
                                allInfo.pipelineInfo.pipelineId,
                                dispatchType
                            )
                            RunsOn(
                                selfHosted = null,
                                poolName = JobRunsOnType.DOCKER.type,
                                container = Container2(
                                    image = containerImage,
                                    credentials = credentials
                                ),
                                agentSelector = null,
                                needs = job.buildEnv
                            )
                        }
                        is MacOSDispatchType -> {
                            RunsOn(
                                selfHosted = null,
                                poolName = "### 可以通过 runs-on: macos-10.15 使用macOS公共构建集群。" +
                                    "注意默认的Xcode版本为12.2，若需自定义，请在JOB下自行执行 xcode-select 命令切换 ###",
                                container = null,
                                agentSelector = null
                            )
                        }
                        else -> {
                            RunsOn(
                                selfHosted = null,
                                poolName = "### 该环境不支持自动导出，请参考 https://iwiki.woa.com/x/2ebDKw 手动配置 ###",
                                container = null,
                                agentSelector = null
                            )
                        }
                    }

                    jobs[jobKey] = PreJob(
                        name = job.name,
                        runsOn = runsOn,
                        container = null,
                        services = null,
                        ifField = when (job.jobControlOption?.runCondition) {
                            JobRunCondition.CUSTOM_CONDITION_MATCH -> job.jobControlOption?.customCondition
                            JobRunCondition.CUSTOM_VARIABLE_MATCH -> {
                                val ifString =
                                    ExportCondition.parseNameAndValueWithAnd(
                                        context = context,
                                        nameAndValueList = job.jobControlOption?.customVariables,
                                        pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                    )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN -> {
                                val ifString = ExportCondition.parseNameAndValueWithOr(
                                    context = context,
                                    nameAndValueList = job.jobControlOption?.customVariables,
                                    pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                                )
                                if (job.jobControlOption?.customVariables?.isEmpty() == true) null
                                else ifString
                            }
                            else -> null
                        },
                        steps = ExportStep.getV2StepFromJob(
                            allInfo = allInfo,
                            context = context,
                            job = job,
                            pipelineExportV2YamlConflictMapItem = pipelineExportV2YamlConflictMapItem
                        ),
                        timeoutMinutes = if (timeoutMinutes < 480) timeoutMinutes else null,
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
                else -> {
                    logger.error("get jobs from stage failed, unknown classType:(${it.getClassType()})")
                }
            }
        }
        return if (jobs.isEmpty()) null else jobs
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

    /**
     * 新版的构建环境直接传入指定的构建机方式
     */
    private fun getDispatchType(param: VMBuildContainer): DispatchType {
        if (param.dispatchType != null) {
            return param.dispatchType!!
        } else {
            // 第三方构建机ID
            val agentId = param.thirdPartyAgentId ?: ""
            // 构建环境ID
            val envId = param.thirdPartyAgentEnvId ?: ""
            val workspace = param.thirdPartyWorkspace ?: ""
            return if (agentId.isNotBlank()) {
                ThirdPartyAgentIDDispatchType(
                    displayName = agentId,
                    workspace = workspace,
                    agentType = AgentType.ID,
                    dockerInfo = null
                )
            } else if (envId.isNotBlank()) {
                ThirdPartyAgentEnvDispatchType(
                    envName = envId,
                    envProjectId = null,
                    workspace = workspace,
                    agentType = AgentType.ID,
                    dockerInfo = null
                )
            } // docker建机指定版本(旧)
            else if (!param.dockerBuildVersion.isNullOrBlank()) {
                DockerDispatchType(param.dockerBuildVersion!!)
            } else {
                ESXiDispatchType()
            }
        }
    }
}

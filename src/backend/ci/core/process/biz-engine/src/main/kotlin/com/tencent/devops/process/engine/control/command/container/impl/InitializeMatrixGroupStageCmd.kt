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

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.api.constant.TEMPLATE_ACROSS_INFO_ID
import com.tencent.devops.common.api.exception.DependNotFoundException
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.matrix.MatrixConfig
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption.Companion.MATRIX_CASE_MAX_COUNT
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.atom.parser.DispatchTypeParser
import com.tencent.devops.process.engine.cfg.ModelContainerIdGenerator
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.context.MatrixBuildContext
import com.tencent.devops.process.engine.control.command.CmdFlowState
import com.tencent.devops.process.engine.control.command.container.ContainerCmd
import com.tencent.devops.process.engine.control.command.container.ContainerContext
import com.tencent.devops.process.engine.control.lock.ContainerIdLock
import com.tencent.devops.process.engine.control.lock.StageMatrixLock
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.service.PipelineBuildTemplateAcrossInfoService
import com.tencent.devops.process.utils.PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_STAGE_CONTAINERS_COUNT_MAX
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.min

/**
 * Stage计算构建矩阵事件命令处理
 */
@Suppress(
    "ComplexMethod",
    "LongMethod",
    "ReturnCount",
    "NestedBlockDepth",
    "ThrowsCount",
    "LongParameterList",
    "LargeClass"
)
@Service
class InitializeMatrixGroupStageCmd(
    private val dslContext: DSLContext,
    private val containerBuildDetailService: ContainerBuildDetailService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val templateAcrossInfoService: PipelineBuildTemplateAcrossInfoService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val dispatchTypeParser: DispatchTypeParser,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(InitializeMatrixGroupStageCmd::class.java)
    }

    override fun canExecute(commandContext: ContainerContext): Boolean {
        // 仅在初次准备并发执行Stage下Container是执行
        return commandContext.cmdFlowState == CmdFlowState.CONTINUE &&
            commandContext.container.matrixGroupFlag == true &&
            commandContext.container.status.isReadyToRun()
    }

    override fun execute(commandContext: ContainerContext) {

        // 在下发构建机任务前进行构建矩阵计算
        val parentContainer = commandContext.container
        val count = try {
            buildLogPrinter.addLine(
                buildId = parentContainer.buildId,
                message = "Start preparing to generate build matrix...",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                jobId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount
            )
            commandContext.buildStatus = BuildStatus.RUNNING
            val stageLock = StageMatrixLock(redisOperation, parentContainer.buildId, parentContainer.stageId)
            stageLock.use {
                // #6440 只有一个stage下出现多个时需要进行并发锁
                if (commandContext.stageMatrixCount > 1) it.lock()
                generateMatrixGroup(commandContext, parentContainer)
            }
        } catch (ignore: Throwable) {
            LOG.error(
                "ENGINE|${parentContainer.buildId}|MATRIX_CONTAINER_INIT_FAILED|" +
                    "matrix(${parentContainer.containerId})|" +
                    "parentContainer=$parentContainer",
                ignore
            )
            buildLogPrinter.addRedLine(
                buildId = parentContainer.buildId,
                message = "Abnormal matrix calculation: ${ignore.message}",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                jobId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount
            )
            0
        }

        LOG.info(
            "ENGINE|${parentContainer.buildId}|MATRIX_CONTAINER_INIT|" +
                "matrix(${parentContainer.containerId})|newContainerCount=$count"
        )

        if (count > 0) {
            commandContext.latestSummary = "Matrix(${parentContainer.containerId}) generateNew($count)"
        } else {
            commandContext.buildStatus = BuildStatus.FAILED
            commandContext.cmdFlowState = CmdFlowState.FINALLY
            commandContext.latestSummary = "j(${parentContainer.containerId}) matrix failed"
        }
    }

    private fun generateMatrixGroup(
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer
    ): Int {

        val event = commandContext.event
        val variables = commandContext.variables
        val asCodeEnabled = commandContext.pipelineAsCodeEnabled ?: false
        val modelStage = containerBuildDetailService.getBuildModel(
            projectId = parentContainer.projectId,
            buildId = parentContainer.buildId
        )?.getStage(parentContainer.stageId) ?: throw DependNotFoundException(
            "stage(${parentContainer.stageId}) cannot be found in model"
        )
        val modelContainer = modelStage.getContainer(
            vmSeqId = parentContainer.seq.toString()
        ) ?: throw DependNotFoundException(
            "container(${parentContainer.containerId}) cannot be found in model"
        )
        val recordContainer = containerBuildRecordService.getRecord(
            transactionContext = null,
            projectId = parentContainer.projectId,
            pipelineId = parentContainer.pipelineId,
            buildId = parentContainer.buildId,
            containerId = parentContainer.containerId,
            executeCount = parentContainer.executeCount
        )
        // #4518 待生成的分裂后container表和task表记录
        val buildContainerList = mutableListOf<PipelineBuildContainer>()
        val buildTaskList = mutableListOf<PipelineBuildTask>()
        val recordContainerList = mutableListOf<BuildRecordContainer>()
        val recordTaskList = mutableListOf<BuildRecordTask>()

        // #4518 根据当前上下文对每一个构建矩阵进行裂变
        val groupContainers = mutableListOf<PipelineBuildContainer>()
        val matrixGroupId = parentContainer.containerId
        val context = MatrixBuildContext(
            userId = event.userId,
            executeCount = commandContext.executeCount,
            // #4518 裂变的容器的seq id需要以父容器的seq id作为前缀
            containerSeq = VMUtils.genMatrixContainerSeq(matrixGroupId.toInt(), 1)
        )

        LOG.info(
            "ENGINE|${event.buildId}|${event.source}|INIT_MATRIX_CONTAINER|${event.stageId}|" +
                "matrixGroupId=$matrixGroupId|containerHashId=${modelContainer.containerHashId}" +
                "|context=$context|asCodeEnabled=$asCodeEnabled"
        )

        val matrixOption: MatrixControlOption
        val matrixConfig: MatrixConfig
        val contextCaseList: List<Map<String, String>>
        val jobControlOption: JobControlOption

        val matrixJobIds = mutableListOf<String>()
        val matrixTaskIds = mutableListOf<String>()

        // 每一种上下文组合都是一个新容器
        when (modelContainer) {
            is VMBuildContainer -> {

                jobControlOption = modelContainer.jobControlOption!!.copy(
                    dependOnType = null,
                    dependOnId = null,
                    dependOnName = null,
                    dependOnContainerId2JobIds = null
                )
                matrixOption = checkAndFetchOption(modelContainer.matrixControlOption)
                matrixConfig = matrixOption.convertMatrixConfig(variables, asCodeEnabled)
                contextCaseList = matrixConfig.getAllCombinations()

                if (contextCaseList.size > MATRIX_CASE_MAX_COUNT) {
                    throw ExecuteException(
                        "Matrix case(${contextCaseList.size}) exceeds " +
                            "the limit($MATRIX_CASE_MAX_COUNT)"
                    )
                }

                contextCaseList.forEach { contextCase ->
                    val contextPair = if (asCodeEnabled) {
                        EnvReplacementParser.getCustomExecutionContextByMap(variables)
                    } else null
                    // 包括matrix.xxx的所有上下文，矩阵生成的要覆盖原变量
                    val allContext = (modelContainer.customBuildEnv ?: mapOf()).plus(contextCase)

                    // 对自定义构建环境的做特殊解析
                    // customDispatchType决定customBaseOS是否计算，请勿填充默认值
                    val parsedInfo = matrixOption.customDispatchInfo?.let { self ->
                        dispatchTypeParser.parseInfo(
                            projectId = parentContainer.projectId,
                            pipelineId = parentContainer.pipelineId,
                            buildId = parentContainer.buildId,
                            customInfo = self,
                            context = allContext
                        )
                    }
                    val customDispatchType = parsedInfo?.dispatchType
                    val customBaseOS = parsedInfo?.baseOS
                    val customBuildEnv = parsedInfo?.buildEnv
                    val mutexGroup = modelContainer.mutexGroup?.let { self ->
                        self.copy(
                            mutexGroupName = EnvReplacementParser.parse(
                                self.mutexGroupName, allContext, asCodeEnabled, contextPair
                            ),
                            linkTip = EnvReplacementParser.parse(self.linkTip, allContext, asCodeEnabled, contextPair)
                        )
                    }
                    val newSeq = context.containerSeq++
                    val innerSeq = context.innerSeq++

                    // 刷新所有插件的ID，并生成对应的纯状态插件
                    val postParentIdMap = mutableMapOf<String, String>()
                    val statusElements = generateMatrixElements(
                        modelContainer.elements, context.executeCount, postParentIdMap, matrixTaskIds
                    )
                    val newContainer = VMBuildContainer(
                        name = EnvReplacementParser.parse(modelContainer.name, allContext, asCodeEnabled, contextPair),
                        id = newSeq.toString(),
                        containerId = newSeq.toString(),
                        containerHashId = modelContainerIdGenerator.getNextId(),
                        jobId = modelContainer.jobId?.let { self -> VMUtils.genMatrixJobId(self, innerSeq) },
                        matrixGroupId = matrixGroupId,
                        matrixContext = contextCase,
                        elements = modelContainer.elements,
                        canRetry = modelContainer.canRetry,
                        enableExternal = modelContainer.enableExternal,
                        jobControlOption = jobControlOption,
                        mutexGroup = mutexGroup,
                        executeCount = context.executeCount,
                        containPostTaskFlag = modelContainer.containPostTaskFlag,
                        customBuildEnv = allContext,
                        baseOS = customBaseOS ?: modelContainer.baseOS,
                        vmNames = modelContainer.vmNames,
                        dockerBuildVersion = modelContainer.dockerBuildVersion,
                        dispatchType = customDispatchType ?: modelContainer.dispatchType?.let { itd ->
                            itd.replaceVariable(allContext) // 只处理${{matrix.xxx}}, 其余在DispatchVMStartupTaskAtom处理
                            itd
                        },
                        buildEnv = customBuildEnv ?: modelContainer.buildEnv,
                        thirdPartyAgentId = modelContainer.thirdPartyAgentId?.let { self ->
                            EnvReplacementParser.parse(self, allContext, asCodeEnabled, contextPair)
                        },
                        thirdPartyAgentEnvId = modelContainer.thirdPartyAgentEnvId?.let { self ->
                            EnvReplacementParser.parse(self, allContext, asCodeEnabled, contextPair)
                        },
                        thirdPartyWorkspace = modelContainer.thirdPartyWorkspace?.let { self ->
                            EnvReplacementParser.parse(self, allContext, asCodeEnabled, contextPair)
                        }
                    )
                    newContainer.jobId?.let { matrixJobIds.add(it) }
                    groupContainers.add(
                        pipelineContainerService.prepareMatrixBuildContainer(
                            projectId = event.projectId,
                            pipelineId = event.pipelineId,
                            buildId = event.buildId,
                            container = newContainer,
                            stage = modelStage,
                            context = context,
                            buildTaskList = buildTaskList,
                            recordTaskList = recordTaskList,
                            resourceVersion = recordContainer?.resourceVersion,
                            jobControlOption = jobControlOption,
                            matrixGroupId = matrixGroupId,
                            postParentIdMap = postParentIdMap,
                            mutexGroup = mutexGroup
                        )
                    )
                    recordContainer?.let {
                        val containerVar = mutableMapOf<String, Any>(
                            "@type" to newContainer.getClassType(),
                            newContainer::containerHashId.name to (newContainer.containerHashId ?: ""),
                            newContainer::name.name to (newContainer.name),
                            newContainer::matrixGroupId.name to matrixGroupId,
                            newContainer::matrixContext.name to contextCase
                        )
                        modelContainer.mutexGroup?.let {
                            containerVar[newContainer::mutexGroup.name] = it
                        }
                        recordContainerList.add(
                            BuildRecordContainer(
                                projectId = event.projectId,
                                pipelineId = event.pipelineId,
                                resourceVersion = recordContainer.resourceVersion,
                                buildId = event.buildId,
                                stageId = event.stageId,
                                containerId = newContainer.containerId!!,
                                containerType = recordContainer.containerType,
                                executeCount = context.executeCount,
                                matrixGroupFlag = false,
                                matrixGroupId = matrixGroupId,
                                containerVar = containerVar,
                                status = newContainer.status,
                                startTime = null,
                                endTime = null,
                                timestamps = mapOf()
                            )
                        )
                    }

                    // 如为空就初始化，如有元素就直接追加
                    if (modelContainer.groupContainers.isNullOrEmpty()) {
                        modelContainer.groupContainers = mutableListOf(
                            newContainer.copy(elements = statusElements)
                        )
                    } else {
                        modelContainer.groupContainers!!.add(
                            newContainer.copy(elements = statusElements)
                        )
                    }
                }
            }
            is NormalContainer -> {

                jobControlOption = modelContainer.jobControlOption!!.copy(
                    dependOnType = null,
                    dependOnId = null,
                    dependOnName = null,
                    dependOnContainerId2JobIds = null
                )
                matrixOption = checkAndFetchOption(modelContainer.matrixControlOption)
                matrixConfig = matrixOption.convertMatrixConfig(variables, asCodeEnabled)
                contextCaseList = matrixConfig.getAllCombinations()

                contextCaseList.forEach { contextCase ->

                    // 刷新所有插件的ID，并生成对应的纯状态插件
                    val newSeq = context.containerSeq++
                    val innerSeq = context.innerSeq++

                    // 刷新所有插件的ID，并生成对应的纯状态插件
                    val postParentIdMap = mutableMapOf<String, String>()
                    val statusElements = generateMatrixElements(
                        modelContainer.elements, context.executeCount, postParentIdMap, matrixTaskIds
                    )
                    val replacement = if (asCodeEnabled) {
                        EnvReplacementParser.getCustomExecutionContextByMap(variables)
                    } else null
                    val mutexGroup = modelContainer.mutexGroup?.let { self ->
                        self.copy(
                            mutexGroupName = EnvReplacementParser.parse(
                                value = self.mutexGroupName,
                                contextMap = contextCase,
                                onlyExpression = asCodeEnabled,
                                contextPair = replacement
                            ),
                            linkTip = EnvReplacementParser.parse(self.linkTip, contextCase, asCodeEnabled, replacement)
                        )
                    }
                    val newContainer = NormalContainer(
                        name = EnvReplacementParser.parse(modelContainer.name, contextCase, asCodeEnabled, replacement),
                        id = newSeq.toString(),
                        containerId = newSeq.toString(),
                        containerHashId = modelContainerIdGenerator.getNextId(),
                        jobId = modelContainer.jobId?.let { self -> VMUtils.genMatrixJobId(self, innerSeq) },
                        matrixGroupId = matrixGroupId,
                        matrixContext = contextCase,
                        elements = modelContainer.elements,
                        canRetry = modelContainer.canRetry,
                        jobControlOption = jobControlOption,
                        mutexGroup = mutexGroup,
                        executeCount = context.executeCount,
                        containPostTaskFlag = modelContainer.containPostTaskFlag
                    )
                    newContainer.jobId?.let { matrixJobIds.add(it) }
                    groupContainers.add(
                        pipelineContainerService.prepareMatrixBuildContainer(
                            projectId = event.projectId,
                            pipelineId = event.pipelineId,
                            buildId = event.buildId,
                            container = newContainer,
                            stage = modelStage,
                            context = context,
                            buildTaskList = buildTaskList,
                            recordTaskList = recordTaskList,
                            resourceVersion = recordContainer?.resourceVersion,
                            jobControlOption = jobControlOption,
                            matrixGroupId = matrixGroupId,
                            postParentIdMap = postParentIdMap,
                            mutexGroup = mutexGroup
                        )
                    )
                    recordContainer?.let {
                        val containerVar = mutableMapOf<String, Any>(
                            "@type" to newContainer.getClassType(),
                            newContainer::containerHashId.name to (newContainer.containerHashId ?: ""),
                            newContainer::name.name to (newContainer.name),
                            newContainer::matrixGroupId.name to matrixGroupId,
                            newContainer::matrixContext.name to contextCase
                        )
                        modelContainer.mutexGroup?.let {
                            containerVar[newContainer::mutexGroup.name] = it
                        }
                        recordContainerList.add(
                            BuildRecordContainer(
                                projectId = event.projectId,
                                pipelineId = event.pipelineId,
                                resourceVersion = recordContainer.resourceVersion,
                                buildId = event.buildId,
                                stageId = event.stageId,
                                containerId = newContainer.containerId!!,
                                containerType = recordContainer.containerType,
                                executeCount = context.executeCount,
                                matrixGroupFlag = false,
                                matrixGroupId = matrixGroupId,
                                containerVar = containerVar,
                                status = newContainer.status,
                                startTime = null,
                                endTime = null,
                                timestamps = mapOf()
                            )
                        )
                    }

                    // 如为空就初始化，如有元素就直接追加
                    if (modelContainer.groupContainers.isNullOrEmpty()) {
                        modelContainer.groupContainers = mutableListOf(
                            newContainer.copy(elements = statusElements)
                        )
                    } else {
                        modelContainer.groupContainers!!.add(
                            newContainer.copy(elements = statusElements)
                        )
                    }
                }
            }
            else -> {
                throw InvalidParamException(
                    "matrix(${parentContainer.containerId}) with " +
                        "type(${modelContainer.getClassType()}) is invalid"
                )
            }
        }

        variables[TEMPLATE_ACROSS_INFO_ID]?.let { templateId ->
            LOG.info("ENGINE|INIT_MATRIX_CONTAINER|UPDATE_TEMPLATE_ACROSS|$templateId")
            val info = templateAcrossInfoService.getAcrossInfo(event.projectId, event.pipelineId, templateId)
            info.firstOrNull {
                it.templateType == TemplateAcrossInfoType.JOB &&
                    it.templateInstancesIds.contains(modelContainer.jobId)
            } ?: return@let
            info.forEach {
                when (it.templateType) {
                    TemplateAcrossInfoType.JOB -> it.templateInstancesIds.addAll(matrixJobIds)
                    TemplateAcrossInfoType.STEP -> it.templateInstancesIds.addAll(matrixTaskIds)
                }
            }
            templateAcrossInfoService.batchUpdateAcrossInfo(event.projectId, event.pipelineId, event.buildId, info)
        }

        // 输出结果信息到矩阵的构建日志中
        matrixConfig.printMatrixResult(commandContext, modelContainer.name, contextCaseList, context.executeCount)

        // 新增容器全部添加到Container表中
        buildContainerList.addAll(groupContainers)
        matrixOption.totalCount = groupContainers.size
        matrixOption.maxConcurrency = min(
            matrixOption.maxConcurrency ?: PIPELINE_MATRIX_MAX_CON_RUNNING_SIZE_DEFAULT,
            PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
        )

        LOG.info(
            "ENGINE|${event.buildId}|${event.source}|INIT_MATRIX_CONTAINER" +
                "|${event.stageId}|${modelContainer.id}|containerHashId=" +
                "${modelContainer.containerHashId}|context=$context|" +
                "groupJobSize=${groupContainers.size}|taskSize=${buildTaskList.size}"
        )
        // #6440 进行已有总容器数量判断，如果大于单个stage下的job数量则分裂出错
        val stageContainers = pipelineContainerService.listContainers(
            projectId = parentContainer.projectId,
            buildId = parentContainer.buildId,
            stageId = parentContainer.stageId
        )
        val count = stageContainers.size + groupContainers.size - commandContext.stageMatrixCount
        LOG.info(
            "ENGINE|${event.buildId}|${event.source}|CHECK_CONTAINER_COUNT" +
                "|${event.stageId}|${modelContainer.id}|containerHashId=" +
                "${modelContainer.containerHashId}|currentCount=${stageContainers.size}|" +
                "countResult=$count"
        )
        if (count > PIPELINE_STAGE_CONTAINERS_COUNT_MAX) {
            throw InvalidParamException(
                "The number of containers($count) in stage(${parentContainer.stageId})" +
                    " exceeds the limit[$PIPELINE_STAGE_CONTAINERS_COUNT_MAX]"
            )
        }
        // #7168 在表中增加所有分裂的Job和Task，此时需要避免同时有update操作，因此先获取所有Job的id锁
        val containerLockList = stageContainers.filter { c ->
            c.matrixGroupFlag != true && !c.status.isFinish()
        }.map { c ->
            ContainerIdLock(redisOperation, c.buildId, c.containerId)
        }
        try {
            containerLockList.forEach { it.lock() }
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineContainerService.batchSave(transactionContext, buildContainerList)
                pipelineTaskService.batchSave(transactionContext, buildTaskList)
                recordContainer?.let {
                    containerBuildRecordService.batchSave(
                        transactionContext, recordContainerList, recordTaskList
                    )
                }
            }
        } finally {
            containerLockList.forEach { it.unlock() }
        }

        buildLogPrinter.addLine(
            buildId = parentContainer.buildId,
            message = "[MATRIX] Successfully saved count: ${buildContainerList.size}",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            jobId = parentContainer.containerHashId,
            executeCount = context.executeCount
        )
        buildLogPrinter.addLine(
            buildId = parentContainer.buildId,
            message = "",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            jobId = parentContainer.containerHashId,
            executeCount = context.executeCount
        )
        buildLogPrinter.addLine(
            buildId = parentContainer.buildId,
            message = "[MATRIX] Start to run...",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            jobId = parentContainer.containerHashId,
            executeCount = context.executeCount
        )

        // 在详情中刷新所有分裂后的矩阵
        pipelineContainerService.updateMatrixGroupStatus(
            projectId = parentContainer.projectId,
            pipelineId = parentContainer.pipelineId,
            buildId = parentContainer.buildId,
            stageId = parentContainer.stageId,
            matrixGroupId = matrixGroupId,
            executeCount = parentContainer.executeCount,
            buildStatus = commandContext.buildStatus,
            controlOption = parentContainer.controlOption.copy(matrixControlOption = matrixOption),
            modelContainer = modelContainer
        )

        return buildContainerList.size
    }

    private fun generateMatrixElements(
        elements: List<Element>,
        executeCount: Int,
        postParentIdMap: MutableMap<String, String>,
        matrixTaskIds: MutableList<String>
    ): List<MatrixStatusElement> {
        val originToNewId = mutableMapOf<String, String>()
        return elements.map { e ->
            // 每次写入TASK表都要是新获取的taskId，统一调整为不可重试
            val newTaskId = modelTaskIdGenerator.getNextId()
            // 记录所有新ID对应的原ID，并将post-action信息更新父插件的ID
            originToNewId[e.id!!] = newTaskId
            e.additionalOptions?.elementPostInfo?.parentElementId?.let { originId ->
                originToNewId[originId]
            }?.let { newId ->
                postParentIdMap[newTaskId] = newId
                e.additionalOptions?.elementPostInfo?.parentElementId = newId
            }
            matrixTaskIds.add(newTaskId)

            // 刷新ID为新的唯一值，强制设为无法重试
            e.id = newTaskId
            e.canRetry = false
            val (interceptTask, interceptTaskName, reviewUsers) = when (e) {
                is QualityGateInElement -> {
                    Triple(e.interceptTask, e.interceptTaskName, e.reviewUsers)
                }
                is QualityGateOutElement -> {
                    Triple(e.interceptTask, e.interceptTaskName, e.reviewUsers)
                }
                is ManualReviewUserTaskElement -> {
                    Triple(null, null, e.reviewUsers)
                }
                else -> {
                    Triple(null, null, null)
                }
            }
            MatrixStatusElement(
                name = e.name,
                id = e.id,
                stepId = e.stepId,
                executeCount = executeCount,
                originClassType = e.getClassType(),
                originAtomCode = e.getAtomCode(),
                originTaskAtom = e.getTaskAtom(),
                interceptTask = interceptTask,
                interceptTaskName = interceptTaskName,
                reviewUsers = reviewUsers?.toMutableList()
            )
        }
    }

    private fun MatrixConfig.printMatrixResult(
        commandContext: ContainerContext,
        containerName: String,
        contextCaseList: List<Map<String, String>>,
        executeCount: Int
    ) {
        val buildId = commandContext.container.buildId
        val containerHashId = commandContext.container.containerHashId
        val taskId = VMUtils.genStartVMTaskId(commandContext.container.containerId)

        // 打印参数矩阵
        buildLogPrinter.addLine(
            buildId = buildId, message = "",
            tag = taskId, jobId = containerHashId, executeCount = executeCount
        )
        buildLogPrinter.addFoldStartLine(
            buildId = buildId, groupName = "[MATRIX] Job strategy:",
            tag = taskId, jobId = containerHashId, executeCount = executeCount
        )
        buildLogPrinter.addLine(
            buildId = buildId, message = "",
            tag = taskId, jobId = containerHashId, executeCount = executeCount
        )
        this.strategy?.forEach { (key, valueList) ->
            buildLogPrinter.addLine(
                buildId = buildId, message = "$key: $valueList",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
        }
        buildLogPrinter.addFoldEndLine(
            buildId = buildId, groupName = "",
            tag = taskId, jobId = containerHashId, executeCount = executeCount
        )

        // 打印追加参数
        if (!this.include.isNullOrEmpty()) {
            buildLogPrinter.addFoldStartLine(
                buildId = buildId, groupName = "[MATRIX] Include cases:",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
            buildLogPrinter.addLine(
                buildId = buildId, message = "",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
            printMatrixCases(buildId, taskId, containerHashId, executeCount, this.include)
            buildLogPrinter.addFoldEndLine(
                buildId = buildId, groupName = "",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
        }

        // 打印排除参数
        if (!this.exclude.isNullOrEmpty()) {
            buildLogPrinter.addFoldStartLine(
                buildId = buildId, groupName = "[MATRIX] Exclude cases:",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
            buildLogPrinter.addLine(
                buildId = buildId, message = "",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
            printMatrixCases(buildId, taskId, containerHashId, executeCount, this.exclude)
            buildLogPrinter.addFoldEndLine(
                buildId = buildId, groupName = "",
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
        }

        // 打印最终结果
        buildLogPrinter.addFoldStartLine(
            groupName = "[MATRIX] After calculated, ${contextCaseList.size} jobs are generated:",
            buildId = buildId, tag = taskId, jobId = containerHashId, executeCount = executeCount
        )
        buildLogPrinter.addLine(
            buildId = buildId, message = "",
            tag = taskId, jobId = containerHashId, executeCount = executeCount
        )
        contextCaseList.forEach { contextCase ->
            val nameBuilder = StringBuilder("$containerName(")
            var index = 0
            contextCase.forEach { (key, value) ->
                if (index++ == 0) nameBuilder.append("$key:$value")
                else nameBuilder.append(", $key:$value")
            }
            nameBuilder.append(")")
            buildLogPrinter.addLine(
                buildId = buildId, message = nameBuilder.toString(),
                tag = taskId, jobId = containerHashId, executeCount = executeCount
            )
        }
        buildLogPrinter.addFoldEndLine(
            buildId = buildId, groupName = "",
            tag = taskId, jobId = containerHashId, executeCount = executeCount
        )
    }

    private fun printMatrixCases(
        buildId: String,
        taskId: String,
        containerHashId: String?,
        executeCount: Int,
        cases: List<Map<String, String>>?
    ) {
        cases?.forEach { case ->
            var index = 0
            case.forEach { (key, value) ->
                if (index++ == 0) buildLogPrinter.addLine(
                    buildId = buildId, message = "- $key: $value",
                    tag = taskId, jobId = containerHashId, executeCount = executeCount
                ) else buildLogPrinter.addLine(
                    buildId = buildId, message = "  $key: $value",
                    tag = taskId, jobId = containerHashId, executeCount = executeCount
                )
            }
        }
    }

    private fun checkAndFetchOption(option: MatrixControlOption?): MatrixControlOption {
        if (option == null) throw DependNotFoundException("matrix option not found")
        if ((option.maxConcurrency ?: 0) > PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX) {
            throw InvalidParamException(
                "matrix maxConcurrency(${option.maxConcurrency}) " +
                    "is larger than $PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX"
            )
        }
        return option
    }
}

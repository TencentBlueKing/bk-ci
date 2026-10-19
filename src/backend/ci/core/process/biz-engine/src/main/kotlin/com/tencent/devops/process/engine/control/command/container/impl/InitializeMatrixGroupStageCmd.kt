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

package com.tencent.devops.process.engine.control.command.container.impl

import com.tencent.devops.common.api.constant.TEMPLATE_ACROSS_INFO_ID
import com.tencent.devops.common.api.exception.DependNotFoundException
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil.deepCopy
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.matrix.MatrixConfig
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.option.MatrixControlOption.Companion.MATRIX_CASE_MAX_COUNT
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_MATRIX_MAX_CONCURRENCY_INVALID
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
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.service.PipelineBuildTemplateAcrossInfoService
import com.tencent.devops.process.service.PipelineContextService
import com.tencent.devops.process.utils.MatrixConcurrencyUtils
import com.tencent.devops.process.utils.NODE_OS
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_STAGE_CONTAINERS_COUNT_MAX
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val templateAcrossInfoService: PipelineBuildTemplateAcrossInfoService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContextService: PipelineContextService,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val dispatchTypeParser: DispatchTypeParser,
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation
) : ContainerCmd {

    companion object {
        private val LOG = LoggerFactory.getLogger(InitializeMatrixGroupStageCmd::class.java)
        // 容器记录的变量表中存放Job名的key
        private const val CONTAINER_NAME_KEY = "name"
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
        // 矩阵局部重试：若子容器已存在（局部重试未删除子容器），则跳过重新分裂，
        // 直接进入命令链后续的 MatrixExecuteContainerCmd 轮询下发被重置回排队的子Job。
        // 全量/Stage重试已通过 cleanContainersInMatrixGroup 删空子容器 → 走正常分裂逻辑。
        val existingChildren = pipelineContainerService.listGroupContainers(
            projectId = parentContainer.projectId,
            buildId = parentContainer.buildId,
            matrixGroupId = parentContainer.containerId
        )
        if (existingChildren.isNotEmpty()) {
            LOG.info(
                "ENGINE|${parentContainer.buildId}|MATRIX_CONTAINER_REUSE|" +
                    "matrix(${parentContainer.containerId})|reuseChildCount=${existingChildren.size}"
            )
            // 局部重试不重新分裂，但最大并发数可能由变量配置，仍需解析校验后交给下游调度，
            // 否则会沿用解析前的旧值，变相绕过有效性要求
            try {
                refreshMaxConcurrency(commandContext = commandContext, parentContainer = parentContainer)
            } catch (ignore: Throwable) {
                // 与分裂失败保持完全一致的失败处理：异常若冒泡到MQ监听会被吞掉并重试数次才结束，
                // 既拿不到「最大并发数非法」的文案，也让Job迟迟不失败
                logMatrixFailure(
                    commandContext = commandContext,
                    parentContainer = parentContainer,
                    ignore = ignore,
                    scene = "reuse"
                )
                markMatrixFailed(commandContext = commandContext, parentContainer = parentContainer)
                return
            }
            commandContext.buildStatus = BuildStatus.RUNNING
            commandContext.latestSummary = "Matrix(${parentContainer.containerId}) reuse(${existingChildren.size})"
            return
        }
        val count = try {
            buildLogPrinter.addLine(
                buildId = parentContainer.buildId,
                message = "Start preparing to generate build matrix...",
                tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
                containerHashId = parentContainer.containerHashId,
                executeCount = commandContext.executeCount,
                jobId = null,
                stepId = VMUtils.genStartVMTaskId(parentContainer.containerId)
            )
            commandContext.buildStatus = BuildStatus.RUNNING
            val stageLock = StageMatrixLock(redisOperation, parentContainer.buildId, parentContainer.stageId)
            stageLock.use {
                // #6440 只有一个stage下出现多个时需要进行并发锁
                if (commandContext.stageMatrixCount > 1) it.lock()
                generateMatrixGroup(commandContext, parentContainer)
            }
        } catch (ignore: Throwable) {
            logMatrixFailure(
                commandContext = commandContext,
                parentContainer = parentContainer,
                ignore = ignore,
                scene = "init"
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
            markMatrixFailed(commandContext = commandContext, parentContainer = parentContainer)
        }
    }

    /**
     * 记录矩阵失败的日志与构建日志红行，并让矩阵Job失败退出
     */
    private fun logMatrixFailure(
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer,
        ignore: Throwable,
        scene: String
    ) {
        LOG.error(
            "ENGINE|${parentContainer.buildId}|MATRIX_CONTAINER_${scene.uppercase()}_FAILED|" +
                "matrix(${parentContainer.containerId})|parentContainer=$parentContainer",
            ignore
        )
        buildLogPrinter.addRedLine(
            buildId = parentContainer.buildId,
            message = "Abnormal matrix calculation: ${ignore.message}",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            containerHashId = parentContainer.containerHashId,
            executeCount = commandContext.executeCount,
            jobId = null,
            stepId = VMUtils.genStartVMTaskId(parentContainer.containerId)
        )
    }

    /**
     * 矩阵Job失败退出：进入FINALLY结束本Job，失败原因已在[logMatrixFailure]写入构建日志
     */
    private fun markMatrixFailed(commandContext: ContainerContext, parentContainer: PipelineBuildContainer) {
        commandContext.buildStatus = BuildStatus.FAILED
        commandContext.cmdFlowState = CmdFlowState.FINALLY
        commandContext.latestSummary = "j(${parentContainer.containerId}) matrix failed"
    }

    private fun generateMatrixGroup(
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer
    ): Int {

        val event = commandContext.event
        val variables = commandContext.variables
        val recordContainer = containerBuildRecordService.getRecord(
            transactionContext = null,
            projectId = parentContainer.projectId,
            pipelineId = parentContainer.pipelineId,
            buildId = parentContainer.buildId,
            containerId = parentContainer.containerId,
            executeCount = parentContainer.executeCount
        )
        val modelStage = containerBuildRecordService.getRecordModel(
            projectId = parentContainer.projectId,
            pipelineId = parentContainer.pipelineId,
            version = recordContainer?.resourceVersion ?: 1,
            buildId = parentContainer.buildId,
            executeCount = parentContainer.executeCount
        )?.getStage(parentContainer.stageId) ?: throw DependNotFoundException(
            "stage(${parentContainer.stageId}) cannot be found in model"
        )
        val modelContainer = modelStage.getContainer(
            vmSeqId = parentContainer.seq.toString()
        ) ?: throw DependNotFoundException(
            "container(${parentContainer.containerId}) cannot be found in model"
        )
        val dialect = PipelineDialectUtil.getPipelineDialect(variables[PIPELINE_DIALECT])
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
                    "|context=$context|dialect=${variables[PIPELINE_DIALECT]}"
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

                jobControlOption = (modelContainer.jobControlOption ?: JobControlOption()).copy(
                    dependOnType = null,
                    dependOnId = null,
                    dependOnName = null,
                    dependOnContainerId2JobIds = null
                )
                matrixOption = fetchMatrixOption(
                    option = modelContainer.matrixControlOption,
                    jobName = modelContainer.name,
                    commandContext = commandContext,
                    dialect = dialect
                )
                matrixConfig = matrixOption.convertMatrixConfig(variables)
                contextCaseList = matrixConfig.getAllCombinations()
                variables[NODE_OS]
                    ?.takeIf { it.isNotBlank() && modelContainer.baseOS != null }
                    ?.let { nodeOs ->
                        modelContainer.baseOS = VMBaseOS.valueOf(nodeOs)
                    }
                if (contextCaseList.size > MATRIX_CASE_MAX_COUNT) {
                    throw ExecuteException(
                        "Matrix case(${contextCaseList.size}) exceeds " +
                            "the limit($MATRIX_CASE_MAX_COUNT)"
                    )
                }

                contextCaseList.forEach { contextCase ->
                    // 包括matrix.xxx的所有上下文，矩阵生成的要覆盖原变量
                    val customBuildEnv = mutableMapOf<String, String>()
                    modelContainer.customEnv?.forEach {
                        if (!it.key.isNullOrBlank()) customBuildEnv[it.key!!] = it.value ?: ""
                    }
                    val allContext = customBuildEnv.plus(contextCase)
                    val contextPair = if (dialect.supportUseExpression()) {
                        EnvReplacementParser.getCustomExecutionContextByMap(allContext)
                    } else null
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
                    val buildEnv = parsedInfo?.buildEnv
                    val mutexGroup = modelContainer.mutexGroup?.let { self ->
                        self.copy(
                            mutexGroupName = EnvReplacementParser.parse(
                                value = self.mutexGroupName,
                                contextMap = allContext,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = contextPair
                            ),
                            linkTip = EnvReplacementParser.parse(
                                value = self.linkTip,
                                contextMap = allContext,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = contextPair
                            )
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
                        name = EnvReplacementParser.parse(
                            value = modelContainer.name,
                            contextMap = allContext,
                            onlyExpression = dialect.supportUseExpression(),
                            contextPair = contextPair
                        ),
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
                        customEnv = allContext.map { NameAndValue(it.key, it.value) },
                        baseOS = customBaseOS ?: modelContainer.baseOS,
                        vmNames = modelContainer.vmNames,
                        dockerBuildVersion = modelContainer.dockerBuildVersion,
                        dispatchType = customDispatchType
                            ?: modelContainer.dispatchType?.deepCopy<DispatchType>()?.let { itd ->
                                itd.replaceVariable(allContext) // 只处理${{matrix.xxx}}, 其余在DispatchVMStartupTaskAtom处理
                                itd
                            },
                        buildEnv = buildEnv ?: modelContainer.buildEnv,
                        thirdPartyAgentId = modelContainer.thirdPartyAgentId?.let { self ->
                            EnvReplacementParser.parse(
                                value = self,
                                contextMap = allContext,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = contextPair
                            )
                        },
                        thirdPartyAgentEnvId = modelContainer.thirdPartyAgentEnvId?.let { self ->
                            EnvReplacementParser.parse(
                                value = self,
                                contextMap = allContext,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = contextPair
                            )
                        },
                        thirdPartyWorkspace = modelContainer.thirdPartyWorkspace?.let { self ->
                            EnvReplacementParser.parse(
                                value = self,
                                contextMap = allContext,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = contextPair
                            )
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
                            mutexGroup = mutexGroup,
                            agentReuseMutex = parentContainer.controlOption.agentReuseMutex
                        )
                    )
                    recordContainer?.let {
                        val containerVar = mutableMapOf(
                            "@type" to newContainer.getClassType(),
                            newContainer::containerHashId.name to (newContainer.containerHashId ?: ""),
                            newContainer::name.name to (newContainer.name),
                            newContainer::matrixGroupId.name to matrixGroupId,
                            newContainer::matrixContext.name to contextCase,
                            newContainer::startVMTaskSeq.name to (newContainer.startVMTaskSeq ?: 1)
                        )
                        modelContainer.mutexGroup?.let {
                            containerVar[newContainer::mutexGroup.name] = it
                        }
                        newContainer.jobId?.let {
                            containerVar[newContainer::jobId.name] = it
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
                                containPostTaskFlag = newContainer.containPostTaskFlag,
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

                jobControlOption = (modelContainer.jobControlOption ?: JobControlOption()).copy(
                    dependOnType = null,
                    dependOnId = null,
                    dependOnName = null,
                    dependOnContainerId2JobIds = null
                )
                matrixOption = fetchMatrixOption(
                    option = modelContainer.matrixControlOption,
                    jobName = modelContainer.name,
                    commandContext = commandContext,
                    dialect = dialect
                )
                matrixConfig = matrixOption.convertMatrixConfig(variables)
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
                    val replacement = EnvReplacementParser.getCustomExecutionContextByMap(contextCase)
                    val mutexGroup = modelContainer.mutexGroup?.let { self ->
                        self.copy(
                            mutexGroupName = EnvReplacementParser.parse(
                                value = self.mutexGroupName,
                                contextMap = contextCase,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = replacement
                            ),
                            linkTip = EnvReplacementParser.parse(
                                value = self.linkTip,
                                contextMap = contextCase,
                                onlyExpression = dialect.supportUseExpression(),
                                contextPair = replacement
                            )
                        )
                    }
                    val newContainer = NormalContainer(
                        name = EnvReplacementParser.parse(
                            value = modelContainer.name,
                            contextMap = contextCase,
                            onlyExpression = dialect.supportUseExpression(),
                            contextPair = replacement
                        ),
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
                            mutexGroup = mutexGroup,
                            agentReuseMutex = parentContainer.controlOption.agentReuseMutex
                        )
                    )
                    recordContainer?.let {
                        val containerVar = mutableMapOf(
                            "@type" to newContainer.getClassType(),
                            newContainer::containerHashId.name to (newContainer.containerHashId ?: ""),
                            newContainer::name.name to (newContainer.name),
                            newContainer::matrixGroupId.name to matrixGroupId,
                            newContainer::matrixContext.name to contextCase,
                            newContainer::startVMTaskSeq.name to (newContainer.startVMTaskSeq ?: 1)
                        )
                        modelContainer.mutexGroup?.let {
                            containerVar[newContainer::mutexGroup.name] = it
                        }
                        newContainer.jobId?.let {
                            containerVar[newContainer::jobId.name] = it
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
                                containPostTaskFlag = newContainer.containPostTaskFlag,
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
        // 同步回当前容器快照：同一条命令链内的MatrixExecuteContainerCmd复用commandContext.container，
        // 只落库不回写会让首轮调度读到解析前的旧值，使变量被忽略或突破并发上限
        parentContainer.controlOption.matrixControlOption?.maxConcurrency = matrixOption.maxConcurrency

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
            containerHashId = parentContainer.containerHashId,
            executeCount = context.executeCount,
            jobId = null,
            stepId = VMUtils.genStartVMTaskId(parentContainer.containerId)
        )
        buildLogPrinter.addLine(
            buildId = parentContainer.buildId,
            message = "",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            containerHashId = parentContainer.containerHashId,
            executeCount = context.executeCount,
            jobId = null,
            stepId = VMUtils.genStartVMTaskId(parentContainer.containerId)
        )
        buildLogPrinter.addLine(
            buildId = parentContainer.buildId,
            message = "[MATRIX] Start to run...",
            tag = VMUtils.genStartVMTaskId(parentContainer.containerId),
            containerHashId = parentContainer.containerHashId,
            executeCount = context.executeCount,
            jobId = null,
            stepId = VMUtils.genStartVMTaskId(parentContainer.containerId)
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
            // 每次写入TASK表都要是新获取的taskId（矩阵重新分裂会生成全新taskId）
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

            // 刷新ID为新的唯一值；canRetry/canSkip 交由详情返回时统一按子容器状态计算
            e.id = newTaskId
            MatrixStatusElement(
                name = e.name,
                id = e.id,
                stepId = e.stepId,
                executeCount = executeCount,
                originClassType = e.getClassType(),
                originAtomCode = e.getAtomCode(),
                originTaskAtom = e.getTaskAtom()
            ).apply {
                when (e) {
                    is QualityGateInElement -> {
                        interceptTask = e.interceptTask
                        interceptTaskName = e.interceptTaskName
                        reviewUsers = e.reviewUsers?.toMutableList()
                    }

                    is QualityGateOutElement -> {
                        interceptTask = e.interceptTask
                        interceptTaskName = e.interceptTaskName
                        reviewUsers = e.reviewUsers?.toMutableList()
                    }

                    is ManualReviewUserTaskElement -> {
                        reviewUsers = e.reviewUsers.toMutableList()
                        desc = e.desc
                        params = e.params
                        suggestRequired = e.suggestRequired
                    }

                    else -> {}
                }
            }
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
            tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
        )
        buildLogPrinter.addFoldStartLine(
            buildId = buildId, groupName = "[MATRIX] Job strategy:",
            tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
        )
        buildLogPrinter.addLine(
            buildId = buildId, message = "",
            tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
        )
        this.strategy?.forEach { (key, valueList) ->
            buildLogPrinter.addLine(
                buildId = buildId, message = "$key: $valueList",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
        }
        buildLogPrinter.addFoldEndLine(
            buildId = buildId, groupName = "",
            tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
        )

        // 打印追加参数
        if (!this.include.isNullOrEmpty()) {
            buildLogPrinter.addFoldStartLine(
                buildId = buildId, groupName = "[MATRIX] Include cases:",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
            buildLogPrinter.addLine(
                buildId = buildId, message = "",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
            printMatrixCases(buildId, taskId, containerHashId, executeCount, this.include)
            buildLogPrinter.addFoldEndLine(
                buildId = buildId, groupName = "",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
        }

        // 打印排除参数
        if (!this.exclude.isNullOrEmpty()) {
            buildLogPrinter.addFoldStartLine(
                buildId = buildId, groupName = "[MATRIX] Exclude cases:",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
            buildLogPrinter.addLine(
                buildId = buildId, message = "",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
            printMatrixCases(buildId, taskId, containerHashId, executeCount, this.exclude)
            buildLogPrinter.addFoldEndLine(
                buildId = buildId, groupName = "",
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
        }

        // 打印最终结果
        buildLogPrinter.addFoldStartLine(
            groupName = "[MATRIX] After calculated, ${contextCaseList.size} jobs are generated:",
            buildId = buildId, tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
        )
        buildLogPrinter.addLine(
            buildId = buildId, message = "",
            tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
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
                tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                jobId = null,
                stepId = taskId
            )
        }
        buildLogPrinter.addFoldEndLine(
            buildId = buildId, groupName = "",
            tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
            jobId = null,
            stepId = taskId
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
                    tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                    jobId = null,
                    stepId = taskId
                ) else buildLogPrinter.addLine(
                    buildId = buildId, message = "  $key: $value",
                    tag = taskId, containerHashId = containerHashId, executeCount = executeCount,
                    jobId = null,
                    stepId = taskId
                )
            }
        }
    }

    /**
     * 取出矩阵配置并解析出最大并发数，返回解析后的配置副本（不修改编排快照本身）：
     * 最大并发数支持变量（[MatrixControlOption.maxConcurrencyVar]），启动时先用上下文变量解析，
     * 解析结果必须是1到上限之间的整数，否则矩阵Job直接失败退出，
     * 不做静默截断或回退，避免用变量绕过最大并发数的有效性要求。
     */
    private fun fetchMatrixOption(
        option: MatrixControlOption?,
        jobName: String,
        commandContext: ContainerContext,
        dialect: IPipelineDialect
    ): MatrixControlOption {
        val matrixOption = option ?: throw DependNotFoundException("matrix option not found")
        return matrixOption.copy(
            maxConcurrency = resolveMaxConcurrency(
                option = matrixOption,
                jobName = jobName,
                commandContext = commandContext,
                dialect = dialect
            )
        )
    }

    /**
     * 局部重试（子容器已存在）不会重新分裂，最大并发数若由变量配置同样需要解析并校验，
     * 解析结果写入当前容器快照并落库，保证下游MatrixExecuteContainerCmd读到的是解析后的值。
     */
    private fun refreshMaxConcurrency(
        commandContext: ContainerContext,
        parentContainer: PipelineBuildContainer
    ) {
        val option = parentContainer.controlOption.matrixControlOption ?: return
        val dialect = PipelineDialectUtil.getPipelineDialect(commandContext.variables[PIPELINE_DIALECT])
        val maxConcurrency = resolveMaxConcurrency(
            option = option,
            jobName = fetchJobName(parentContainer),
            commandContext = commandContext,
            dialect = dialect
        )
        val originConcurrency = option.maxConcurrency
        option.maxConcurrency = maxConcurrency
        // 解析结果必须落库：后续轮询事件都会从DB重新加载容器，
        // 只改内存会让下一轮读回解析前的旧值，变量配置等同于不生效。
        // 值未变化时跳过写库，避免每次轮询事件都产生一次无效更新
        if (originConcurrency != maxConcurrency) {
            pipelineContainerService.updateMatrixGroupStatus(
                projectId = parentContainer.projectId,
                pipelineId = parentContainer.pipelineId,
                buildId = parentContainer.buildId,
                stageId = parentContainer.stageId,
                matrixGroupId = parentContainer.containerId,
                executeCount = parentContainer.executeCount,
                buildStatus = commandContext.buildStatus,
                modelContainer = null,
                controlOption = parentContainer.controlOption
            )
        }
    }

    /**
     * 取矩阵Job名用于失败文案：构建容器记录里没有直接的名称字段，
     * 从容器记录的变量表中读取，取不到时退回容器ID
     */
    private fun fetchJobName(parentContainer: PipelineBuildContainer): String {
        val record = containerBuildRecordService.getRecord(
            transactionContext = null,
            projectId = parentContainer.projectId,
            pipelineId = parentContainer.pipelineId,
            buildId = parentContainer.buildId,
            containerId = parentContainer.containerId,
            executeCount = parentContainer.executeCount
        )
        return record?.containerVar?.get(CONTAINER_NAME_KEY)?.toString() ?: parentContainer.containerId
    }

    /**
     * 构造解析最大并发数使用的变量上下文：构建变量 + [PipelineContextService.buildContext]聚合的运行上下文
     * （job 输出变量、job 状态、矩阵聚合输出、ci.* 等）。
     *
     * 只聚合不替换：buildContext 只输出上下文键，原始构建变量始终保留，
     * 与 CheckConditionalSkipContainerCmd 等运行时判断的用法保持一致。
     */
    private fun buildConcurrencyVariables(commandContext: ContainerContext): Map<String, String> {
        val container = commandContext.container
        return commandContext.variables.plus(
            pipelineContextService.buildContext(
                projectId = container.projectId,
                pipelineId = container.pipelineId,
                buildId = container.buildId,
                stageId = container.stageId,
                containerId = container.containerId,
                taskId = null,
                variables = commandContext.variables,
                executeCount = commandContext.executeCount
            )
        )
    }

    /**
     * 解析并校验最大并发数，返回生效值（未配置时取默认值）：
     * 变量表达式先用上下文变量解析，解析结果必须是1到上限之间的整数，
     * 否则矩阵Job直接失败退出，不做静默截断或回退，避免用变量绕过最大并发数的有效性要求。
     *
     * 解析上下文除构建变量外，还合并[PipelineContextService.buildContext]聚合的运行上下文
     * （jobs.<jobId>.steps.<stepId>.outputs.<name>、job状态、矩阵聚合输出、ci.*等），
     * 与Job运行条件等其它运行时表达式判断保持一致，避免同一表达式在条件里能解析、在并发数里解析不了。
     *
     * 注意：输出变量没有裸名短键（短键 steps.<stepId>.outputs.<name> 只对当前Job生效），
     * 跨Job引用必须使用完整表达式如 ${{ jobs.<jobId>.steps.<stepId>.outputs.<name> }}，
     * 裸名（如 ${{my_name}}）在任何上下文中都不存在，只能命中流水线入参/普通构建变量。
     */
    private fun resolveMaxConcurrency(
        option: MatrixControlOption,
        jobName: String,
        commandContext: ContainerContext,
        dialect: IPipelineDialect
    ): Int {
        val concurrencyVar = option.maxConcurrencyVar?.trim()
        if (!concurrencyVar.isNullOrBlank()) {
            // 仅在配置了变量时才构建聚合上下文，避免给纯数字配置增加额外的构建记录读取开销
            val variables = buildConcurrencyVariables(commandContext = commandContext)
            val parsed = EnvReplacementParser.parse(
                value = concurrencyVar,
                contextMap = variables,
                onlyExpression = dialect.supportUseExpression()
            ).trim()
            val concurrency = parsed.toIntOrNull()
            if (concurrency == null || !MatrixConcurrencyUtils.isValid(concurrency)) {
                LOG.warn(
                    "ENGINE|MATRIX_MAX_CONCURRENCY_VAR_INVALID|job=$jobName|var=$concurrencyVar|parsed=$parsed"
                )
                // 解析结果为空时展示原始变量表达式，避免出现「最大并发数为 ，须为...」这种无值文案
                throw InvalidParamException(
                    invalidMessage(
                        jobName = jobName,
                        value = parsed.takeIf { it.isNotBlank() } ?: concurrencyVar,
                        userId = commandContext.event.userId
                    )
                )
            }
            return concurrency
        }
        val maxConcurrency = MatrixConcurrencyUtils.default(option.maxConcurrency)
        if (!MatrixConcurrencyUtils.isValid(maxConcurrency)) {
            LOG.warn("ENGINE|MATRIX_MAX_CONCURRENCY_INVALID|job=$jobName|maxConcurrency=$maxConcurrency")
            throw InvalidParamException(
                invalidMessage(
                    jobName = jobName,
                    value = maxConcurrency.toString(),
                    userId = commandContext.event.userId
                )
            )
        }
        return maxConcurrency
    }

    /** 「运行时」场景的文案：Matrix job「{Job 名}」的最大并发数为 {值}，须为 1-MAX 的整数 */
    private fun invalidMessage(jobName: String, value: String, userId: String): String =
        I18nUtil.getCodeLanMessage(
            messageCode = ERROR_MATRIX_MAX_CONCURRENCY_INVALID,
            params = arrayOf(jobName, value, PIPELINE_MATRIX_CON_RUNNING_SIZE_MAX.toString()),
            language = I18nUtil.getLanguage(userId)
        )
}

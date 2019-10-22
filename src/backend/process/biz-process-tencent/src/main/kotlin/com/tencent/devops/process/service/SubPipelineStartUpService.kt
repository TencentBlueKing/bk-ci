package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.service.SubPipelineCallElement
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.pipeline.ProjectBuildId
import com.tencent.devops.process.pojo.pipeline.StartUpInfo
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SubPipelineStartUpService(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildService: PipelineBuildService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineStartUpService::class.java)
    }

    /**
     * 启动子流水线方法，带入子流水线运行参数
     * @param projectId 流水线所在项目的ID，只能启动同一个项目下的子流水线
     * @param parentPipelineId 启动子流水线的流水线ID
     * @param buildId 本次流水线构建的ID
     * @param callPipelineId 子流水线ID
     * @param taskId 本次构建任务的ID
     * @param runMode 子流水线运行方式
     * @param values 子流水线启动参数
     */
    fun callPipelineStartup(
        projectId: String,
        parentPipelineId: String,
        buildId: String,
        callPipelineId: String,
        atomCode: String,
        taskId: String,
        runMode: String,
        values: Map<String, String>
    ): Result<ProjectBuildId> {
        logger.info("callPipelineStartup: $projectId | $parentPipelineId | $buildId | $callPipelineId | $taskId | $runMode")

        // 获取构建任务
        val task = pipelineRuntimeService.getBuildTask(buildId, taskId)
                ?: return MessageCodeUtil.generateResponseDataObject(ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID.toString(), arrayOf(buildId))

        logger.info("task: $task")

        logger.info("callPipelineStartup: ${task.projectId} | $parentPipelineId | $buildId | $callPipelineId | $taskId | $runMode")

        // 通过 runVariables获取 userId 和 channelCode
        val runVariables = pipelineRuntimeService.getAllVariable(buildId)
        logger.info("runVariables: $runVariables")
        val channelCode = ChannelCode.valueOf(runVariables[PIPELINE_START_CHANNEL]
                ?: return MessageCodeUtil.generateResponseDataObject(ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID.toString(), arrayOf(buildId)))
        // 获取子流水线启动参数
        val startParams = mutableMapOf<String, Any>()
        values.forEach {
            startParams[it.key] = parseVariable(it.value, runVariables)
        }
        val pipelineInfo = (pipelineRepositoryService.getPipelineInfo(task.projectId, callPipelineId)
                ?: return MessageCodeUtil.generateResponseDataObject(ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID.toString(), arrayOf(buildId)))

        logger.info("pipelineInfo: $pipelineInfo")

        val existPipelines = HashSet<String>()
        existPipelines.add(parentPipelineId)
        try {
            checkSubpipeline(atomCode, task.projectId, callPipelineId, existPipelines)
        } catch (e: OperationException) {
            return MessageCodeUtil.generateResponseDataObject(ProcessMessageCode.ERROR_SUBPIPELINE_CYCLE_CALL.toString())
        }

        val subBuildId = buildService.subpipelineStartup(
                userId = runVariables.getValue(PIPELINE_START_USER_ID),
                startType = StartType.PIPELINE,
                projectId = task.projectId,
                parentPipelineId = parentPipelineId,
                parentBuildId = buildId,
                parentTaskId = taskId,
                pipelineId = callPipelineId,
                channelCode = channelCode,
                parameters = startParams,
                checkPermission = false,
                isMobile = false
        )

        return Result(ProjectBuildId(id = subBuildId, projectId = task.projectId))
    }

    /**
     * 解析子流水线启动参数
     * @param value 子流水线启动参数
     * @param runVariables 本地运行时变量
     */
    fun parseVariable(value: String?, runVariables: Map<String, String>): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return EnvUtils.parseEnv(value!!, runVariables)
    }

    /**
     * 检查本次子流水线调用是否有循环调用问题，注意：
     * 在加入新的子流水线调用插件后，为防止新旧插件循环调用的情况，需要检查MarketBuildLessAtomElement和
     * SubPipelineCallElement两种类型的插件是否调用了当前流水线的情况
     * @param projectId 流水线所在项目的ID
     * @param pipelineId 子流水线ID
     * @param existPipelines 保存当前递归次时父流水线的ID
     */
    fun checkSubpipeline(atomCode: String, projectId: String, pipelineId: String, existPipelines: HashSet<String>) {

        if (existPipelines.contains(pipelineId)) {
            throw OperationException("子流水线不允许循环调用")
        }
        existPipelines.add(pipelineId)
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: return
        val existModel = pipelineRepositoryService.getModel(pipelineId, pipeline.version) ?: return

        val currentExistPipelines = HashSet(existPipelines)
        existModel.stages.forEachIndexed stage@{ index, stage ->
            if (index == 0) {
                // Ignore the trigger container
                return@stage
            }
            stage.containers.forEach container@{ container ->
                if (container !is NormalContainer) {
                    // 只在无构建环境中
                    return@container
                }

                container.elements.forEach element@{ element ->
                    // 只能是无构建环境插件
                    if (element !is MarketBuildLessAtomElement && element !is SubPipelineCallElement) {
                        return@element
                    }
                    if (element is MarketBuildLessAtomElement && element.getAtomCode() != atomCode) {
                        return@element
                    }
                    if (element is SubPipelineCallElement && element.subPipelineId.isBlank()) {
                        return@element
                    }

                    if (element is MarketBuildLessAtomElement) {
                        val map = element.data
                        val msg = map["input"] as? Map<*, *> ?: return@element
                        val subPip = msg["subPip"]
                        val exist = HashSet(currentExistPipelines)
                        checkSubpipeline(atomCode, projectId, subPip as String, exist)
                        existPipelines.addAll(exist)
                    } else if (element is SubPipelineCallElement) {
                        val exist = HashSet(currentExistPipelines)
                        checkSubpipeline(atomCode, projectId, element.subPipelineId, exist)
                        existPipelines.addAll(exist)
                    }
                }
            }
        }
    }

    /**
     * 获取流水线的手动启动参数，返回至前端渲染界面。
     * @param userId 流水线启东人的用户ID
     * @param projectId 流水线所在项目ID
     * @param pipelineId 流水线ID
     */
    fun subpipManualStartupInfo(userId: String, projectId: String, pipelineId: String): Result<List<SubPipelineStartUpInfo>> {
        if (pipelineId.isEmpty())
            return Result(ArrayList())
        val result = buildService.buildManualStartupInfo(userId, projectId, pipelineId, ChannelCode.BS)
        val parameter = ArrayList<SubPipelineStartUpInfo>()
        val prop = result.properties

        for (item in prop) {
            if (item.type != BuildFormPropertyType.MULTIPLE) {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                keyList.add(StartUpInfo(item.id, item.defaultValue))
                valueList.add(StartUpInfo(item.id, item.defaultValue))
                val info = SubPipelineStartUpInfo(item.id, true, "input", "list", "",
                        ArrayList(), keyList, false, item.defaultValue, false, "input", "list",
                        "", ArrayList(), valueList, false)
                parameter.add(info)
            } else {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                val defaultValue = item.defaultValue as String
                keyList.add(StartUpInfo(item.id, item.defaultValue))
                for (option in item.options!!) {
                    valueList.add(StartUpInfo(option.key, option.value))
                }
                val info = SubPipelineStartUpInfo(item.id, true, "input", "list", "",
                        ArrayList(), keyList, false, defaultValue.split(","), false, "select", "list",
                        "", ArrayList(), valueList, true)
                parameter.add(info)
            }
        }
        return Result(parameter)
    }
}
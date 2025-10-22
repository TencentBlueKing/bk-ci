package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import com.tencent.devops.process.pojo.pipeline.SubPipelineTaskParam
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.util.regex.Pattern

/**
 * 子流水线节点服务
 */
@Service
class SubPipelineTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val pipelineResDao: PipelineResourceDao,
    @Lazy
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val subPipelineRefService: SubPipelineRefService,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao
) {
    /**
     * 支持的元素
     */
    fun supportElement(element: Element) = element is SubPipelineCallElement ||
            (element is MarketBuildAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE) ||
            (element is MarketBuildLessAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE)

    /**
     * 子流水线插件atomCode
     */
    fun supportAtomCode(atomCode: String) = (atomCode == SUB_PIPELINE_EXEC_ATOM_CODE) ||
            atomCode == SubPipelineCallElement.classType

    @Suppress("UNCHECKED_CAST")
    fun getSubPipelineParam(
        projectId: String,
        element: Element,
        contextMap: Map<String, String>
    ) = when (element) {
        is SubPipelineCallElement -> {
            resolveSubPipelineCall(
                projectId = projectId,
                element = element,
                contextMap = contextMap
            )
        }

        is MarketBuildAtomElement -> {
            resolveSubPipelineExec(
                projectId = projectId,
                inputMap = element.data["input"] as Map<String, Any>,
                contextMap = contextMap
            )
        }

        is MarketBuildLessAtomElement -> {
            resolveSubPipelineExec(
                projectId = projectId,
                inputMap = element.data["input"] as Map<String, Any>,
                contextMap = contextMap
            )
        }

        else -> null
    }

    private fun resolveSubPipelineCall(
        projectId: String,
        element: SubPipelineCallElement,
        contextMap: Map<String, String>
    ): SubPipelineTaskParam? {
        val subPipelineType = element.subPipelineType ?: SubPipelineType.ID
        val subPipelineId = element.subPipelineId
        val subPipelineName = element.subPipelineName
        val (finalProjectId, finalPipelineId, finalPipeName) = getSubPipelineParam(
            projectId = projectId,
            subProjectId = projectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        ) ?: return null
        return SubPipelineTaskParam(
            taskProjectId = projectId,
            taskPipelineType = subPipelineType,
            taskPipelineId = subPipelineId,
            taskPipelineName = subPipelineName,
            projectId = finalProjectId,
            pipelineId = finalPipelineId,
            pipelineName = finalPipeName
        )
    }

    private fun resolveSubPipelineExec(
        projectId: String,
        inputMap: Map<String, Any>,
        contextMap: Map<String, String>
    ): SubPipelineTaskParam? {
        // projectId为空使用当前流水线的projectId
        val subProjectId = inputMap["projectId"]?.let { projectIdStr ->
            if (projectIdStr is String && projectIdStr.isNotBlank()) projectIdStr else null
        } ?: projectId
        val subPipelineTypeStr = inputMap.getOrDefault("subPipelineType", "ID")
        val subPipelineName = inputMap["subPipelineName"]?.toString()
        val subPipelineId = inputMap["subPip"]?.toString()
        val subPipelineType = when (subPipelineTypeStr) {
            "ID" -> SubPipelineType.ID
            "NAME" -> SubPipelineType.NAME
            else -> return null
        }
        // 分支版本
        val branch = inputMap["subBranch"]?.toString().let {
            EnvUtils.parseEnv(it, contextMap)
        }
        val (finalProjectId, finalPipelineId, finalPipeName) = getSubPipelineParam(
            projectId = projectId,
            subProjectId = subProjectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        ) ?: return null
        return SubPipelineTaskParam(
            taskProjectId = subProjectId,
            taskPipelineType = subPipelineType,
            taskPipelineId = subPipelineId,
            taskPipelineName = subPipelineName,
            projectId = finalProjectId,
            pipelineId = finalPipelineId,
            pipelineName = finalPipeName,
            branch = branch
        )
    }

    @SuppressWarnings("LongParameterList")
    private fun getSubPipelineParam(
        projectId: String,
        subProjectId: String,
        subPipelineType: SubPipelineType,
        subPipelineId: String?,
        subPipelineName: String?,
        contextMap: Map<String, String>
    ): Triple<String, String, String>? {
        return when (subPipelineType) {
            SubPipelineType.ID -> {
                if (subPipelineId.isNullOrBlank()) {
                    return null
                }
                val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
                    projectId = subProjectId, pipelineId = subPipelineId
                ) ?: run {
                    logger.info(
                        "sub-pipeline not found|projectId:$projectId|subPipelineType:$subPipelineType|" +
                                "subProjectId:$subProjectId|subPipelineId:$subPipelineId"
                    )
                    return null
                }
                Triple(subProjectId, subPipelineId, pipelineInfo.pipelineName)
            }

            SubPipelineType.NAME -> {
                if (subPipelineName.isNullOrBlank()) {
                    return null
                }
                val finalSubProjectId = EnvUtils.parseEnv(subProjectId, contextMap)
                var finalSubPipelineName = EnvUtils.parseEnv(subPipelineName, contextMap)
                var finalSubPipelineId = pipelineRepositoryService.listPipelineIdByName(
                    projectId = finalSubProjectId,
                    pipelineNames = setOf(finalSubPipelineName),
                    filterDelete = true
                )[finalSubPipelineName]
                // 流水线名称直接使用流水线ID代替
                if (finalSubPipelineId.isNullOrBlank() && PIPELINE_ID_PATTERN.matcher(
                        finalSubPipelineName
                    ).matches()
                ) {
                    finalSubPipelineId = finalSubPipelineName
                    finalSubPipelineName = pipelineRepositoryService.getPipelineInfo(
                        projectId = finalSubProjectId, pipelineId = finalSubPipelineName
                    )?.pipelineName ?: ""
                }
                if (finalSubPipelineId.isNullOrBlank() || finalSubPipelineName.isEmpty()) {
                    logger.info(
                        "sub-pipeline not found|projectId:$projectId|subPipelineType:$subPipelineType|" +
                                "subProjectId:$subProjectId|subPipelineName:$subPipelineName"
                    )
                    return null
                }
                Triple(finalSubProjectId, finalSubPipelineId, finalSubPipelineName)
            }
        }
    }

    /**
     * 获取最新版流水线编排
     */
    fun getModel(projectId: String, pipelineId: String): Model? {
        var model: Model? = null
        val modelString = pipelineResDao.getLatestVersionModelString(dslContext, projectId, pipelineId)
        if (modelString.isNullOrBlank()) {
            logger.warn("model not found: [$projectId|$pipelineId]")
            return null
        }
        try {
            model = objectMapper.readValue(modelString, Model::class.java)
        } catch (ignored: Exception) {
            logger.warn("parse process($pipelineId) model fail", ignored)
        }
        return model
    }

    fun getContextMap(stages: List<Stage>): Map<String, String> {
        val triggerContainer = stages[0].containers[0] as TriggerContainer
        val variables = triggerContainer.params.associate { param ->
            param.id to param.defaultValue.toString()
        }
        return PipelineVarUtil.fillVariableMap(variables)
    }

    fun modelTaskConvertSubPipelineRef(
        model: Model,
        channel: String,
        enableModelTasks: List<PipelineModelTask>,
        validRefList: MutableList<SubPipelineRef>,
        invalidTaskIds: MutableSet<String>
    ) {
        enableModelTasks.forEach {
            val subPipelineTaskParam = getSubPipelineParam(
                projectId = it.projectId,
                element = JsonUtil.mapTo(it.taskParams, Element::class.java),
                contextMap = getContextMap(model.stages)
            )
            if (subPipelineTaskParam == null) {
                // 记录无效数据，插件存在无效参数的情况
                invalidTaskIds.add(it.taskId)
                return@forEach
            }
            validRefList.add(
                SubPipelineRef(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = model.name,
                    channel = channel,
                    element = EmptyElement(id = it.taskId, name = it.taskName),
                    taskPosition = it.taskPosition,
                    subPipelineId = subPipelineTaskParam.pipelineId,
                    subProjectId = subPipelineTaskParam.projectId,
                    subPipelineName = subPipelineTaskParam.pipelineName,
                    taskProjectId = subPipelineTaskParam.taskProjectId,
                    taskPipelineType = subPipelineTaskParam.taskPipelineType,
                    taskPipelineId = subPipelineTaskParam.taskPipelineId,
                    taskPipelineName = subPipelineTaskParam.taskPipelineName
                )
            )
        }
    }

    fun batchDelete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        subPipelineRefService.deleteAll(
            transaction = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun batchAdd(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        model: Model,
        channel: String,
        modelTasks: List<PipelineModelTask>
    ) {
        // 启用状态的插件
        val enableModelTasks = modelTasks.filter { it.taskEnable() }
        // 有效的引用信息
        val validRefList = mutableListOf<SubPipelineRef>()
        // 无效的引用信息
        val invalidTaskIds = mutableSetOf<String>()
        // 转换为引用信息
        modelTaskConvertSubPipelineRef(
            model = model,
            channel = channel,
            enableModelTasks = enableModelTasks,
            validRefList = validRefList,
            invalidTaskIds = invalidTaskIds
        )
        // 清理无效数据
        cleanUpInvalidRefs(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            existsTaskIds = enableModelTasks.map { it.taskId }.toSet(),
            invalidTaskIds = invalidTaskIds
        )
        // 添加并更新引用信息
        subPipelineRefService.batchAdd(
            transaction = dslContext,
            subPipelineRefList = validRefList
        )
    }

    /**
     * 清理无效引用信息
     * 1. 已禁用的插件引用
     * 2. 无效配置的插件引用
     */
    private fun cleanUpInvalidRefs(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        existsTaskIds: Set<String>,
        invalidTaskIds: Set<String>
    ) {
        subPipelineRefService.cleanUpInvalidRefs(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            existsTaskIds = existsTaskIds,
            invalidTaskIds = invalidTaskIds
        )
    }

    /**
     * 校验是否为子流水线插件, 同时校验启用状态
     */
    private fun PipelineModelTask.taskEnable(): Boolean {
        val elementEnable = (this.stageEnable && this.containerEnable && this.additionalOptions?.enable ?: true)
        val supportElement = supportAtomCode(this.atomCode) || this.taskAtom == SubPipelineCallElement.TASK_ATOM
        return elementEnable && supportElement
    }

    fun getBranchVersionResource(
        projectId: String,
        pipelineId: String,
        branchName: String?
    ) = pipelineRepositoryService.getBranchVersionResource(
        projectId = projectId,
        pipelineId = pipelineId,
        branchName = branchName
    )

    companion object {
        val logger = LoggerFactory.getLogger(SubPipelineTaskService::class.java)
        private val PIPELINE_ID_PATTERN = Pattern.compile("(p-)?[a-f\\d]{32}")
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
    }
}
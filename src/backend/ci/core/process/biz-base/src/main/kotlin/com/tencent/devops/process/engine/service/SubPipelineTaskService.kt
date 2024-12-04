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
import com.tencent.devops.process.engine.dao.SubPipelineRefDao
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
    private val subPipelineRefDao: SubPipelineRefDao
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
    fun supportAtomCode(atomCode: String) = (atomCode == SUB_PIPELINE_EXEC_ATOM_CODE)

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
        val subProjectId = inputMap.getOrDefault("projectId", projectId).toString()
        val subPipelineTypeStr = inputMap.getOrDefault("subPipelineType", "ID")
        val subPipelineName = inputMap["subPipelineName"]?.toString()
        val subPipelineId = inputMap["subPip"]?.toString()
        val subPipelineType = when (subPipelineTypeStr) {
            "ID" -> SubPipelineType.ID
            "NAME" -> SubPipelineType.NAME
            else -> return null
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
            pipelineName = finalPipeName
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
        modelTasks: List<PipelineModelTask>
    ): List<SubPipelineRef> {
        val subPipelineRefList = mutableListOf<SubPipelineRef>()
        modelTasks.filter {
            val elementEnable = (it.stageEnable && it.containerEnable && it.additionalOptions?.enable ?: true)
            val supportElement = supportAtomCode(it.atomCode) || it.atomCode == SubPipelineCallElement.TASK_ATOM
            elementEnable && supportElement
        }.forEach {
            val subPipelineTaskParam = getSubPipelineParam(
                projectId = it.projectId,
                element = JsonUtil.mapTo(it.taskParams, Element::class.java),
                contextMap = getContextMap(model.stages)
            ) ?: return@forEach
            subPipelineRefList.add(
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
        return subPipelineRefList
    }

    fun batchDelete(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ) {
        subPipelineRefDao.deleteAll(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun batchAdd(
        dslContext: DSLContext,
        model: Model,
        channel: String,
        modelTasks: List<PipelineModelTask>
    ) {
        subPipelineRefDao.batchAdd(
            dslContext = dslContext,
            subPipelineRefList = modelTaskConvertSubPipelineRef(
                model = model,
                channel = channel,
                modelTasks = modelTasks
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(SubPipelineTaskService::class.java)
        private val PIPELINE_ID_PATTERN = Pattern.compile("(p-)?[a-f\\d]{32}")
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
    }
}
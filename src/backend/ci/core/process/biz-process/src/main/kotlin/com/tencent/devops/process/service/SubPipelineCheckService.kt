package com.tencent.devops.process.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementHolder
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.extend.DefaultModelCheckPlugin
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.SubPipelineIdAndName
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import com.tencent.devops.process.pojo.pipeline.SubPipelineTaskParam
import com.tencent.devops.process.service.pipeline.SubPipelineRefService
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.HashMap
import java.util.regex.Pattern
import javax.ws.rs.core.Response

@Service
class SubPipelineCheckService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val pipelineResDao: PipelineResourceDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val defaultModelCheckPlugin: DefaultModelCheckPlugin,
    private val pipelinePermissionService: PipelinePermissionService,
    private val subPipelineRefService: SubPipelineRefService
) {

    /**
     * 检查当前流水线下所有子流水线插件的权限
     * @param projectId 项目id
     * @param pipelineId 流水线id
     * @param userId 目标用户id
     * @param permission 目标权限
     */
    @SuppressWarnings("NestedBlockDepth")
    fun checkSubPipelinePermission(
        projectId: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission
    ): Set<String> {
        val model = getModel(projectId, pipelineId) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        val stages = model.stages
        val contextMap = getContextMap(stages)
        val elements = mutableListOf<ElementHolder>()
        stages.forEachIndexed { index, stage ->
            if (index == 0) return@forEachIndexed
            stage.containers.forEach { container ->
                container.elements.filter { supportElement(it) }.forEach { element ->
                    val holder = ElementHolder(stage = stage, container = container, element = element)
                    elements.add(holder)
                }
            }
        }
        return batchCheckPermission(
            projectId = projectId,
            elements = elements,
            contextMap = contextMap,
            userId = userId,
            permission = permission
        )
    }

    /**
     * 子流水线去重
     *
     * 去掉同一条流水线配置多条相同子流水线,减少子流水线重复校验
     *
     * @return 返回子流水线与插件的映射
     */
    fun distinctSubPipeline(
        projectId: String,
        elements: List<ElementHolder>,
        contextMap: Map<String, String>
    ): Map<SubPipelineIdAndName, MutableList<ElementHolder>> {
        val subPipelineElementMap = mutableMapOf<SubPipelineIdAndName, MutableList<ElementHolder>>()
        elements.forEach { holder ->
            // 禁用的插件不校验
            if (!holder.enableElement()) return@forEach
            val subPipelineTaskParam = getSubPipelineParam(
                element = holder.element,
                projectId = projectId,
                contextMap = contextMap
            ) ?: return@forEach
            val subPipeline = SubPipelineIdAndName(
                projectId = subPipelineTaskParam.projectId,
                pipelineId = subPipelineTaskParam.pipelineId,
                pipelineName = subPipelineTaskParam.pipelineName
            )
            subPipelineElementMap.getOrPut(subPipeline) { mutableListOf() }.add(holder)
        }
        return subPipelineElementMap
    }

    private fun ElementHolder.enableElement() =
        stage.stageEnabled() && container.containerEnabled() && element.elementEnabled()

    fun batchCheckPermission(
        projectId: String,
        elements: List<ElementHolder>,
        contextMap: Map<String, String>,
        userId: String,
        permission: AuthPermission
    ): Set<String> {
        val subPipelineElementMap = distinctSubPipeline(projectId = projectId, elements = elements, contextMap)
        return batchCheckPermission(
            subPipelineElementMap = subPipelineElementMap,
            userId = userId,
            permission = permission
        )
    }

    /**
     * 批量校验子流水线权限
     *
     * @return 权限检查错误提示
     */
    fun batchCheckPermission(
        userId: String,
        permission: AuthPermission,
        subPipelineElementMap: Map<SubPipelineIdAndName, MutableList<ElementHolder>>
    ): Set<String> {
        val errorDetails = mutableSetOf<String>()
        subPipelineElementMap.forEach { (subPipeline, elements) ->
            val subProjectId = subPipeline.projectId
            val subPipelineId = subPipeline.pipelineId
            val subPipelineName = subPipeline.pipelineName
            // 校验流水线修改人是否有子流水线执行权限
            val checkPermission = pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = subPipeline.projectId,
                pipelineId = subPipeline.pipelineId,
                permission = permission
            )
            val pipelinePermissionUrl = "/console/pipeline/$subProjectId/$subPipelineId/history"
            if (!checkPermission) {
                elements.forEach { elementHolder ->
                    errorDetails.add(
                        I18nUtil.getCodeLanMessage(
                            messageCode = ProcessMessageCode.BK_NOT_SUB_PIPELINE_EXECUTE_PERMISSION_ERROR_MESSAGE,
                            params = arrayOf(
                                elementHolder.stage.name!!,
                                elementHolder.container.name,
                                elementHolder.element.name,
                                pipelinePermissionUrl,
                                subPipelineName
                            )
                        )
                    )
                }
            }
        }
        return errorDetails
    }

    /**
     * 批量校验子流水线是否循环依赖
     *
     * @return 循环依赖提示信息
     */
    fun batchCheckCycle(
        projectId: String,
        pipelineId: String,
        subPipelineElementMap: Map<SubPipelineIdAndName, MutableList<ElementHolder>>
    ): Set<String> {
        val errorDetails = mutableSetOf<String>()
        val rootPipelineKey = "${projectId}_$pipelineId"
        subPipelineElementMap.forEach { (subPipeline, elements) ->
            val subProjectId = subPipeline.projectId
            val subPipelineId = subPipeline.pipelineId
            val subPipelineRef = SubPipelineRef(
                projectId = projectId,
                pipelineId = pipelineId,
                subPipelineId = subPipelineId,
                subProjectId = subProjectId
            )
            val checkResult = subPipelineRefService.checkCircularDependency(
                subPipelineRef = subPipelineRef,
                rootPipelineKey = rootPipelineKey,
                existsPipeline = HashMap(mapOf(rootPipelineKey to subPipelineRef))
            )
            if (!checkResult.result) {
                errorDetails.add(checkResult.errorMessage ?: "")
            }
        }
        return errorDetails
    }

    fun supportElement(element: Element) = element is SubPipelineCallElement ||
            (element is MarketBuildAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE) ||
            (element is MarketBuildLessAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE)

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

    private fun getContextMap(stages: List<Stage>): Map<String, String> {
        val trigger = stages.getOrNull(0)
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB)
        // 检查触发容器
        val paramsMap = defaultModelCheckPlugin.checkTriggerContainer(trigger)
        return PipelineVarUtil.fillVariableMap(paramsMap.mapValues { it.value.defaultValue.toString() })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineCheckService::class.java)
        private val PIPELINE_ID_PATTERN = Pattern.compile("(p-)?[a-f\\d]{32}")
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
    }
}

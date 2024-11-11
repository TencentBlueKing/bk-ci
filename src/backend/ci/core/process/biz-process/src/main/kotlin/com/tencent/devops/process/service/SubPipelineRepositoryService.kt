package com.tencent.devops.process.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import jakarta.ws.rs.core.Response

@Service
class SubPipelineRepositoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val pipelineResDao: PipelineResourceDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelinePermissionService: PipelinePermissionService
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
    ): List<ElementCheckResult> {
        val model = getModel(projectId, pipelineId) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        val checkResults = mutableListOf<ElementCheckResult>()
        val stages = model.stages
        val contextMap = getContextMap(stages)
        stages.subList(1, stages.size).forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    if (supportElement(element)) {
                        checkElementPermission(
                            projectId = projectId,
                            stageName = stage.name ?: "",
                            containerName = container.name,
                            element = element,
                            contextMap = contextMap,
                            userId = userId,
                            permission = permission
                        )?.let {
                            checkResults.add(it)
                        }
                    }
                }
            }
        }
        return checkResults
    }

    /**
     * 检查用户是否有插件的目标流水线的指定权限
     * @param userId 目标用户
     * @param permission 目标权限
     */
    @SuppressWarnings("LongParameterList", "LongMethod")
    fun checkElementPermission(
        projectId: String,
        stageName: String,
        containerName: String,
        element: Element,
        contextMap: Map<String, String>,
        userId: String,
        permission: AuthPermission
    ): ElementCheckResult? {
        val subPipelineInfo = getSubPipelineInfo(
            projectId = projectId,
            element = element,
            contextMap = contextMap
        )
        if (subPipelineInfo == null) {
            return null
        }
        val (subProjectId, subPipelineId, subPipelineName) = subPipelineInfo
        logger.info(
            "check the sub-pipeline permissions[${permission.name}]|" +
                    "project:$projectId|elementId:${element.id}|userId:$userId|" +
                    "subProjectId:$subProjectId|subPipelineId:$subPipelineId"
        )
        // 校验流水线修改人是否有子流水线执行权限
        val checkPermission = pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = subProjectId,
            pipelineId = subPipelineId,
            permission = permission
        )
        val pipelinePermissionUrl = "/console/pipeline/$subProjectId/$subPipelineId/history"
        return if (checkPermission) {
            null
        } else {
            ElementCheckResult(
                result = false,
                errorTitle = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_NOT_SUB_PIPELINE_EXECUTE_PERMISSION_ERROR_TITLE,
                    params = arrayOf(userId)
                ),
                errorMessage = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_NOT_SUB_PIPELINE_EXECUTE_PERMISSION_ERROR_MESSAGE,
                    params = arrayOf(
                        stageName,
                        containerName,
                        element.name,
                        pipelinePermissionUrl,
                        subPipelineName
                    )
                )
            )
        }
    }

    fun supportElement(element: Element) = element is SubPipelineCallElement ||
            (element is MarketBuildAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE) ||
            (element is MarketBuildLessAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE)

    fun getSubPipelineInfo(
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
    ): Triple<String, String, String>? {
        val subPipelineType = element.subPipelineType ?: SubPipelineType.ID
        val subPipelineId = element.subPipelineId
        val subPipelineName = element.subPipelineName
        return getSubPipelineInfo(
            projectId = projectId,
            subProjectId = projectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        )
    }

    private fun resolveSubPipelineExec(
        projectId: String,
        inputMap: Map<String, Any>,
        contextMap: Map<String, String>
    ): Triple<String, String, String>? {
        val subProjectId = inputMap.getOrDefault("projectId", projectId).toString()
        val subPipelineTypeStr = inputMap.getOrDefault("subPipelineType", "ID")
        val subPipelineName = inputMap["subPipelineName"]?.toString()
        val subPipelineId = inputMap["subPip"]?.toString()
        val subPipelineType = when (subPipelineTypeStr) {
            "ID" -> SubPipelineType.ID
            "NAME" -> SubPipelineType.NAME
            else -> return null
        }
        return getSubPipelineInfo(
            projectId = projectId,
            subProjectId = subProjectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        )
    }

    @SuppressWarnings("LongParameterList")
    private fun getSubPipelineInfo(
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
    private fun getModel(projectId: String, pipelineId: String): Model? {
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
        val triggerContainer = stages[0].containers[0] as TriggerContainer
        val variables = triggerContainer.params.associate { param ->
            param.id to param.defaultValue.toString()
        }
        return PipelineVarUtil.fillVariableMap(variables)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineRepositoryService::class.java)
        private val PIPELINE_ID_PATTERN = Pattern.compile("(p-)?[a-f\\d]{32}")
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
    }
}

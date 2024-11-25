package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementHolder
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.SubPipelineTaskService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.SubPipelineIdAndName
import com.tencent.devops.process.pojo.pipeline.SubPipelineRef
import com.tencent.devops.process.service.pipeline.SubPipelineRefService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.HashMap
import javax.ws.rs.core.Response

/**
 * 子流水线合法性检查服务
 */
@Service
@SuppressWarnings("TooManyFunctions", "LongParameterList")
class SubPipelineCheckService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val subPipelineRefService: SubPipelineRefService,
    private val SubPipelineTaskService: SubPipelineTaskService
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
        val model = SubPipelineTaskService.getModel(projectId, pipelineId) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        val stages = model.stages
        val contextMap = SubPipelineTaskService.getContextMap(stages)
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
            val subPipelineTaskParam = SubPipelineTaskService.getSubPipelineParam(
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
            val checkResult = checkCircularDependency(
                subPipelineRef = subPipelineRef,
                rootPipelineKey = rootPipelineKey,
                existsPipeline = HashMap(mapOf(rootPipelineKey to subPipelineRef)),
                recursiveChain = mutableListOf(subPipelineRef)
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



    /**
     * 检查循环依赖
     */
    @SuppressWarnings("LongMethod")
    fun checkCircularDependency(
        subPipelineRef: SubPipelineRef,
        rootPipelineKey: String,
        recursiveChain: MutableList<SubPipelineRef>,
        existsPipeline: HashMap<String, SubPipelineRef>
    ): ElementCheckResult {
        with(subPipelineRef) {
            logger.info("check circular dependency|subPipelineRef[$this]|existsPipeline[$existsPipeline]")
            val pipelineRefKey = subRefKey()
            if (existsPipeline.contains(pipelineRefKey)) {
                val chainStr = recursiveChain.joinToString(separator = "->") { "[${it.chainKey()}]" }
                logger.warn(
                    "subPipeline does not allow loop calls|projectId:$subProjectId|" +
                            "pipelineId:$subPipelineId|chain[$chainStr]"
                )
                return getCircularDependencyResult(
                    parentPipelineRef = existsPipeline[pipelineRefKey]!!,
                    subPipelineRef = subPipelineRef,
                    rootPipelineKey = rootPipelineKey,
                    pipelineRefKey = pipelineRefKey
                )
            }
            val subRefList = subPipelineRefService.list(
                projectId = subProjectId,
                pipelineId = subPipelineId
            ).map {
                SubPipelineRef(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    channel = it.channel,
                    element = EmptyElement(
                        id = it.taskId,
                        name = it.taskName
                    ),
                    stageId = it.stageId,
                    containerId = it.containerId,
                    subPipelineId = it.subPipelineId,
                    subProjectId = it.subProjectId,
                    subPipelineName = it.subPipelineName ?: "",
                    taskProjectId = it.taskProjectId,
                    taskPipelineType = SubPipelineType.valueOf(it.taskPipelineType),
                    taskPipelineId = it.taskPipelineId,
                    taskPipelineName = it.taskPipelineName,
                    taskSeq = it.taskSeq
                )
            }
            logger.info("check circular dependency|subRefList[$subRefList]")
            if (subRefList.isEmpty()) return ElementCheckResult(true)
            subRefList.forEach {
                val exist = HashMap(existsPipeline)
                val chain = mutableListOf<SubPipelineRef>()
                // 保存链路
                chain.addAll(recursiveChain)
                chain.add(it)
                exist[it.refKey()] = it
                logger.info(
                    "callPipelineStartup|" +
                            "supProjectId:${it.subProjectId},subPipelineId:${it.subPipelineId}," +
                            "subElementId:${it.element.id},parentProjectId:${it.projectId}," +
                            "parentPipelineId:${it.pipelineId}"
                )
                val checkResult = checkCircularDependency(
                    subPipelineRef = it,
                    rootPipelineKey = rootPipelineKey,
                    recursiveChain = chain,
                    existsPipeline = exist
                )
                // 检查不成功，直接返回
                if (!checkResult.result) {
                    return checkResult
                }
                existsPipeline.putAll(exist)
            }
            return ElementCheckResult(true)
        }
    }

    private fun pipelineEditUrl(projectId: String, pipelineId: String) =
        "/console/pipeline/$projectId/$pipelineId/edit"

    private fun getCircularDependencyResult(
        rootPipelineKey: String,
        pipelineRefKey: String,
        parentPipelineRef: SubPipelineRef,
        subPipelineRef: SubPipelineRef
    ):ElementCheckResult {
        val (msgCode, params) = with(subPipelineRef){
            when {
                // [当前流水线] -> [当前流水线]
                "${projectId}_$pipelineId" == rootPipelineKey -> {
                    ProcessMessageCode.BK_CURRENT_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE to
                            emptyArray<String>()
                }
                // [其他流水线] -> [当前流水线]
                pipelineRefKey == rootPipelineKey -> {
                    val editUrl = pipelineEditUrl(projectId, pipelineId)
                    ProcessMessageCode.BK_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE to arrayOf(
                        editUrl,
                        "${subPipelineRef.pipelineName} [${subPipelineRef.relSeq()}]"
                    )
                }
                // [其他流水线_1] -> [其他流水线_2]
                // [其他流水线_2] -> ... ->[其他流水线_1]
                else -> {
                    val editUrlBase = pipelineEditUrl(parentPipelineRef.projectId, parentPipelineRef.pipelineId)
                    val editUrl = pipelineEditUrl(projectId, pipelineId)
                    ProcessMessageCode.BK_OTHER_SUB_PIPELINE_CIRCULAR_DEPENDENCY_ERROR_MESSAGE to arrayOf(
                        editUrl,
                        "${subPipelineRef.pipelineName} [${subPipelineRef.relSeq()}]",
                        editUrlBase,
                        parentPipelineRef.pipelineName.ifBlank { subPipelineRef.pipelineName }
                    )
                }
            }
        }

        return ElementCheckResult(
            result = false,
            errorMessage = I18nUtil.getCodeLanMessage(
                messageCode = msgCode,
                params = params
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SubPipelineCheckService::class.java)
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
    }
}

package com.tencent.devops.process.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.ModelHandleService
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.VarReferenceInfo
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.VarRefDetail
import com.tencent.devops.process.dao.VarRefDetailDao
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.service.`var`.PublicVarService
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ModelHandleServiceImpl @Autowired constructor(
    private val publicVarService: PublicVarService,
    private val varRefDetailDao: VarRefDetailDao,
    private val dslContext: DSLContext,
    private val publicVarDao: PublicVarDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val client: Client
) : ModelHandleService {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelHandleServiceImpl::class.java)
        // 变量引用正则表达式：匹配 ${ variables.<var_name> } 和 ${{ variables.<var_name> }}
        private val VAR_PATTERN_1 = Regex("""\$\{\s*variables\.(\w+)\s*\}""")
        private val VAR_PATTERN_2 = Regex("""\$\{\{\s*variables\.(\w+)\s*\}\}""")
        // 自定义条件表达式中的变量引用：variables.{varName}==
        private val CUSTOM_CONDITION_PATTERN = Regex("""variables\.(\w+)==""")
        // 线程池大小
        private const val THREAD_POOL_SIZE = 10
    }

    // 创建线程池用于并行处理
    private val executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE)

    override fun handleModelParams(
        projectId: String,
        model: Model,
        referId: String,
        referType: String,
        referVersion: Int
    ) {
        publicVarService.handleModelParams(
            projectId = projectId,
            model = model,
            referId = referId,
            referType = PublicVerGroupReferenceTypeEnum.valueOf(referType),
            referVersion = referVersion
        )
    }

    override fun handleModelVarReferences(
        userId: String,
        projectId: String,
        model: Model,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int
    ) {
        val startTime = System.currentTimeMillis()
        val params = model.getTriggerContainer().params
        val publicVarList = if (!model.publicVarGroups.isNullOrEmpty()) {
            params.filter{!it.varGroupName.isNullOrBlank()}
        } else {
            null
        }

        val varNames = params.map { it.id }.toSet()

        logger.info("Start detecting variable references for resource: $resourceId, total stages: ${model.stages.size}")

        // 并行处理所有 Stage
        val futures = model.stages.mapIndexed { stageIndex, stage ->
            CompletableFuture.supplyAsync({
                handleStageVarReferences(stage, stageIndex, varNames)
            }, executorService)
        }

        // 等待所有任务完成并收集结果
        try {
            CompletableFuture.allOf(*futures.toTypedArray()).get()
            val allReferences = futures.flatMap { it.get() }

            val elapsedTime = System.currentTimeMillis() - startTime
            logger.info(
                "Detected ${allReferences.size} variable references in model " +
                "(resourceId=$resourceId, elapsed=${elapsedTime}ms)"
            )

            // 转换为 VarRefDetail
            val varRefDetails = if (allReferences.isNotEmpty()) {
                convertToVarRefDetails(
                    projectId = projectId,
                    resourceId = resourceId,
                    resourceType = resourceType,
                    resourceVersion = resourceVersion,
                    userId = userId,
                    references = allReferences
                )
            } else {
                emptyList()
            }

            // 业务逻辑：对比变量名列表，删除不存在的，新增或更新存在的
            val newVarNames = varRefDetails.map { it.varName }.toSet()

            if (newVarNames.isEmpty()) {
                // 如果新集合为空，删除该资源的所有变量引用
                varRefDetailDao.deleteByResourceId(
                    dslContext = dslContext,
                    projectId = projectId,
                    resourceId = resourceId,
                    resourceType = resourceType,
                    referVersion = resourceVersion
                )
            } else {
                // 删除新集合中不存在的变量引用
                varRefDetailDao.deleteByVarNamesNotIn(
                    dslContext = dslContext,
                    projectId = projectId,
                    resourceId = resourceId,
                    resourceType = resourceType,
                    referVersion = resourceVersion,
                    varNames = newVarNames
                )

                // 新增或更新变量引用
                varRefDetailDao.batchSave(dslContext, varRefDetails)
            }

            // 更新公共变量引用计数
            publicVarService.updatePublicVarReferCount(
                userId = userId,
                projectId = projectId,
                resourceId = resourceId,
                resourceType = resourceType,
                resourceVersion = resourceVersion,
                publicVarList = publicVarList,
                allReferences = allReferences
            )

        } catch (e: Throwable) {
            logger.warn("Error while detecting variable references for resource: $resourceId", e)
            throw e
        }
    }

    /**
     * 将 VarReferenceInfo 列表转换为 VarRefDetail 列表
     */
    private fun convertToVarRefDetails(
        projectId: String,
        resourceId: String,
        resourceType: String,
        resourceVersion: Int,
        userId: String,
        references: List<VarReferenceInfo>
    ): List<VarRefDetail> {
        val now = LocalDateTime.now()
        return references.map { ref ->
            VarRefDetail(
                projectId = projectId,
                varName = ref.varName,
                resourceId = resourceId,
                resourceType = resourceType,
                resourceVersionName = resourceVersion.toString(),
                referVersion = resourceVersion,
                stageId = ref.stageId,
                containerId = ref.containerId,
                taskId = ref.taskId,
                positionPath = ref.positionPath,
                creator = userId,
                modifier = userId,
                createTime = now,
                updateTime = now
            )
        }
    }

    /**
     * 处理 Stage 层级的变量引用
     */
    private fun handleStageVarReferences(stage: Stage, stageIndex: Int, varNames: Set<String>): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()
        val stageId = stage.id ?: VMUtils.genStageId(stageIndex + 1)
        val basePath = "stages[$stageIndex]"

        try {
            // 检测 customBuildEnv
            stage.customBuildEnv?.forEach { (key, value) ->
                references.addAll(detectVarInString(
                    value = value,
                    stageId = stageId,
                    positionPath = "$basePath.customBuildEnv[$key]",
                    varNames = varNames
                ))
            }

            // 检测 stageControlOption
            stage.stageControlOption?.let { controlOption ->
                // 检测 customVariables
                references.addAll(detectCustomVariables(
                    customVariables = controlOption.customVariables,
                    stageId = stageId,
                    basePath = "$basePath.stageControlOption.customVariables",
                    varNames = varNames
                ))

                // 检测 customCondition
                controlOption.customCondition?.let { condition ->
                    references.addAll(detectVarInCustomCondition(
                        condition = condition,
                        stageId = stageId,
                        positionPath = "$basePath.stageControlOption.customCondition",
                        varNames = varNames
                    ))
                }
            }

            // 检测 checkIn 和 checkOut
            listOf(
                stage.checkIn to "checkIn",
                stage.checkOut to "checkOut"
            ).forEach { (pauseCheck, fieldName) ->
                pauseCheck?.let {
                    references.addAll(detectStagePauseCheck(
                        stagePauseCheck = it,
                        stageId = stageId,
                        positionPath = "$basePath.$fieldName",
                        varNames = varNames
                    ))
                }
            }

            // 遍历 Containers
            stage.containers.forEachIndexed { containerIndex, container ->
                references.addAll(handleContainerVarReferences(
                    container = container,
                    stageId = stageId,
                    stageIndex = stageIndex,
                    containerIndex = containerIndex,
                    varNames = varNames
                ))
            }
        } catch (e: Throwable) {
            logger.warn("Error while handling stage var references: stageId=$stageId, stageIndex=$stageIndex", e)
        }

        return references
    }

    /**
     * 处理 Container 层级的变量引用
     */
    private fun handleContainerVarReferences(
        container: Container,
        stageId: String,
        stageIndex: Int,
        containerIndex: Int,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()
        val containerId = container.containerId ?: container.id ?: "container-${containerIndex + 1}"

        try {
            // 检测 Container 固定参数
            references.addAll(detectVarInString(
                value = container.name,
                stageId = stageId,
                containerId = containerId,
                positionPath = "stages[$stageIndex].containers[$containerIndex].name",
                varNames = varNames
            ))

            // 根据 Container 类型调用不同的检测方法
            when (container) {
                is NormalContainer -> {
                    references.addAll(detectNormalContainerVarReferences(
                        container = container,
                        stageId = stageId,
                        containerId = containerId,
                        stageIndex = stageIndex,
                        containerIndex = containerIndex,
                        varNames = varNames
                    ))
                }
                is VMBuildContainer -> {
                    references.addAll(detectVMBuildContainerVarReferences(
                        container = container,
                        stageId = stageId,
                        containerId = containerId,
                        stageIndex = stageIndex,
                        containerIndex = containerIndex,
                        varNames = varNames
                    ))
                }
            }

            // 遍历 Elements
            container.elements.forEachIndexed { elementIndex, element ->
                references.addAll(handleElementVarReferences(
                    element = element,
                    stageId = stageId,
                    containerId = containerId,
                    stageIndex = stageIndex,
                    containerIndex = containerIndex,
                    elementIndex = elementIndex,
                    varNames = varNames
                ))
            }
        } catch (e: Throwable) {
            logger.warn("Error while handling container var references: containerId=$containerId", e)
        }

        return references
    }

    /**
     * 检测 NormalContainer 特有的变量引用
     */
    private fun detectNormalContainerVarReferences(
        container: NormalContainer,
        stageId: String,
        containerId: String,
        stageIndex: Int,
        containerIndex: Int,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()
        val basePath = "stages[$stageIndex].containers[$containerIndex]"

        try {
            // 检测 jobControlOption
            container.jobControlOption?.let { jobControlOption ->
                // 检测 customVariables
                references.addAll(detectCustomVariables(
                    customVariables = jobControlOption.customVariables,
                    stageId = stageId,
                    containerId = containerId,
                    basePath = "$basePath.jobControlOption.customVariables",
                    varNames = varNames
                ))

                // 检测 customCondition
                jobControlOption.customCondition?.let { condition ->
                    references.addAll(detectVarInCustomCondition(
                        condition = condition,
                        stageId = stageId,
                        positionPath = "$basePath.jobControlOption.customCondition",
                        varNames = varNames
                    ))
                }
            }
        } catch (e: Throwable) {
            logger.warn("Error while detecting NormalContainer var references: containerId=$containerId", e)
        }

        return references
    }

    /**
     * 检测 VMBuildContainer 特有的变量引用
     */
    private fun detectVMBuildContainerVarReferences(
        container: VMBuildContainer,
        stageId: String,
        containerId: String,
        stageIndex: Int,
        containerIndex: Int,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()
        val basePath = "stages[$stageIndex].containers[$containerIndex]"

        try {
            // 检测环境变量 Map
            listOf(
                container.buildEnv to "buildEnv",
                container.customBuildEnv to "customBuildEnv"
            ).forEach { (envMap, fieldName) ->
                envMap?.forEach { (key, value) ->
                    references.addAll(detectVarInString(
                        value = value,
                        stageId = stageId,
                        containerId = containerId,
                        positionPath = "$basePath.$fieldName[$key]",
                        varNames = varNames
                    ))
                }
            }

            // 检测 customEnv
            references.addAll(detectCustomVariables(
                customVariables = container.customEnv,
                stageId = stageId,
                containerId = containerId,
                basePath = "$basePath.customEnv",
                varNames = varNames
            ))

            // 检测 jobControlOption
            container.jobControlOption?.let { jobControlOption ->
                // 检测 customVariables
                references.addAll(detectCustomVariables(
                    customVariables = jobControlOption.customVariables,
                    stageId = stageId,
                    containerId = containerId,
                    basePath = "$basePath.jobControlOption.customVariables",
                    varNames = varNames
                ))

                // 检测 customCondition
                jobControlOption.customCondition?.let { condition ->
                    references.addAll(detectVarInCustomCondition(
                        condition = condition,
                        stageId = stageId,
                        positionPath = "$basePath.jobControlOption.customCondition",
                        varNames = varNames
                    ))
                }
            }

            // 检测第三方构建机相关字段
            listOf(
                container.thirdPartyAgentId to "thirdPartyAgentId",
                container.thirdPartyAgentEnvId to "thirdPartyAgentEnvId",
                container.thirdPartyWorkspace to "thirdPartyWorkspace"
            ).forEach { (value, fieldName) ->
                value?.let {
                    references.addAll(detectVarInString(
                        value = it,
                        stageId = stageId,
                        containerId = containerId,
                        positionPath = "$basePath.$fieldName",
                        varNames = varNames
                    ))
                }
            }
        } catch (e: Throwable) {
            logger.warn("Error while detecting VMBuildContainer var references: containerId=$containerId", e)
        }

        return references
    }

    /**
     * 处理 Element 层级的变量引用
     */
    private fun handleElementVarReferences(
        element: Element,
        stageId: String,
        containerId: String,
        stageIndex: Int,
        containerIndex: Int,
        elementIndex: Int,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()
        val taskId = element.id ?: "element-${elementIndex + 1}"
        val basePath = "stages[$stageIndex].containers[$containerIndex].elements[$elementIndex]"

        try {
            element.additionalOptions?.let { additionalOptions ->
                // 检测 customVariables
                references.addAll(detectCustomVariables(
                    customVariables = additionalOptions.customVariables,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = taskId,
                    basePath = "$basePath.additionalOptions.customVariables",
                    varNames = varNames
                ))

                // 检测 customCondition
                additionalOptions.customCondition?.takeIf { it.isNotBlank() }?.let { condition ->
                    references.addAll(detectVarInCustomCondition(
                        condition = condition,
                        stageId = stageId,
                        positionPath = "$basePath.additionalOptions.customCondition",
                        varNames = varNames
                    ))
                }

                // 检测 elementPostInfo.postEntryParam
                additionalOptions.elementPostInfo?.postEntryParam?.takeIf { it.isNotBlank() }?.let { postEntryParam ->
                    references.addAll(detectVarInString(
                        value = postEntryParam,
                        stageId = stageId,
                        containerId = containerId,
                        taskId = taskId,
                        positionPath = "$basePath.additionalOptions.elementPostInfo.postEntryParam",
                        varNames = varNames
                    ))
                }
            }

            // 处理 element 输入参数
            val inputParams = element.getInputParamMap()
            inputParams.forEach { (key, value) ->
                references.addAll(detectVarInParams(
                    value = value,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = taskId,
                    positionPath = "$basePath.$key",
                    varNames = varNames
                ))
            }
        } catch (e: Throwable) {
            logger.warn("Error while handling element var references: taskId=$taskId", e)
        }

        return references
    }

    /**
     * 检测字符串中的变量引用
     */
    private fun detectVarInString(
        value: String,
        stageId: String,
        containerId: String? = null,
        taskId: String? = null,
        positionPath: String,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()

        // 匹配 ${varName}
        VAR_PATTERN_1.findAll(value).forEach { matchResult ->
            val varName = matchResult.groupValues[1]
            // 只有当变量名在 varNames 中时才添加
            if (varNames.contains(varName)) {
                references.add(VarReferenceInfo(
                    varName = varName,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = taskId,
                    positionPath = positionPath
                ))
            }
        }

        // 匹配 ${{varName}}
        VAR_PATTERN_2.findAll(value).forEach { matchResult ->
            val varName = matchResult.groupValues[1]
            // 只有当变量名在 varNames 中时才添加
            if (varNames.contains(varName)) {
                references.add(VarReferenceInfo(
                    varName = varName,
                    stageId = stageId,
                    containerId = containerId,
                    taskId = taskId,
                    positionPath = positionPath
                ))
            }
        }

        return references
    }

    /**
     * 检测 StagePauseCheck 中的变量引用
     */
    private fun detectStagePauseCheck(
        stagePauseCheck: StagePauseCheck,
        stageId: String,
        positionPath: String,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()

        try {
            // 检测 notifyGroup
            stagePauseCheck.notifyGroup?.forEachIndexed { index, group ->
                references.addAll(detectVarInString(
                    value = group,
                    stageId = stageId,
                    positionPath = "$positionPath.notifyGroup[$index]",
                    varNames = varNames
                ))
            }

            // 检测 reviewDesc
            stagePauseCheck.reviewDesc?.let { desc ->
                references.addAll(detectVarInString(
                    value = desc,
                    stageId = stageId,
                    positionPath = "$positionPath.reviewDesc",
                    varNames = varNames
                ))
            }

            // 检测 reviewGroups
            stagePauseCheck.reviewGroups?.forEachIndexed { groupIndex, reviewGroup ->
                // 检测 reviewers
                reviewGroup.reviewers.forEachIndexed { reviewerIndex, reviewer ->
                    references.addAll(detectVarInString(
                        value = reviewer,
                        stageId = stageId,
                        positionPath = "$positionPath.reviewGroups[$groupIndex].reviewers[$reviewerIndex]",
                        varNames = varNames
                    ))
                }
                // 检测 params
                references.addAll(detectParamValues(
                    params = reviewGroup.params,
                    stageId = stageId,
                    basePath = "$positionPath.reviewGroups[$groupIndex].params",
                    varNames = varNames
                ))
            }

            // 检测 reviewParams
            references.addAll(detectParamValues(
                params = stagePauseCheck.reviewParams,
                stageId = stageId,
                basePath = "$positionPath.reviewParams",
                varNames = varNames
            ))
        } catch (e: Throwable) {
            logger.warn("Error while detecting StagePauseCheck var references: positionPath=$positionPath", e)
        }

        return references
    }

    /**
     * 检测参数列表中的 value 字段（公共方法）
     */
    private fun detectParamValues(
        params: List<*>?,
        stageId: String,
        basePath: String,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()

        try {
            params?.forEachIndexed { index, param ->
                val valueField = param?.javaClass?.getDeclaredField("value")
                valueField?.isAccessible = true
                val value = valueField?.get(param)?.toString()

                if (!value.isNullOrBlank()) {
                    references.addAll(detectVarInString(
                        value = value,
                        stageId = stageId,
                        positionPath = "$basePath[$index].value",
                        varNames = varNames
                    ))
                }
            }
        } catch (e: Throwable) {
            logger.warn("Error while detecting param values: basePath=$basePath", e)
        }

        return references
    }

    /**
     * 检测参数中的变量引用
     * 支持 String、List、Map 类型
     */
    private fun detectVarInParams(
        value: Any?,
        stageId: String,
        containerId: String? = null,
        taskId: String? = null,
        positionPath: String,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()

        try {
            when (value) {
                is String -> {
                    // 处理 String 类型：直接检测变量引用
                    if (value.isNotBlank()) {
                        references.addAll(detectVarInString(
                            value = value,
                            stageId = stageId,
                            containerId = containerId,
                            taskId = taskId,
                            positionPath = positionPath,
                            varNames = varNames
                        ))
                    }
                }
                is List<*> -> {
                    // 处理 List 类型
                    value.forEachIndexed { index, item ->
                        if (item is String && item.isNotBlank()) {
                            references.addAll(detectVarInString(
                                value = item,
                                stageId = stageId,
                                containerId = containerId,
                                taskId = taskId,
                                positionPath = "$positionPath[$index]",
                                varNames = varNames
                            ))
                        }
                    }
                }
                is Map<*, *> -> {
                    // 处理 Map 类型
                    value.forEach { (key, mapValue) ->
                        if (mapValue is String && mapValue.isNotBlank()) {
                            references.addAll(detectVarInString(
                                value = mapValue,
                                stageId = stageId,
                                containerId = containerId,
                                taskId = taskId,
                                positionPath = "$positionPath.$key",
                                varNames = varNames
                            ))
                        }
                    }
                }
                null -> {
                    // 忽略 null 值
                }
                else -> {
                    // 其他类型尝试转换为字符串
                    val strValue = value.toString()
                    if (strValue.isNotBlank()) {
                        references.addAll(detectVarInString(
                            value = strValue,
                            stageId = stageId,
                            containerId = containerId,
                            taskId = taskId,
                            positionPath = positionPath,
                            varNames = varNames
                        ))
                    }
                }
            }
        } catch (e: Throwable) {
            logger.warn("Error while detecting var in params: positionPath=$positionPath", e)
        }

        return references
    }

    /**
     * 检测自定义变量列表中的变量引用（公共方法）
     * 用于处理 customVariables、customEnv 等字段
     */
    private fun detectCustomVariables(
        customVariables: List<*>?,
        stageId: String,
        containerId: String? = null,
        taskId: String? = null,
        basePath: String,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()

        try {
            customVariables?.forEachIndexed { index, variable ->
                // 使用反射获取 key 和 value 属性
                val keyField = variable?.javaClass?.getDeclaredField("key")
                val valueField = variable?.javaClass?.getDeclaredField("value")

                keyField?.isAccessible = true
                valueField?.isAccessible = true

                val key = keyField?.get(variable) as? String
                val value = valueField?.get(variable) as? String

                // 检测 key
                if (!key.isNullOrBlank() && varNames.contains(key)) {
                    references.add(VarReferenceInfo(
                        varName = key,
                        stageId = stageId,
                        containerId = containerId,
                        taskId = taskId,
                        positionPath = "$basePath[$index].key"
                    ))
                }

                // 检测 value
                if (!value.isNullOrBlank()) {
                    references.addAll(detectVarInString(
                        value = value,
                        stageId = stageId,
                        containerId = containerId,
                        taskId = taskId,
                        positionPath = "$basePath[$index].value",
                        varNames = varNames
                    ))
                }
            }
        } catch (e: Throwable) {
            logger.warn("Error while detecting custom variables: basePath=$basePath", e)
        }

        return references
    }

    /**
     * 检测自定义条件表达式中的变量引用
     */
    private fun detectVarInCustomCondition(
        condition: String,
        stageId: String,
        positionPath: String,
        varNames: Set<String>
    ): List<VarReferenceInfo> {
        val references = mutableListOf<VarReferenceInfo>()

        try {
            CUSTOM_CONDITION_PATTERN.findAll(condition).forEach { matchResult ->
                val varName = matchResult.groupValues[1]
                // 只有当变量名在 varNames 中时才添加
                if (varNames.contains(varName)) {
                    references.add(VarReferenceInfo(
                        varName = varName,
                        stageId = stageId,
                        containerId = null,
                        taskId = null,
                        positionPath = positionPath
                    ))
                }
            }
        } catch (e: Throwable) {
            logger.warn("Error while detecting custom condition var references: positionPath=$positionPath", e)
        }

        return references
    }

}
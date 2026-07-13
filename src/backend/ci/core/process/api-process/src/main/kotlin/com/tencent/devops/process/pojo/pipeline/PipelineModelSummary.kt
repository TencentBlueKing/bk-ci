package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.pipeline.PipelineVersionWithModel
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线编排轻量摘要")
data class PipelineModelSummary(
    @get:Schema(title = "流水线ID")
    val pipelineId: String?,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "版本号")
    val version: Int,
    @get:Schema(title = "版本名称")
    val versionName: String?,
    @get:Schema(title = "最新版本号")
    val latestVersion: Int,
    @get:Schema(title = "阶段总数")
    val stageCount: Int,
    @get:Schema(title = "Job总数")
    val containerCount: Int,
    @get:Schema(title = "插件总数")
    val elementCount: Int,
    @get:Schema(title = "是否包含插件列表")
    val includeElements: Boolean,
    @get:Schema(title = "阶段列表")
    val stages: List<PipelineModelStageSummary>,
    @get:Schema(title = "提示信息")
    val notices: List<String>
)

@Schema(title = "流水线编排阶段摘要")
data class PipelineModelStageSummary(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "用户自定义阶段ID")
    val stageIdForUser: String?,
    @get:Schema(title = "阶段名称")
    val stageName: String?,
    @get:Schema(title = "阶段状态")
    val status: String?,
    @get:Schema(title = "是否为Finally阶段")
    val finallyStage: Boolean,
    @get:Schema(title = "Job数量")
    val containerCount: Int,
    @get:Schema(title = "Job列表")
    val containers: List<PipelineModelContainerSummary>
)

@Schema(title = "流水线编排Job摘要")
data class PipelineModelContainerSummary(
    @get:Schema(title = "容器Hash ID")
    val containerHashId: String?,
    @get:Schema(title = "容器ID")
    val containerId: String?,
    @get:Schema(title = "用户自定义Job ID")
    val jobId: String?,
    @get:Schema(title = "Job名称")
    val containerName: String,
    @get:Schema(title = "容器类型")
    val containerType: String,
    @get:Schema(title = "容器状态")
    val status: String?,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "是否为矩阵组")
    val matrixGroupFlag: Boolean,
    @get:Schema(title = "矩阵上下文")
    val matrixContext: Map<String, String>?,
    @get:Schema(title = "插件数量")
    val elementCount: Int,
    @get:Schema(title = "插件列表")
    val elements: List<PipelineModelElementSummary>? = null
)

@Schema(title = "流水线编排插件摘要")
data class PipelineModelElementSummary(
    @get:Schema(title = "插件ID")
    val elementId: String?,
    @get:Schema(title = "用户自定义Step ID")
    val stepId: String?,
    @get:Schema(title = "插件名称")
    val elementName: String,
    @get:Schema(title = "插件类型")
    val classType: String,
    @get:Schema(title = "插件编码")
    val atomCode: String,
    @get:Schema(title = "插件版本")
    val version: String,
    @get:Schema(title = "插件状态")
    val status: String?,
    @get:Schema(title = "是否启用")
    val enabled: Boolean
)

@Schema(title = "流水线编排节点详情")
data class PipelineModelNodeDetail(
    @get:Schema(title = "流水线ID")
    val pipelineId: String?,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "版本号")
    val version: Int,
    @get:Schema(title = "版本名称")
    val versionName: String?,
    @get:Schema(title = "最新版本号")
    val latestVersion: Int,
    @get:Schema(title = "命中的节点类型")
    val matchedNodeType: String,
    @get:Schema(title = "命中的定位键")
    val matchedBy: String,
    @get:Schema(title = "节点路径")
    val path: PipelineModelNodePath,
    @get:Schema(title = "阶段详情")
    val stage: PipelineModelStageDetail? = null,
    @get:Schema(title = "Job详情")
    val container: PipelineModelContainerDetail? = null,
    @get:Schema(title = "插件详情")
    val element: PipelineModelElementDetail? = null,
    @get:Schema(title = "提示信息")
    val notices: List<String>
)

@Schema(title = "流水线编排节点路径")
data class PipelineModelNodePath(
    @get:Schema(title = "阶段摘要")
    val stage: PipelineModelStagePath,
    @get:Schema(title = "Job摘要")
    val container: PipelineModelContainerPath? = null,
    @get:Schema(title = "插件摘要")
    val element: PipelineModelElementPath? = null
)

@Schema(title = "流水线编排节点路径-阶段")
data class PipelineModelStagePath(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "用户自定义阶段ID")
    val stageIdForUser: String?,
    @get:Schema(title = "阶段名称")
    val stageName: String?
)

@Schema(title = "流水线编排节点路径-Job")
data class PipelineModelContainerPath(
    @get:Schema(title = "容器Hash ID")
    val containerHashId: String?,
    @get:Schema(title = "容器ID")
    val containerId: String?,
    @get:Schema(title = "用户自定义Job ID")
    val jobId: String?,
    @get:Schema(title = "Job名称")
    val containerName: String
)

@Schema(title = "流水线编排节点路径-插件")
data class PipelineModelElementPath(
    @get:Schema(title = "插件ID")
    val elementId: String?,
    @get:Schema(title = "用户自定义Step ID")
    val stepId: String?,
    @get:Schema(title = "插件名称")
    val elementName: String
)

@Schema(title = "流水线编排阶段详情")
data class PipelineModelStageDetail(
    @get:Schema(title = "阶段ID")
    val stageId: String?,
    @get:Schema(title = "用户自定义阶段ID")
    val stageIdForUser: String?,
    @get:Schema(title = "阶段名称")
    val stageName: String?,
    @get:Schema(title = "阶段标签")
    val tag: List<String>?,
    @get:Schema(title = "阶段状态")
    val status: String?,
    @get:Schema(title = "是否为Finally阶段")
    val finallyStage: Boolean,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "Job数量")
    val containerCount: Int,
    @get:Schema(title = "Job摘要")
    val containers: List<PipelineModelContainerSummary>
)

@Schema(title = "流水线编排Job详情")
data class PipelineModelContainerDetail(
    @get:Schema(title = "容器Hash ID")
    val containerHashId: String?,
    @get:Schema(title = "容器ID")
    val containerId: String?,
    @get:Schema(title = "用户自定义Job ID")
    val jobId: String?,
    @get:Schema(title = "Job名称")
    val containerName: String,
    @get:Schema(title = "容器类型")
    val containerType: String,
    @get:Schema(title = "容器状态")
    val status: String?,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "是否为矩阵组")
    val matrixGroupFlag: Boolean,
    @get:Schema(title = "矩阵上下文")
    val matrixContext: Map<String, String>?,
    @get:Schema(title = "插件数量")
    val elementCount: Int,
    @get:Schema(title = "插件摘要")
    val elements: List<PipelineModelElementSummary>
)

@Schema(title = "流水线编排插件详情")
data class PipelineModelElementDetail(
    @get:Schema(title = "插件ID")
    val elementId: String?,
    @get:Schema(title = "用户自定义Step ID")
    val stepId: String?,
    @get:Schema(title = "插件名称")
    val elementName: String,
    @get:Schema(title = "插件类型")
    val classType: String,
    @get:Schema(title = "插件编码")
    val atomCode: String,
    @get:Schema(title = "插件版本")
    val version: String,
    @get:Schema(title = "插件状态")
    val status: String?,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "错误类型")
    val errorType: String?,
    @get:Schema(title = "错误码")
    val errorCode: Int?,
    @get:Schema(title = "错误信息")
    val errorMsg: String?,
    @get:Schema(title = "附加参数")
    val additionalOptions: Map<String, Any?>?,
    @get:Schema(title = "脚本预览")
    val scriptPreview: String? = null
)

fun PipelineVersionWithModel.toPipelineModelSummary(includeElements: Boolean = true): PipelineModelSummary {
    val model = modelAndSetting.model
    val notices = mutableListOf("返回为 AI 轻量编排摘要，适合先定位 Stage/Job/插件，再决定是否继续深挖。")
    if (!includeElements) {
        notices.add("当前未展开插件列表；如需 step 级定位，请将 includeElements 设为 true。")
    }
    val stages = model.stages.map { stage ->
        val containers = stage.expandContainers()
        PipelineModelStageSummary(
            stageId = stage.id,
            stageIdForUser = stage.stageIdForUser,
            stageName = stage.name,
            status = stage.status,
            finallyStage = stage.finally,
            containerCount = containers.size,
            containers = containers.map { container ->
                container.toSummary(includeElements = includeElements)
            }
        )
    }
    return PipelineModelSummary(
        pipelineId = model.pipelineId,
        pipelineName = model.name,
        version = version,
        versionName = versionName,
        latestVersion = latestVersion,
        stageCount = model.stages.size,
        containerCount = model.stages.sumOf { it.expandContainers().size },
        elementCount = model.stages.sumOf { stage ->
            stage.expandContainers().sumOf { container -> container.elements.size }
        },
        includeElements = includeElements,
        stages = stages,
        notices = notices
    )
}

fun PipelineVersionWithModel.findPipelineNodeDetails(
    stageId: String? = null,
    containerHashId: String? = null,
    containerId: String? = null,
    jobId: String? = null,
    elementId: String? = null,
    stepId: String? = null
): List<PipelineModelNodeDetail> {
    val model = modelAndSetting.model
    val stageEntries = model.stages.map { stage -> PipelineStageEntry(stage = stage) }
    val containerEntries = model.stages.flatMap { stage ->
        stage.expandContainers().map { container ->
            PipelineContainerEntry(stage = stage, container = container)
        }
    }
    val elementEntries = containerEntries.flatMap { containerEntry ->
        containerEntry.container.elements.map { element ->
            PipelineElementEntry(
                stage = containerEntry.stage,
                container = containerEntry.container,
                element = element
            )
        }
    }
    return when {
        !elementId.isNullOrBlank() -> {
            elementEntries.filter { it.element.id == elementId }.map { it.toNodeDetail(this, "elementId") }
        }

        !stepId.isNullOrBlank() -> {
            elementEntries.filter { it.element.stepId == stepId }.map { it.toNodeDetail(this, "stepId") }
        }

        !containerHashId.isNullOrBlank() -> {
            containerEntries.filter { it.container.containerHashId == containerHashId }
                .map { it.toNodeDetail(this, "containerHashId") }
        }

        !containerId.isNullOrBlank() -> {
            containerEntries.filter { entry ->
                entry.container.id == containerId || entry.container.containerId == containerId
            }.map { it.toNodeDetail(this, "containerId") }
        }

        !jobId.isNullOrBlank() -> {
            containerEntries.filter { it.container.jobId == jobId }.map { it.toNodeDetail(this, "jobId") }
        }

        !stageId.isNullOrBlank() -> {
            stageEntries.filter { entry ->
                entry.stage.id == stageId || entry.stage.stageIdForUser == stageId
            }.map { it.toNodeDetail(this, "stageId") }
        }

        else -> emptyList()
    }
}

private data class PipelineStageEntry(
    val stage: Stage
) {
    fun toNodeDetail(versionWithModel: PipelineVersionWithModel, matchedBy: String): PipelineModelNodeDetail {
        val containers = stage.expandContainers()
        val model = versionWithModel.modelAndSetting.model
        return PipelineModelNodeDetail(
            pipelineId = model.pipelineId,
            pipelineName = model.name,
            version = versionWithModel.version,
            versionName = versionWithModel.versionName,
            latestVersion = versionWithModel.latestVersion,
            matchedNodeType = "stage",
            matchedBy = matchedBy,
            path = PipelineModelNodePath(stage = stage.toPath()),
            stage = PipelineModelStageDetail(
                stageId = stage.id,
                stageIdForUser = stage.stageIdForUser,
                stageName = stage.name,
                tag = stage.tag,
                status = stage.status,
                finallyStage = stage.finally,
                enabled = stage.stageEnabled(),
                containerCount = containers.size,
                containers = containers.map { it.toSummary(includeElements = false) }
            ),
            notices = listOf("当前返回单个 Stage 的轻量详情；如需查看具体插件，请继续使用 container 或 element 定位键。")
        )
    }
}

private data class PipelineContainerEntry(
    val stage: Stage,
    val container: Container
) {
    fun toNodeDetail(versionWithModel: PipelineVersionWithModel, matchedBy: String): PipelineModelNodeDetail {
        val model = versionWithModel.modelAndSetting.model
        return PipelineModelNodeDetail(
            pipelineId = model.pipelineId,
            pipelineName = model.name,
            version = versionWithModel.version,
            versionName = versionWithModel.versionName,
            latestVersion = versionWithModel.latestVersion,
            matchedNodeType = "container",
            matchedBy = matchedBy,
            path = PipelineModelNodePath(
                stage = stage.toPath(),
                container = container.toPath()
            ),
            container = PipelineModelContainerDetail(
                containerHashId = container.containerHashId,
                containerId = container.id ?: container.containerId,
                jobId = container.jobId,
                containerName = container.name,
                containerType = container.getClassType(),
                status = container.status,
                enabled = container.containerEnabled(),
                matrixGroupFlag = container.matrixGroupFlag == true,
                matrixContext = container.fetchMatrixContext(),
                elementCount = container.elements.size,
                elements = container.elements.map { it.toSummary() }
            ),
            notices = listOf("当前返回单个 Job 的轻量详情；如需查看具体插件配置，请继续使用 elementId 或 stepId 定位。")
        )
    }
}

private data class PipelineElementEntry(
    val stage: Stage,
    val container: Container,
    val element: Element
) {
    fun toNodeDetail(versionWithModel: PipelineVersionWithModel, matchedBy: String): PipelineModelNodeDetail {
        val model = versionWithModel.modelAndSetting.model
        return PipelineModelNodeDetail(
            pipelineId = model.pipelineId,
            pipelineName = model.name,
            version = versionWithModel.version,
            versionName = versionWithModel.versionName,
            latestVersion = versionWithModel.latestVersion,
            matchedNodeType = "element",
            matchedBy = matchedBy,
            path = PipelineModelNodePath(
                stage = stage.toPath(),
                container = container.toPath(),
                element = element.toPath()
            ),
            element = PipelineModelElementDetail(
                elementId = element.id,
                stepId = element.stepId,
                elementName = element.name,
                classType = element.getClassType(),
                atomCode = element.getAtomCode(),
                version = element.version,
                status = element.status,
                enabled = element.elementEnabled(),
                errorType = element.errorType,
                errorCode = element.errorCode,
                errorMsg = element.errorMsg,
                additionalOptions = element.additionalOptions?.let { options ->
                    mapOf(
                        "enable" to options.enable,
                        "continueWhenFailed" to options.continueWhenFailed,
                        "manualSkip" to options.manualSkip,
                        "retryWhenFailed" to options.retryWhenFailed,
                        "retryCount" to options.retryCount,
                        "timeout" to options.timeout,
                        "runCondition" to options.runCondition?.name
                    )
                },
                scriptPreview = buildScriptPreview(element)
            ),
            notices = listOf("当前返回单个插件的轻量详情；如需完整编排或完整插件对象，请再调用获取流水线编排。")
        )
    }
}

private fun Stage.toPath(): PipelineModelStagePath {
    return PipelineModelStagePath(
        stageId = id,
        stageIdForUser = stageIdForUser,
        stageName = name
    )
}

private fun Container.toPath(): PipelineModelContainerPath {
    return PipelineModelContainerPath(
        containerHashId = containerHashId,
        containerId = id ?: containerId,
        jobId = jobId,
        containerName = name
    )
}

private fun Element.toPath(): PipelineModelElementPath {
    return PipelineModelElementPath(
        elementId = id,
        stepId = stepId,
        elementName = name
    )
}

private fun Container.toSummary(includeElements: Boolean): PipelineModelContainerSummary {
    return PipelineModelContainerSummary(
        containerHashId = containerHashId,
        containerId = id ?: containerId,
        jobId = jobId,
        containerName = name,
        containerType = getClassType(),
        status = status,
        enabled = containerEnabled(),
        matrixGroupFlag = matrixGroupFlag == true,
        matrixContext = fetchMatrixContext(),
        elementCount = elements.size,
        elements = if (includeElements) {
            elements.map { it.toSummary() }
        } else {
            null
        }
    )
}

private fun Element.toSummary(): PipelineModelElementSummary {
    return PipelineModelElementSummary(
        elementId = id,
        stepId = stepId,
        elementName = name,
        classType = getClassType(),
        atomCode = getAtomCode(),
        version = version,
        status = status,
        enabled = elementEnabled()
    )
}

private fun Stage.expandContainers(): List<Container> {
    return containers.flatMap { container ->
        listOf(container) + (container.fetchGroupContainers() ?: emptyList())
    }
}

private fun buildScriptPreview(element: Element): String? {
    val script = when (element) {
        is com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement -> element.script
        is com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement -> element.script
        else -> null
    } ?: return null
    return if (script.length <= SCRIPT_PREVIEW_LENGTH) {
        script
    } else {
        script.take(SCRIPT_PREVIEW_LENGTH) + "...(脚本已截断)"
    }
}

private const val SCRIPT_PREVIEW_LENGTH = 500

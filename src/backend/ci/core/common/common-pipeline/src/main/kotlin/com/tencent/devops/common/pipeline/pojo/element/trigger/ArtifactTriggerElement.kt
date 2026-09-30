package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactKind
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactMetadataOperator
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactRepositoryType
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.ArtifactTriggerEventType
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品到达触发器
 */
@Schema(title = "制品到达触发", description = ArtifactTriggerElement.classType)
data class ArtifactTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "制品到达触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "插件用户ID", required = false)
    override var stepId: String? = null,
    @get:Schema(title = "数据", required = true)
    val data: ArtifactTriggerData
) : Element(name, id, status) {
    companion object {
        const val classType = "codeArtifactWebhookTrigger"
    }

    override fun getClassType() = classType

    override fun findFirstTaskIdByStartType(startType: StartType): String {
        return if (startType.name == StartType.WEB_HOOK.name) {
            this.id!!
        } else {
            super.findFirstTaskIdByStartType(startType)
        }
    }
}

@Schema(title = "制品到达触发数据包装")
data class ArtifactTriggerData(
    @get:Schema(title = "制品到达触发数据", required = true)
    val input: ArtifactTriggerInput
)

@Schema(title = "制品到达触发数据")
data class ArtifactTriggerInput(
    @get:Schema(title = "监听仓库 (pipeline/custom/image)", required = true)
    val repository: ArtifactRepositoryType,
    @get:Schema(title = "监听流水线 (单选，流水线仓库必填)", required = false)
    val watchPipeline: String? = "",
    @get:Schema(title = "监听根路径 (仅自定义仓库必填，可为 /aaa/bbb/)", required = false)
    val watchRootPath: String? = "",
    @get:Schema(title = "监听范围 (file/folder)", required = false)
    val kind: ArtifactKind? = ArtifactKind.FILE,
    @get:Schema(title = "监听事件类型 (当前仅 arrived)", required = true)
    val eventType: ArtifactTriggerEventType,
    @get:Schema(title = "匹配名称 Glob (仅流水线仓库)", required = false)
    val artifactsName: String? = "",
    @get:Schema(title = "排除名称 Glob (仅流水线仓库)", required = false)
    val artifactsNameIgnore: String? = "",
    @get:Schema(title = "匹配路径 Glob (仅自定义仓库)", required = false)
    val paths: String? = "",
    @get:Schema(title = "排除路径 Glob (仅自定义仓库)", required = false)
    val pathsIgnore: String? = "",
    @get:Schema(title = "镜像名 (仅镜像仓库)", required = false)
    val image: String? = "",
    @get:Schema(title = "匹配 Tag Glob (仅镜像仓库)", required = false)
    val tags: String? = "",
    @get:Schema(title = "排除 Tag Glob (仅镜像仓库)", required = false)
    val tagsIgnore: String? = "",
    @get:Schema(title = "元数据过滤 (键/运算符/值)", required = false)
    val metadata: List<ArtifactMetadataFilter>? = null
)

@Schema(title = "制品元数据过滤条件")
data class ArtifactMetadataFilter(
    @get:Schema(title = "元数据键", required = true)
    val key: String,
    @get:Schema(title = "运算符", required = true)
    val operator: ArtifactMetadataOperator = ArtifactMetadataOperator.EQ,
    @get:Schema(title = "元数据值 (EXISTS/NOT_EXISTS 可为空)", required = false)
    val value: String = ""
)

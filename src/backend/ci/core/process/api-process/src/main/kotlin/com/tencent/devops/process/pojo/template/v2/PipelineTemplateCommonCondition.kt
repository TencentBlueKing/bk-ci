package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema
import kotlin.reflect.full.memberProperties

@Schema(title = "流水线模板通用条件")
data class PipelineTemplateCommonCondition(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String? = null,
    @get:Schema(title = "模板ID", required = false)
    val templateId: String? = null,
    @get:Schema(title = "过滤模板ID", required = false)
    var filterTemplateIds: List<String>? = null,
    @get:Schema(title = "根据名称模糊搜索", required = false)
    val fuzzySearchName: String? = null,
    @get:Schema(title = "根据名称精准搜索", required = false)
    val exactSearchName: String? = null,
    @get:Schema(title = "描述", required = false)
    val desc: String? = null,
    @get:Schema(title = "公共/约束/自定义模式", required = false)
    val mode: TemplateType? = null,
    @get:Schema(title = "升级策略-用于研发商店安装的模板", required = true)
    val upgradeStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "应用范畴", required = false)
    val category: String? = null,
    @get:Schema(title = "模板类型", required = false)
    val type: PipelineTemplateType? = null,
    @get:Schema(title = "是否开启PAC", required = false)
    val enablePac: Boolean? = null,
    @get:Schema(title = "最新发布版本号", required = false)
    val releasedVersion: Long? = null,
    @get:Schema(title = "最新发布版本名称", required = false)
    val releasedVersionName: String? = null,
    @get:Schema(title = "最新发布配置版本号", required = false)
    val releasedSettingVersion: Int? = null,
    @get:Schema(title = "模板状态", required = false)
    val latestVersionStatus: VersionStatus? = null,
    @get:Schema(title = "是否关联研发商店", required = false)
    val storeFlag: Boolean? = null,
    @get:Schema(title = "父模板ID", required = false)
    val srcTemplateId: String? = null,
    @get:Schema(title = "父模板项目ID", required = false)
    val srcTemplateProjectId: String? = null,
    @get:Schema(title = "调试流水线数", required = false)
    val debugPipelineCount: Int? = null,
    @get:Schema(title = "实例流水线数", required = false)
    val instancePipelineCount: Int? = null,
    @get:Schema(title = "研发商店状态", required = false)
    val storeStatus: TemplateStatusEnum? = null,
    @get:Schema(title = "创建人", required = false)
    val creator: String? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "page", required = true)
    val page: Int? = null,
    @get:Schema(title = "pageSize", required = true)
    val pageSize: Int? = null
) {
    fun checkAllFieldsAreNull() {
        val isAllFieldsAreNull = this::class.memberProperties.all {
            it.call(this) == null
        }
        if (isAllFieldsAreNull) {
            throw InvalidParamException(message = "all parameters cannot be null.")
        }
    }
}

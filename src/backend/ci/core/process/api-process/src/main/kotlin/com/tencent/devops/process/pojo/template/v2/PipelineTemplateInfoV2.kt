package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板基础信息")
data class PipelineTemplateInfoV2(
    @get:Schema(title = "模板ID", required = true)
    val id: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "模板名称", required = true)
    val name: String,
    @get:Schema(title = "简介", required = true)
    val desc: String?,
    @get:Schema(title = "公共/约束/自定义模式", required = true)
    val mode: TemplateType,
    @get:Schema(title = "应用范畴", required = true)
    val category: String? = null,
    @get:Schema(title = "模板类型", required = true)
    val type: PipelineTemplateType,
    @get:Schema(title = "logo地址", required = false)
    val logoUrl: String? = null,
    @get:Schema(title = "是否开启PAC", required = true)
    val enablePac: Boolean,
    @get:Schema(
        title = "" +
            "最新发布版本号,最新版本号可能是草稿版本,也可能是分支版本,只有发布过正式版本后,才表示最新的正式版本" +
            "- 当只有草稿版本时,那么表示的是草稿版本" +
            "- 当只有分支版本时,那么表示的是创建时的分支版本", required = true
    )
    val releasedVersion: Long = 0,
    @get:Schema(title = "最新发布版本名称", required = false)
    val releasedVersionName: String? = null,
    @get:Schema(title = "最新发布配置版本号", required = false)
    val releasedSettingVersion: Int = 0,
    @get:Schema(title = "模板状态", required = false)
    val latestVersionStatus: VersionStatus,
    @get:Schema(title = "来源名称", required = true)
    val sourceName: String? = null,
    @get:Schema(title = "研发商店状态", required = true)
    val storeStatus: TemplateStatusEnum = TemplateStatusEnum.NEVER_PUBLISHED,
    @get:Schema(title = "父模板ID", required = false)
    val srcTemplateId: String? = null,
    @get:Schema(title = "父模板项目ID", required = false)
    val srcTemplateProjectId: String? = null,
    @get:Schema(title = "调试流水线数", required = true)
    val debugPipelineCount: Int = 0,
    @get:Schema(title = "实例流水线数", required = true)
    val instancePipelineCount: Int = 0,
    @get:Schema(title = "升级策略-用于研发商店安装的模板", required = true)
    val upgradeStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "配置同步策略-用于研发商店安装的模板", required = true)
    val settingSyncStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "发布策略-研发商店", required = true)
    val publishStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null,
    @get:Schema(title = "创建时间", required = false)
    val createdTime: Long? = null,
    @get:Schema(title = "更新时间", required = false)
    val updateTime: Long? = null
)

package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线模板更新请求体")
data class PipelineTemplateInfoUpdateInfo(
    @get:Schema(title = "模板名称", required = true)
    val name: String? = null,
    @get:Schema(title = "简介", required = true)
    val desc: String? = null,
    @get:Schema(title = "应用范畴", required = true)
    val category: String? = null,
    @get:Schema(title = "logo地址", required = true)
    val logoUrl: String? = null,
    @get:Schema(title = "是否开启PAC", required = true)
    val enablePac: Boolean? = null,
    @get:Schema(title = "最新发布版本号", required = true)
    val releasedVersion: Long? = null,
    @get:Schema(title = "最新发布版本名称", required = false)
    val releasedVersionName: String? = null,
    @get:Schema(title = "最新发布配置版本号", required = false)
    val releasedSettingVersion: Int? = null,
    @get:Schema(title = "发布策略-研发商店", required = true)
    val publishStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "模板状态", required = false)
    val latestVersionStatus: VersionStatus? = null,
    @get:Schema(title = "升级策略-用于研发商店安装的模板", required = true)
    val upgradeStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "配置同步策略-用于研发商店安装的模板", required = true)
    val settingSyncStrategy: UpgradeStrategyEnum? = null,
    @get:Schema(title = "研发商店状态", required = true)
    val storeStatus: TemplateStatusEnum? = null,
    @get:Schema(title = "调试流水线数", required = true)
    val debugPipelineCount: Int? = null,
    @get:Schema(title = "实例流水线数", required = true)
    val instancePipelineCount: Int? = null,
    @get:Schema(title = "更新人", required = true)
    val updater: String? = null
)

package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "功能开关配置")
data class FeatureSwitch(
    @Schema(description = "主键ID", readOnly = true)
    val id: Long? = null,
    @Schema(description = "项目ID，空字符串表示全局")
    val projectId: String = "",
    @Schema(description = "用户ID，空字符串表示所有用户")
    val userId: String = "",
    @Schema(description = "工作空间名称，空字符串表示所有工作空间")
    val workspaceName: String = "",
    @Schema(description = "功能类型")
    val featureType: FeatureSwitchType,
    @Schema(description = "是否启用")
    val enabled: Boolean = false,
    @Schema(description = "创建人", readOnly = true)
    val creator: String? = null,
    @Schema(description = "更新人", readOnly = true)
    val updater: String? = null,
    @Schema(description = "创建时间", readOnly = true)
    val createTime: LocalDateTime? = null,
    @Schema(description = "更新时间", readOnly = true)
    val updateTime: LocalDateTime? = null
)

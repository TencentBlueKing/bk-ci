package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "远程开发用户配置")
data class RemoteDevUserSettings(
    @Schema(description = "用户信息，用于OP页面操作")
    var userId: String? = null,
    @Schema(description = "设置是否仅使用云端IDE")
    var onlyCloudIDE: Boolean = false,
    @Schema(description = "最大运行数")
    var maxRunningCount: Int = 2,
    @Schema(description = "最大创建个数")
    var maxHavingCount: Int = 5,
    @Schema(description = "是否允许复制")
    var allowedCopy: Boolean = true,
    @Schema(description = "是否允许下载")
    var allowedDownload: Boolean = true,
    @Schema(description = "是否需要数字水印")
    var needWatermark: Boolean = true,
    @Schema(description = "自动销毁连续空闲时间")
    var autoDeletedDays: Int = 14,
    @Schema(description = "设置linux构建集群类型")
    var mountType: WorkspaceMountType? = null,
    @Schema(description = "云桌面剩余体验时长")
    var remainExperienceDuration: Int? = null,
    @Schema(description = "START云桌面体验时长")
    var startCloudExperienceDuration: Int? = null,
    @Schema(description = "是否加入client白名单")
    var clientWhiteList: Boolean? = true,
    @Schema(description = "是否加入START云桌面白名单")
    var startWhiteList: Boolean? = false
)

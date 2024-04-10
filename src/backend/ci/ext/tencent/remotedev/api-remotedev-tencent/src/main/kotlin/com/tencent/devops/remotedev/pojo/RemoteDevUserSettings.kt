package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "远程开发用户配置")
data class RemoteDevUserSettings(
    @get:Schema(title = "用户信息，用于OP页面操作")
    var userId: String? = null,
    @get:Schema(title = "设置是否仅使用云端IDE")
    var onlyCloudIDE: Boolean = false,
    @get:Schema(title = "最大运行数")
    var maxRunningCount: Int = 2,
    @get:Schema(title = "最大创建个数")
    var maxHavingCount: Int = 5,
    @get:Schema(title = "是否允许复制")
    var allowedCopy: Boolean = true,
    @get:Schema(title = "是否允许下载")
    var allowedDownload: Boolean = true,
    @get:Schema(title = "是否需要数字水印")
    var needWatermark: Boolean = true,
    @get:Schema(title = "自动销毁连续空闲时间")
    var autoDeletedDays: Int = 14,
    @get:Schema(title = "设置linux构建集群类型")
    var mountType: WorkspaceMountType? = null,
    @get:Schema(title = "云桌面剩余体验时长")
    var remainExperienceDuration: Int? = null,
    @get:Schema(title = "START云桌面体验时长")
    var startCloudExperienceDuration: Int? = null,
    @get:Schema(title = "是否加入client白名单")
    var clientWhiteList: Boolean? = true,
    @get:Schema(title = "是否加入START云桌面白名单")
    var startWhiteList: Boolean? = false
)

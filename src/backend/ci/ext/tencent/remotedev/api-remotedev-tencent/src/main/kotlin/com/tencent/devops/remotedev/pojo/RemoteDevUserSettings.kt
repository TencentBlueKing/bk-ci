package com.tencent.devops.remotedev.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("远程开发用户配置")
data class RemoteDevUserSettings(
    @ApiModelProperty("设置是否仅使用云端IDE")
    var onlyCloudIDE: Boolean = false,
    @ApiModelProperty("最大运行数")
    var maxRunningCount: Int = 2,
    @ApiModelProperty("最大创建个数")
    var maxHavingCount: Int = 5,
    @ApiModelProperty("是否允许复制")
    var allowedCopy: Boolean = true,
    @ApiModelProperty("是否允许下载")
    var allowedDownload: Boolean = true,
    @ApiModelProperty("是否需要数字水印")
    var needWatermark: Boolean = true,
    @ApiModelProperty("自动销毁连续空闲时间")
    var autoDeletedDays: Int = 14,
    @ApiModelProperty("START云桌面体验时长")
    var startCloudExperienceDuration: Int = 24
)

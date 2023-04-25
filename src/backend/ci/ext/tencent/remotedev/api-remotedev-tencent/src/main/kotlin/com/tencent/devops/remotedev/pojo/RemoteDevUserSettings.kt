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
    var maxHavingCount: Int = 5
)

package com.tencent.devops.plugin.pojo.enums

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚app扫描任务状态")
enum class JinGangAppStatus(private val status: Int) {
    @ApiModelProperty("成功")
    SUCCESS(0),
    @ApiModelProperty("失败")
    FAILED(1)
}
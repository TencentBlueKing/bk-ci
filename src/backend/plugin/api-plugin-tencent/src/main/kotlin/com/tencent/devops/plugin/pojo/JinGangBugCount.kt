package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚高中低危漏洞统计")
data class JinGangBugCount(
    @ApiModelProperty("低危漏洞统计")
    var lowCount: Int = 0,
    @ApiModelProperty("中危漏洞统计")
    var mediumCount: Int = 0,
    @ApiModelProperty("高危漏洞统计")
    var highCount: Int = 0
)

package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("查询有权限账号列表")
data class JobCloudGetAccountListResult(
    @ApiModelProperty(value = "有权限账号列表")
    val data: List<JobCloudAuthorizedAccount>?,
    @ApiModelProperty(value = "分页记录起始位置", notes = "不传默认0", required = true)
    val start: Int,
    @ApiModelProperty(value = "查询结果总量", required = true)
    val total: Int,
    @ApiModelProperty(value = "单次返回最大记录数。最大1000，不传默认为20。", required = true)
    val length: Int
) {
    constructor() : this(null, 0, 0, 0)
}
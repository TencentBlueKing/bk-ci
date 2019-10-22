package com.tencent.devops.dispatch.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * Created by rdeng on 2017/9/1.
 */
@ApiModel("VM TASK DETAIL-分页-基本信息")
data class DockerHostZoneWithPage(
    @ApiModelProperty("VM DETAIL总数", required = true)
    val total: Int,
    @ApiModelProperty("VM DETAIL列表", required = true)
    val data: List<DockerHostZone>
)
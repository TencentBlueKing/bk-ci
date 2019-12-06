package com.tencent.devops.store.pojo.image.request

import com.tencent.devops.store.pojo.common.DeptInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像市场-镜像可见范围请求报文体")
data class ImageVisibleDeptReq(
    @ApiModelProperty("镜像代码", required = true)
    val imageCode: String,
    @ApiModelProperty("机构列表", required = true)
    val deptInfos: List<DeptInfo>
)
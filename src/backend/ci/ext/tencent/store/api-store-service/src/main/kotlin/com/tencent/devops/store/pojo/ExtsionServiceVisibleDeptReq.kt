package com.tencent.devops.store.pojo

import com.tencent.devops.store.pojo.common.DeptInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务可见范围")
data class ExtsionServiceVisibleDeptReq (
    @ApiModelProperty("扩展服务编码", required = true)
    val serviceCode: String,
    @ApiModelProperty("机构列表", required = true)
    val deptInfos: List<DeptInfo>
)
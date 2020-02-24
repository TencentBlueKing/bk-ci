package com.tencent.devops.store.pojo.dto

import com.tencent.devops.store.pojo.MediaInfoReq
import com.tencent.devops.store.pojo.common.DeptInfo
import io.swagger.annotations.ApiModelProperty

data class ExtSubmitDTO(
    @ApiModelProperty("评论信息", required = true)
    val mediaInfoList: List<MediaInfoReq>,
    @ApiModelProperty("机构列表", required = true)
    val deptInfoList: List<DeptInfo>
)
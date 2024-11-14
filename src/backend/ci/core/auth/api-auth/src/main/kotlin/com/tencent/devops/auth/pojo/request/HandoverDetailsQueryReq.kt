package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.enum.HandoverType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限交接详细查询请求体")
data class HandoverDetailsQueryReq(
    @get:Schema(title = "流程单号")
    val flowNo: String,
    @get:Schema(title = "组/授权资源关联的资源类型")
    val resourceType: String,
    @get:Schema(title = "交接类型")
    val handoverType: HandoverType,
    @get:Schema(title = "第几页")
    val page: Int,
    @get:Schema(title = "每页大小")
    val pageSize: Int
)

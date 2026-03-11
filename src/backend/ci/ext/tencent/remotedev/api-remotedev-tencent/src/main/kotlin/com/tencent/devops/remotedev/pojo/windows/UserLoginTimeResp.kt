package com.tencent.devops.remotedev.pojo.windows

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户时间段登录数量")
data class UserLoginTimeResp(
    val count: Int,
    val data: List<UserLoginTimeRespData>
)

data class UserLoginTimeRespData(
    val num: Int,
    val time: String
)

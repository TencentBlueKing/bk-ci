package com.tencent.devops.remotedev.pojo.windows

import io.swagger.annotations.ApiModel

@ApiModel("用户时间段登录数量")
data class UserLoginTimeResp(
    val count: Int,
    val data: List<UserLoginTimeRespData>
)

data class UserLoginTimeRespData(
    val num: Int,
    val time: String
)
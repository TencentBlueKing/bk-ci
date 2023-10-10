package com.tencent.devops.remotedev.pojo.windows

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiParam

@ApiModel("windows 机器状态信息")
data class ComputerStatusResp(
    @ApiParam("机器总数")
    val count: Int,
    @ApiParam("机器状态信息")
    val status: List<ComputerStatusData>,
    @ApiParam("机器登录信息")
    val users: List<ComputerUserData>
)

@ApiModel("机器状态信息")
data class ComputerStatusData(
    val value: Int,
    val type: ComputerStatusEnum
)

enum class ComputerStatusEnum {
    NORMAL,
    ABNORMAL
}

data class ComputerUserData(
    val value: Int,
    val type: ComputerUserEnum
)

enum class ComputerUserEnum {
    LOGIN,
    LOGOUT
}
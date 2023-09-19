package com.tencent.devops.remotedev.pojo.windows

import io.swagger.annotations.ApiParam

data class WindowsPoolListFetchData(
    @ApiParam(value = "zoneId", required = false)
    val zoneId: String?,
    @ApiParam(value = "machineType", required = false)
    val machineType: String?,
    @ApiParam(value = "ip", required = false)
    val ips: List<String>?,
    @ApiParam(value = "status", required = false)
    val status: Int?,
    @ApiParam("第几页", required = false, defaultValue = "1")
    val page: Int?,
    @ApiParam("每页多少条", required = false, defaultValue = "6666")
    val pageSize: Int?
)

package com.tencent.devops.remotedev.pojo.windows

import io.swagger.v3.oas.annotations.Parameter

data class WindowsPoolListFetchData(
    @Parameter(description = "zoneId", required = false)
    val zoneId: String?,
    @Parameter(description = "machineType", required = false)
    val machineType: String?,
    @Parameter(description = "ips", required = false)
    val ips: List<String>?,
    @Parameter(description = "status", required = false)
    val status: Int?,
    @Parameter(description = "第几页", required = false, example = "1")
    val page: Int?,
    @Parameter(description = "每页多少条", required = false, example = "6666")
    val pageSize: Int?,
    @Parameter(description = "是否过滤锁住的机器")
    val lockedFlag: Boolean?
)

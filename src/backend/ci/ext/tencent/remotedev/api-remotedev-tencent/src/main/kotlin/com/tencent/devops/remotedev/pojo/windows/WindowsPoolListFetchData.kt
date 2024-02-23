package com.tencent.devops.remotedev.pojo.windows

import io.swagger.v3.oas.annotations.Parameter

data class WindowsPoolListFetchData(
    @Parameter(name = "zoneId", required = false)
    val zoneId: String?,
    @Parameter(name = "machineType", required = false)
    val machineType: String?,
    @Parameter(name = "ips", required = false)
    val ips: List<String>?,
    @Parameter(name = "status", required = false)
    val status: Int?,
    @Parameter(name = "第几页", required = false, example = "1")
    val page: Int?,
    @Parameter(name = "每页多少条", required = false, example = "6666")
    val pageSize: Int?,
    @Parameter(name = "是否过滤锁住的机器")
    val lockedFlag: Boolean?
)

package com.tencent.devops.dispatch.devcloud.pojo.devcloud

data class ListPerformanceRsp(
    val actionCode: Int,
    val actionMessage: String,
    val data: List<PerformanceData>
)

data class PerformanceRsp(
    val actionCode: Int,
    val actionMessage: String,
    val data: PerformanceData
)

data class PerformanceData(
    val uid: String,
    val name: String,
    val desc: String
)

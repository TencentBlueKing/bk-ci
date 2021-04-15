package com.tencent.bk.codecc.apiquery.pojo

data class DefectQueryParam(
    val taskIdList: List<Long>,
    val toolName: String?,
    val status: List<Int>?,
    val checker: List<String>?,
    val notChecker: String?,
    val filterFields: List<String>?,
    val startTime: Long?,
    val endTime: Long?,
    val buildId: String?
)
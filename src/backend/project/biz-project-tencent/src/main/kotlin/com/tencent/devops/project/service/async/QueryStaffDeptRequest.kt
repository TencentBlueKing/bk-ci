package com.tencent.devops.project.service.async

/**
 * 异步获取人员机构信息请求体
 * author: carlyin
 * since: 2018-12-09
 */
data class QueryStaffDeptRequest(
    val userId: String,
    val bk_ticket: String
)
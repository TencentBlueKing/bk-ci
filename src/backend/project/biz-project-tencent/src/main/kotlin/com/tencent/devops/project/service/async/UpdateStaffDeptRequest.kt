package com.tencent.devops.project.service.async

/**
 * 更新人员机构信息请求体
 * author: carlyin
 * since: 2018-12-09
 */
data class UpdateStaffDeptRequest(
    val id: Long, // 项目表主键ID
    val bg_name: String,
    val bg_id: Int,
    val dept_name: String,
    val dept_id: Int,
    val center_name: String,
    val center_id: Int
)
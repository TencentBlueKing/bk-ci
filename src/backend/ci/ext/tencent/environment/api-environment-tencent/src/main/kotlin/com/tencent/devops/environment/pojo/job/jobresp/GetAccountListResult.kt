package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "查询有权限账号列表")
data class GetAccountListResult(
    @get:Schema(title = "有权限账号列表")
    val data: List<AuthorizedAccount>?,
    @get:Schema(title = "分页记录起始位置", description = "不传默认0", required = true)
    val start: Int,
    @get:Schema(title = "查询结果总量", required = true)
    val total: Int,
    @get:Schema(title = "单次返回最大记录数。最大1000，不传默认为20。", required = true)
    val length: Int
)
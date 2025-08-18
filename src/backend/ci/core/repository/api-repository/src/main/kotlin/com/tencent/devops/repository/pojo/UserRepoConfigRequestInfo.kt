package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户使用代码库源配置请求信息")
data class UserRepoConfigRequestInfo(
    @get:Schema(title = "用户ID", required = true)
    val userId: String,
    @get:Schema(title = "用户机构ID列表", required = true)
    val userDeptIdList: List<Int>,
    @get:Schema(title = "代码库源标识", required = true)
    val scmCode: String,
    @get:Schema(title = "代码库源机构信息列表", required = true)
    val configDepInfoList: List<RepoConfigDept>?
)

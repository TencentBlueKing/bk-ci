package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class RepositoryConfigDept(
    @get:Schema(title = "机构ID", required = true)
    val deptId: Int,
    @get:Schema(title = "机构名称", required = true)
    val deptName: String
)

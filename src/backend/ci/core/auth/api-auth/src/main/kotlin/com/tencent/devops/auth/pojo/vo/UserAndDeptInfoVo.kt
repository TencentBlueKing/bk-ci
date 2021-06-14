package com.tencent.devops.auth.pojo.vo

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum

data class UserAndDeptInfoVo(
    val id: Int,
    val name: String,
    val type: ManagerScopesEnum,
    val hasChild: Boolean? = false
)

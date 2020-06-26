package com.tencent.devops.auth.entity

data class GroupCreateInfo (
    val groupCode: String,
    val groupType: Int,
    val groupName: String,
    val projectCode: String,
    val user: String
)
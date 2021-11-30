package com.tencent.devops.scm.pojo.p4

data class Workspace(
    val name: String,
    val description: String,
    val root: String,
    val mappings: List<String>
)

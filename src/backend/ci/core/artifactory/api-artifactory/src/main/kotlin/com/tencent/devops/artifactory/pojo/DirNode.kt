package com.tencent.devops.artifactory.pojo

data class DirNode(
    val name: String,
    val fullPath: String,
    val children: MutableList<DirNode>
)
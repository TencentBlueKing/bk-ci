package com.tencent.devops.artifactory.pojo

/**
 * 目录节点
 */
data class DirNode(
    val name: String,
    val fullPath: String,
    val children: MutableList<DirNode>
)

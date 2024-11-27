package com.tencent.devops.dispatch.kubernetes.pojo.image

data class InspectImageResp(
    val arch: String, // 架构
    val os: String, // 系统
    val size: Long, // 大小
    val created: String, // 创建时间
    val id: String, // HashId
    val author: String, // 镜像作者
    val parent: String, // 父级镜像
    val osVersion: String // 系统版本
)

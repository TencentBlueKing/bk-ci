package com.tencent.devops.dispatch.bcs.pojo

/**
 * 镜像仓库
 * @param host 镜像仓库地址
 * @param username 镜像仓库用户名
 * @param password 镜像仓库密码
 */
data class DockerRegistry(
    val host: String,
    val username: String?,
    val password: String?
)

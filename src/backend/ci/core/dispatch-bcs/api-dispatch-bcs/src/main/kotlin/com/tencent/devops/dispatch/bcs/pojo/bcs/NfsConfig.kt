package com.tencent.devops.dispatch.bcs.pojo.bcs

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 挂载nfs的配置
 * @param server nfs服务地址
 * @param path nfs服务存储路径，默认为/
 * @param mountPath nfs在构建机上的挂载路径，默认为/data
 * @param size 空间大小，单位GB，默认为100GB
 */
data class NfsConfig(
    val server: String,
    val path: String? = null,
    @JsonProperty("mount_path")
    val mountPath: String? = null,
    val size: Int? = null
)

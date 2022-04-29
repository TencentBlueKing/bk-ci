package com.tencent.devops.dispatch.bcs.pojo.bcs

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.dispatch.bcs.pojo.DockerRegistry

/**
 * 创建/启动构建机参数
 * @param name 构建机名称
 * @param image 镜像（镜像名:版本)
 * @param registry 镜像仓库信息
 * @param cpu 构建机cpu核数
 * @param mem 构建机内存大小， 256的倍数，比如512M， 1024M， 以M为单位
 * @param disk 构建机磁盘大小，10的倍数，比如50G，60G，以G为单位
 * @param env 构建机环境变量
 * @param command 构建机启动命令
 * @param diskMountPath 在构建机上的挂载路径，默认为/data
 */
data class BcsBuilder(
    val name: String,
    val image: String,
    val registry: DockerRegistry,
    val cpu: Double,
    val mem: Int,
    val disk: Int,
    val env: Map<String, String>?,
    val command: List<String>?,
    @JsonProperty("disk_mount_path")
    val diskMountPath: String? = "/data/landun/workspace"
)

package com.tencent.devops.dispatch.bcs.pojo.bcs

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.dispatch.bcs.pojo.DockerRegistry

/**
 * 创建bcs job需要的类型
 * @param name job名称
 * @param builderName 依附的构建机名称，如果非空，则当前job会共享使用构建机的容器的/data盘； 构建机如果状态异常，则当前job不能创建
 * @param shareDiskMountPath 共享的构建机磁盘，在job上的挂载路径，默认为/data
 * @param deadline 超时时间，默认4800，单位秒
 * @param image 镜像（镜像名:版本)
 * @param registry 镜像仓库信息
 * @param cpu 构建机cpu核数
 * @param memory 构建机内存大小， 256的倍数，比如512M， 1024M， 以M为单位
 * @param disk 构建机磁盘大小，10的倍数，比如50G，60G，以G为单位
 * @param env 构建机环境变量
 * @param command 构建机启动命令
 */
data class BcsJob(
    val name: String,
    @JsonProperty("builder_name")
    val builderName: String?,
    @JsonProperty("share_disk_mount_path")
    val shareDiskMountPath: String? = "/data",
    val deadline: Int? = 4800,
    val image: String,
    val registry: DockerRegistry,
    val cpu: Double,
    val memory: Int,
    val disk: Int,
    val env: Map<String, String>?,
    val command: List<String>?,
    @JsonProperty("work_dir")
    val workDir: String? = null,
    val nfs: List<NfsConfig>? = null
)

package com.tencent.devops.remotedev.pojo.remotedev

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 扩展数据盘相关
 * @param uid 环境id
 * @param size 数据盘扩容大小单位Gi
 */
data class ExpandDiskData(
    val uid: String,
    val size: String
)

/**
 * @param valid 扩容是否合法
 * @param message 信息
 */
data class ExpandDiskValidateResp(
    val valid: Boolean,
    val message: String?,
    val taskId: String?
)

/**
 * 机器上的磁盘信息
 * @param pvcClass 磁盘类型(如hdd/ssd)
 * @param pvcSize 磁盘大小
 * @param pvcName 磁盘唯一名称
 * @param isSystemVolume 是否系统盘
 */
@Schema(title = "机器上的磁盘信息")
data class VmDiskInfo(
    @get:Schema(title = "磁盘类型(如hdd/ssd)")
    val pvcClass: String,
    @get:Schema(title = "磁盘大小")
    val pvcSize: String,
    @get:Schema(title = "磁盘唯一名称")
    val pvcName: String,
    @get:Schema(title = "是否系统盘")
    val isSystemVolume: Boolean
)
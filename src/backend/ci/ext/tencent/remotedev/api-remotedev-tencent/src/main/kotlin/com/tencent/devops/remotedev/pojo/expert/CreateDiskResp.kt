package com.tencent.devops.remotedev.pojo.expert

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "磁盘创建任务详情")
data class CreateDiskResp(
    @get:Schema(title = "创建任务发起是否成功")
    val result: Boolean,
    @get:Schema(title = "创建发起不成功原因")
    val message: String?
)

/**
 * 创建数据盘相关
 * @param uid 环境id
 * @param pvcSize 数据盘扩容大小单位Gi
 * @param pvcClass PVC类型 ssd or hdd
 */
data class CreateDiskData(
    val uid: String,
    val pvcSize: String,
    val pvcClass: String
)

enum class CreateDiskDataClass(val data: String) {
    HDD("hdd")
}
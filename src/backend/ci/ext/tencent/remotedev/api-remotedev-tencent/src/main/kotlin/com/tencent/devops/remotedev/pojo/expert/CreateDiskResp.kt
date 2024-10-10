package com.tencent.devops.remotedev.pojo.expert

data class CreateDiskResp(
    val result: Boolean,
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
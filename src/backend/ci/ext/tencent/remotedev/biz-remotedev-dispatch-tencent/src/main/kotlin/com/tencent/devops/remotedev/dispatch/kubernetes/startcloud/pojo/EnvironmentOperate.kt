package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

abstract class EnvironmentOperateInf(
    open val uid: String
)

data class EnvironmentOperate(
    override val uid: String,
    val userId: String? = null,
    val appName: String? = null,
    val pipelineId: String? = null,
    val env: Map<String, String>? = null,
    val cgsId: String? = null,
    val image: String? = null,
    val zoneId: String? = null,
    val machineType: String? = null,
    val formatDataDisk: Boolean? = null,
    val size: String? = null,
    val live: Boolean? = null,
    val imageName: String? = null
) : EnvironmentOperateInf(uid)

/**
 * 扩容数据盘相关
 * @param uid 环境id
 * @param size 数据盘扩容大小单位Gi
 * @param pvcId PVCID
 */
data class EnvironmentOperateExpandDisk(
    override val uid: String,
    val size: String,
    val pvcId: String?
) : EnvironmentOperateInf(uid)

/**
 * 创建数据盘相关
 * @param uid 环境id
 * @param pvcSize 数据盘扩容大小单位Gi
 * @param pvcClass PVC类型 ssd or hdd
 */
data class EnvironmentOperateCreateDisk(
    override val uid: String,
    val pvcSize: String,
    val pvcClass: String
) : EnvironmentOperateInf(uid)

package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

import com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo.EnvironmentCreateBasicBody.Toleration

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
    val resetDataDisk: Boolean? = null,
    val size: String? = null,
    val live: Boolean? = null,
    val imageName: String? = null,
    val tolerations: List<Toleration>? = null,
    val nodeSelector: Map<String, String>? = null,
    val force: Boolean = false
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
 * @param forceRestart 是否强制重启
 */
data class EnvironmentOperateCreateDisk(
    override val uid: String,
    val pvcSize: String,
    val pvcClass: String,
    val forceRestart: Boolean?
) : EnvironmentOperateInf(uid)

/**
 * 删除数据盘相关
 * @param uid 环境id
 * @param pvcName 磁盘唯一名称
 * @param forceRestart 是否强制重启
 * @param delaySeconds 延迟删除时间，秒
 */
data class EnvironmentOperateDeleteDisk(
    override val uid: String,
    val pvcName: String,
    val forceRestart: Boolean?,
    val delaySeconds: Int?
) : EnvironmentOperateInf(uid)

/**
 * 备份主机相关
 * @param syncOnly 仅同步,不跑初始化和cgs流程
 * @param targetEnvID 目标vm的EnvID
 * @param uid 原vm的EnvID
 */
data class EnvironmentOperateSyncVm(
    val syncOnly: Boolean?,
    val targetEnvID: String,
    override val uid: String
) : EnvironmentOperateInf(uid)

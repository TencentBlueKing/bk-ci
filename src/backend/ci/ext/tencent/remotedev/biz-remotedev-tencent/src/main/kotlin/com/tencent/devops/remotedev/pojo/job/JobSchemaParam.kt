package com.tencent.devops.remotedev.pojo.job

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 用来给job记录做重放时存储
 */
@Suppress("UnnecessaryAbstractClass")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = NotifyRemoteDevDesktopParam::class, name = NotifyRemoteDevDesktopParam.CLASS_TYPE),
    JsonSubTypes.Type(value = CronPowerOnParam::class, name = CronPowerOnParam.CLASS_TYPE),
    JsonSubTypes.Type(value = PipelineParam::class, name = PipelineParam.CLASS_TYPE)
)
abstract class JobSchemaParam(
    open val scope: JobScope,
    open val machineType: String?,
    open val owners: Set<String>?
)

data class NotifyRemoteDevDesktopParam(
    override val scope: JobScope,
    override val machineType: String?,
    override val owners: Set<String>?,
    val title: String,
    val content: String
) : JobSchemaParam(scope, machineType, owners) {
    companion object {
        const val CLASS_TYPE = JobActionType.NOTIFY_REMOTEDEV_DESKTOP_CONST_NAME
    }
}

data class CronPowerOnParam(
    override val scope: JobScope,
    override val machineType: String?,
    override val owners: Set<String>?
) : JobSchemaParam(scope, machineType, owners) {
    companion object {
        const val CLASS_TYPE = JobActionType.CRON_POWER_ON_CONST_NAME
    }
}

data class PipelineParam(
    override val scope: JobScope,
    override val machineType: String?,
    override val owners: Set<String>?,
    // 流水线由谁的权限执行
    val userId: String,
    // 拥有流水线的项目
    val projectId: String,
    val pipelineId: String,
    val variables: Map<String, String>
) : JobSchemaParam(scope, machineType, owners) {
    companion object {
        const val CLASS_TYPE = JobActionType.PIPELINE_CONST_NAME
    }
}

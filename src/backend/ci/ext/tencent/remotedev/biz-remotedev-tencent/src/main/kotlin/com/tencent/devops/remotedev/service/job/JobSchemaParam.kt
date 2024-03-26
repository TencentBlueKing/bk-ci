package com.tencent.devops.remotedev.service.job

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.remotedev.pojo.job.JobActionType
import com.tencent.devops.remotedev.pojo.job.JobScope

/**
 * 用来给job记录做重放时存储
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = NotifyRemoteDevDesktopParam::class, name = NotifyRemoteDevDesktopParam.CLASS_TYPE),
    JsonSubTypes.Type(value = CronPowerOnParam::class, name = CronPowerOnParam.CLASS_TYPE),
    JsonSubTypes.Type(value = PipelineParam::class, name = PipelineParam.CLASS_TYPE)
)
abstract class JobSchemaParam(
    open val scope: JobScope,
    open val machineType: String?,
    open val owners: Set<String>?,
    open val type: JobActionType
)

data class NotifyRemoteDevDesktopParam(
    override val scope: JobScope,
    override val machineType: String?,
    override val owners: Set<String>?,
    override val type: JobActionType,
    val title: String,
    val content: String
) : JobSchemaParam(scope, machineType, owners, type) {
    companion object {
        const val CLASS_TYPE = JobActionType.NOTIFY_REMOTEDEV_DESKTOP_CONST_NAME
    }
}

data class CronPowerOnParam(
    override val scope: JobScope,
    override val machineType: String?,
    override val owners: Set<String>?,
    override val type: JobActionType
) : JobSchemaParam(scope, machineType, owners, type) {
    companion object {
        const val CLASS_TYPE = JobActionType.CRON_POWER_ON_CONST_NAME
    }
}

data class PipelineParam(
    override val scope: JobScope,
    override val machineType: String?,
    override val owners: Set<String>?,
    override val type: JobActionType,
    // 流水线由谁的权限执行
    val userId: String,
    // 拥有流水线的项目
    val projectId: String,
    val pipelineId: String,
    val variables: Map<String, String>
) : JobSchemaParam(scope, machineType, owners, type) {
    companion object {
        const val CLASS_TYPE = JobActionType.PIPELINE_CONST_NAME
    }
}

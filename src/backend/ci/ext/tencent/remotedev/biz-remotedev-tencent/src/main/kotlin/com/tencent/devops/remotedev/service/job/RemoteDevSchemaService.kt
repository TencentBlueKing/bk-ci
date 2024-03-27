package com.tencent.devops.remotedev.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.remotedev.dao.RemoteDevJobSchemaDao
import com.tencent.devops.remotedev.pojo.job.JobActionType
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaCreateData
import com.tencent.devops.remotedev.pojo.job.JobType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevSchemaService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val remoteDevJobSchemaDao: RemoteDevJobSchemaDao
) {
    fun getJobIdAndNames(
        jobType: JobType
    ): List<JobSchema> {
        return remoteDevJobSchemaDao.fetchSchema(dslContext, jobType).map {
            JobSchema(
                jobSchemaId = it.jobId,
                jobSchemaName = it.jobName,
                schema = null,
                jobType = null
            )
        }
    }

    fun getSchema(
        jobId: String
    ): JobSchema? {
        val record = remoteDevJobSchemaDao.getSchema(dslContext, jobId) ?: return null
        return JobSchema(
            jobSchemaId = record.jobId,
            jobSchemaName = record.jobName,
            schema = objectMapper.readValue<Map<String, Any>>(record.jobSchema.data()),
            jobType = JobType.valueOf(record.jobType)
        )
    }

    fun getSchemaList(): List<JobSchema> {
        return remoteDevJobSchemaDao.fetchSchema(dslContext, null).map {
            JobSchema(
                jobSchemaId = it.jobId,
                jobSchemaName = it.jobName,
                schema = objectMapper.readValue<Map<String, Any>>(it.jobSchema.data()),
                jobType = JobType.valueOf(it.jobType)
            )
        }
    }

    fun createOrUpdateSchema(
        data: JobSchemaCreateData
    ) {
        remoteDevJobSchemaDao.createOrUpdateSchema(
            dslContext = dslContext,
            jobId = data.jobId,
            jobName = data.jobName,
            jobSchema = data.jobSchema,
            jobType = data.jobType,
            jobActionType = data.jobActionType,
            jobActionExtraParam = when (data.jobActionType) {
                JobActionType.NOTIFY_REMOTEDEV_DESKTOP -> data.jobNotifyRemoteDevDesktopActionExtraParam!!
                JobActionType.CRON_POWER_ON -> data.jobNotifyCronPowerOnActionExtraParam!!
                JobActionType.PIPELINE -> data.jobPipelineActionExtraParam!!
            }
        )
    }
}

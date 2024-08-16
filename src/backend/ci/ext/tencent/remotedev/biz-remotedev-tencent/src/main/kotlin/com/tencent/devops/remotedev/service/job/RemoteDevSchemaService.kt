package com.tencent.devops.remotedev.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.model.remotedev.tables.records.TRemotedevJobSchemaRecord
import com.tencent.devops.remotedev.dao.RemoteDevJobSchemaDao
import com.tencent.devops.remotedev.pojo.job.JobActionType
import com.tencent.devops.remotedev.pojo.job.JobBackendActionExtraParam
import com.tencent.devops.remotedev.pojo.job.JobPipelineActionExtraParam
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaShort
import com.tencent.devops.remotedev.pojo.job.JobSchemaWithExtra
import com.tencent.devops.remotedev.pojo.job.JobType
import com.tencent.devops.remotedev.pojo.job.OpJobSchemaCreateData
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
    ): List<JobSchemaShort> {
        return remoteDevJobSchemaDao.fetchSchema(dslContext, jobType).map {
            JobSchemaShort(
                jobSchemaId = it.jobId,
                jobSchemaName = it.jobName
            )
        }
    }

    fun getSchema(
        jobId: String,
        withExtra: Boolean
    ): JobSchema? {
        val record = remoteDevJobSchemaDao.getSchema(dslContext, jobId) ?: return null
        if (!withExtra) {
            return JobSchema(
                jobSchemaId = record.jobId,
                jobSchemaName = record.jobName,
                schema = objectMapper.readValue<Map<String, Any>>(record.jobSchema.data()),
                jobType = JobType.valueOf(record.jobType)
            )
        }
        return genJobSchemaWithExtra(record)
    }

    fun getSchemaList(): List<JobSchema> {
        return remoteDevJobSchemaDao.fetchSchema(dslContext, null).map {
            genJobSchemaWithExtra(it)
        }
    }

    private fun genJobSchemaWithExtra(record: TRemotedevJobSchemaRecord): JobSchemaWithExtra {
        return JobSchemaWithExtra(
            jobSchemaId = record.jobId,
            jobSchemaName = record.jobName,
            schema = objectMapper.readValue<Map<String, Any>>(record.jobSchema.data()),
            jobType = JobType.valueOf(record.jobType),
            jobActionType = JobActionType.valueOf(record.jobActionType),
            jobActionExtraParam = when (JobActionType.valueOf(record.jobActionType)) {
                JobActionType.NOTIFY_REMOTEDEV_DESKTOP, JobActionType.CRON_POWER_ON ->
                    objectMapper.readValue<JobBackendActionExtraParam>(record.jobActionExtraParam!!.data())

                JobActionType.PIPELINE ->
                    objectMapper.readValue<JobPipelineActionExtraParam>(record.jobActionExtraParam!!.data())
            }
        )
    }

    fun opCreateOrUpdateSchema(
        data: OpJobSchemaCreateData
    ) {
        val schema = objectMapper.readValue<Map<String, Any>>(data.jobSchema)
        val extraParam = when (data.jobActionType) {
            JobActionType.NOTIFY_REMOTEDEV_DESKTOP, JobActionType.CRON_POWER_ON ->
                objectMapper.readValue<JobBackendActionExtraParam>(data.jobActionParam)

            JobActionType.PIPELINE -> objectMapper.readValue<JobPipelineActionExtraParam>(data.jobActionParam)
        }
        remoteDevJobSchemaDao.createOrUpdateSchema(
            dslContext = dslContext,
            jobId = data.jobId,
            jobName = data.jobName,
            jobSchema = schema,
            jobType = data.jobType,
            jobActionType = data.jobActionType,
            jobActionExtraParam = extraParam
        )
    }

    fun deleteSchema(schemaId: String) {
        remoteDevJobSchemaDao.delete(dslContext = dslContext, schemaId = schemaId)
    }
}

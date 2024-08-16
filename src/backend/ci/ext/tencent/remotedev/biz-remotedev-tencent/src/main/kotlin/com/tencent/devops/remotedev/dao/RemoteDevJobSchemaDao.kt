package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TRemotedevJobSchema
import com.tencent.devops.model.remotedev.tables.records.TRemotedevJobSchemaRecord
import com.tencent.devops.remotedev.pojo.job.JobActionExtraParam
import com.tencent.devops.remotedev.pojo.job.JobActionType
import com.tencent.devops.remotedev.pojo.job.JobType
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository

@Repository
class RemoteDevJobSchemaDao {
    fun fetchSchema(
        dslContext: DSLContext,
        jobType: JobType?
    ): List<TRemotedevJobSchemaRecord> {
        with(TRemotedevJobSchema.T_REMOTEDEV_JOB_SCHEMA) {
            if (jobType == null) {
                return dslContext.selectFrom(this).skipCheck().fetch()
            }
            return dslContext.selectFrom(this).where(JOB_TYPE.eq(jobType.name)).fetch()
        }
    }

    fun getSchema(
        dslContext: DSLContext,
        jobId: String
    ): TRemotedevJobSchemaRecord? {
        with(TRemotedevJobSchema.T_REMOTEDEV_JOB_SCHEMA) {
            return dslContext.selectFrom(this).where(JOB_ID.eq(jobId)).fetchAny()
        }
    }

    fun createOrUpdateSchema(
        dslContext: DSLContext,
        jobId: String,
        jobName: String,
        jobSchema: Map<String, Any>,
        jobType: JobType,
        jobActionType: JobActionType,
        jobActionExtraParam: JobActionExtraParam
    ) {
        with(TRemotedevJobSchema.T_REMOTEDEV_JOB_SCHEMA) {
            dslContext.insertInto(
                this,
                JOB_ID,
                JOB_NAME,
                JOB_SCHEMA,
                JOB_TYPE,
                JOB_ACTION_TYPE,
                JOB_ACTION_EXTRA_PARAM
            ).values(
                jobId,
                jobName,
                JSON.json(JsonUtil.toJson(jobSchema, false)),
                jobType.name,
                jobActionType.name,
                JSON.json(JsonUtil.toJson(jobActionExtraParam, false))
            ).onDuplicateKeyUpdate()
                .set(JOB_NAME, jobName)
                .set(JOB_SCHEMA, JSON.json(JsonUtil.toJson(jobSchema, false)))
                .set(JOB_ACTION_EXTRA_PARAM, JSON.json(JsonUtil.toJson(jobActionExtraParam, false)))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        schemaId: String
    ) {
        with(TRemotedevJobSchema.T_REMOTEDEV_JOB_SCHEMA) {
            dslContext.deleteFrom(this).where(JOB_ID.eq(schemaId)).execute()
        }
    }
}

package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TRemotedevJobSchema
import com.tencent.devops.model.remotedev.tables.records.TRemotedevJobSchemaRecord
import com.tencent.devops.remotedev.pojo.job.JobType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RemoteDevJobSchemaDao {
    fun fetchSchema(
        dslContext: DSLContext,
        jobType: JobType
    ): List<TRemotedevJobSchemaRecord> {
        with(TRemotedevJobSchema.T_REMOTEDEV_JOB_SCHEMA) {
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
}

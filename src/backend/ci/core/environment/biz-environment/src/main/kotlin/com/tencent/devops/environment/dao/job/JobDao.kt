package com.tencent.devops.environment.dao.job

import com.tencent.devops.model.environment.tables.TProjectJob
import com.tencent.devops.model.environment.tables.records.TProjectJobRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JobDao {
    fun getProjIdFromJobInsIdList(
        dslContext: DSLContext,
        projectId: String,
        jobInstanceId: Long
    ): List<TProjectJob> {
        with(TProjectJob.T_JOB_PROJ) {
            return dslContext.selectFrom(this)
                .where(JOB_INSTANCE_ID.eq(jobInstanceId))
                .orderBy(JOB_INSTANCE_ID.desc())
                .fetch()
        }
    }

    fun addJobProjRecord(
        dslContext: DSLContext,
        projectId: String,
        jobInstanceId: Long,
        createUser: String
    ): Int {
        val currentTime = LocalDateTime.now()
        with(TProjectJob.T_JOB_PROJ) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                JOB_INSTANCE_ID,
                CREATED_USER,
                CREATED_TIME
            ).values(
                projectId,
                jobInstanceId,
                createUser,
                currentTime
            ).execute()
        }
    }
}
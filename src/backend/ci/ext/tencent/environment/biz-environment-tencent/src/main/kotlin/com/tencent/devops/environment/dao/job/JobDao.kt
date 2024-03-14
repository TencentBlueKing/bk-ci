package com.tencent.devops.environment.dao.job

import com.tencent.devops.model.environment.tables.TProjectJob
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class JobDao {
    fun isJobInsExist(
        dslContext: DSLContext,
        projectId: String,
        jobInstanceId: Long
    ): Boolean {
        with(TProjectJob.T_PROJECT_JOB) {
            return dslContext.selectCount().from(this)
                .where(JOB_INSTANCE_ID.eq(jobInstanceId))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(JOB_INSTANCE_ID.desc())
                .fetchOne(0, Long::class.java)!! > 0
        }
    }

    fun addJobProjRecord(
        dslContext: DSLContext,
        projectId: String,
        jobInstanceId: Long,
        createUser: String
    ): Int {
        val currentTime = LocalDateTime.now()
        with(TProjectJob.T_PROJECT_JOB) {
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
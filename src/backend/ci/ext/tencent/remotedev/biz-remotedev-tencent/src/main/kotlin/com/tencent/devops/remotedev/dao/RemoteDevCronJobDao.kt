package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TRemotedevCronJob
import com.tencent.devops.model.remotedev.tables.records.TRemotedevCronJobRecord
import com.tencent.devops.remotedev.pojo.job.JobSchemaParam
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RemoteDevCronJobDao {
    fun createCronJob(
        dslContext: DSLContext,
        projectId: String,
        jobName: String,
        creator: String,
        cronExp: String,
        jobSchemaId: String,
        jobSchemaParam: JobSchemaParam
    ) {
        with(TRemotedevCronJob.T_REMOTEDEV_CRON_JOB) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                JOB_NAME,
                CREATOR,
                UPDATER,
                CRON_EXP,
                JOB_SCHEMA_ID,
                JOB_SCHEMA_PARAM,
                ENABLE
            ).values(
                projectId,
                jobName,
                creator,
                creator,
                cronExp,
                jobSchemaId,
                JSON.json(JsonUtil.toJson(jobSchemaParam, false)),
                true
            ).execute()
        }
    }

    fun updateCronJob(
        dslContext: DSLContext,
        id: Long,
        jobName: String,
        updater: String,
        cronExp: String,
        jobSchemaId: String,
        jobSchemaParam: JobSchemaParam
    ) {
        with(TRemotedevCronJob.T_REMOTEDEV_CRON_JOB) {
            dslContext.update(this)
                .set(JOB_NAME, jobName)
                .set(UPDATER, updater)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(CRON_EXP, cronExp)
                .set(JOB_SCHEMA_ID, jobSchemaId)
                .set(JOB_SCHEMA_PARAM, JSON.json(JsonUtil.toJson(jobSchemaParam, false)))
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateRunTime(
        dslContext: DSLContext,
        id: Long
    ) {
        with(TRemotedevCronJob.T_REMOTEDEV_CRON_JOB) {
            dslContext.update(this)
                .set(LAST_RUN_TIME, LocalDateTime.now())
                .set(RUN_TIMES, RUN_TIMES + 1)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun countCronJob(
        dslContext: DSLContext,
        projectId: String
    ): Long {
        with(TRemotedevCronJob.T_REMOTEDEV_CRON_JOB) {
            return dslContext.selectCount().from(this).where(PROJECT_ID.eq(projectId)).fetchOne(0, Long::class.java)!!
        }
    }

    fun fetchCronJon(
        dslContext: DSLContext,
        projectId: String,
        sqlLimit: SQLLimit
    ): List<TRemotedevCronJobRecord> {
        with(TRemotedevCronJob.T_REMOTEDEV_CRON_JOB) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .limit(sqlLimit.limit).offset(sqlLimit.offset)
                .fetch()
        }
    }
}

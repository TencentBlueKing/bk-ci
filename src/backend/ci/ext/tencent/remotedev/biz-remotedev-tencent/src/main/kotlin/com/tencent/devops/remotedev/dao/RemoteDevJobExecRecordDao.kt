package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.remotedev.tables.TRemotedevJobExecRecord
import com.tencent.devops.model.remotedev.tables.records.TRemotedevJobExecRecordRecord
import com.tencent.devops.remotedev.pojo.job.JobReceiptInfo
import com.tencent.devops.remotedev.pojo.job.JobRecordStatus
import com.tencent.devops.remotedev.pojo.job.JobSchemaParam
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RemoteDevJobExecRecordDao {
    fun insertRecord(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        creator: String,
        createTime: LocalDateTime,
        status: JobRecordStatus,
        jobSchemaId: String,
        jobSchemaParam: JobSchemaParam
    ): Long {
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                CREATE_TIME,
                CREATOR,
                STATUS,
                JOB_SCHEMA_ID,
                JOB_SCHEMA_PARAM
            ).values(
                projectId,
                name,
                createTime,
                creator,
                status.name,
                jobSchemaId,
                JSON.json(JsonUtil.toJson(jobSchemaParam, false))
            ).returning(ID)
                .fetchOne()!!.id
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: JobRecordStatus,
        errMsg: String?,
        endTime: LocalDateTime?,
        resetReceipt: Boolean = false
    ) {
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            val dsl = dslContext.update(this)
                .set(STATUS, status.name)
                .set(ERROR_MSG, errMsg)
                .set(END_TIME, endTime)
            if (resetReceipt) {
                dsl.set(RECEIPT_INFO, JSON.valueOf(null))
            }
            dsl.where(ID.eq(id)).execute()
        }
    }

    fun updateReceiptInfo(
        dslContext: DSLContext,
        id: Long,
        info: JobReceiptInfo
    ) {
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            dslContext.update(this)
                .set(RECEIPT_INFO, JSON.json(JsonUtil.toJson(info, false)))
                .where(ID.eq(id))
                .execute()
        }
    }

    fun countRecord(
        dslContext: DSLContext,
        projectId: String,
        creator: String?,
        status: JobRecordStatus?,
        name: String?,
        id: Long?
    ): Long {
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            return dslContext.selectCount().from(this).where(
                genRecordCond(
                    projectId = projectId,
                    creator = creator,
                    status = status,
                    name = name,
                    id = id
                )
            ).fetchOne(0, Long::class.java)!!
        }
    }

    fun fetchRecord(
        dslContext: DSLContext,
        projectId: String,
        sqlLimit: SQLLimit,
        creator: String?,
        status: JobRecordStatus?,
        name: String?,
        id: Long?
    ): List<TRemotedevJobExecRecordRecord> {
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            return dslContext.selectFrom(this).where(
                genRecordCond(
                    projectId = projectId,
                    creator = creator,
                    status = status,
                    name = name,
                    id = id
                )
            ).orderBy(ID.desc()).limit(sqlLimit.limit).offset(sqlLimit.offset).fetch()
        }
    }

    private fun genRecordCond(
        projectId: String,
        creator: String?,
        status: JobRecordStatus?,
        name: String?,
        id: Long?
    ): List<Condition> {
        val conditions = mutableListOf<Condition>()
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            conditions.add(PROJECT_ID.eq(projectId))

            if (id != null) {
                conditions.add(ID.eq(id))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(CREATOR.eq(creator))
            }
            if (status != null) {
                conditions.add(STATUS.eq(status.name))
            }
            if (!name.isNullOrBlank()) {
                conditions.add(NAME.like("%$name%"))
            }
        }
        return conditions
    }

    fun getRecord(
        dslContext: DSLContext,
        id: Long
    ): TRemotedevJobExecRecordRecord? {
        with(TRemotedevJobExecRecord.T_REMOTEDEV_JOB_EXEC_RECORD) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchAny()
        }
    }
}

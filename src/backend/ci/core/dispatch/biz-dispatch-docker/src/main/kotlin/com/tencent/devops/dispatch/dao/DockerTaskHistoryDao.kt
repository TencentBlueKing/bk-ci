package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TIdcTaskHistory
import com.tencent.devops.model.dispatch.tables.records.TIdcTaskHistoryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DockerTaskHistoryDao @Autowired constructor() {
    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        buildId: String,
        vmSeq: String,
        idcIp: String,
        status: Int
    ) {
        with(TIdcTaskHistory.T_IDC_TASK_HISTORY) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ,
                IDC_IP,
                STATUS,
                GMT_CREATE
            ).values(
                pipelineId,
                buildId,
                vmSeq,
                idcIp,
                status,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        buildId: String,
        vmSeq: String,
        status: Int
    ) {
        with(TIdcTaskHistory.T_IDC_TASK_HISTORY) {
            dslContext.update(this)
                .set(STATUS, status)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ.eq(vmSeq))
                .execute()
        }
    }

    fun updateContainerId(
        dslContext: DSLContext,
        buildId: String,
        vmSeq: String,
        containerId: String
    ) {
        with(TIdcTaskHistory.T_IDC_TASK_HISTORY) {
            dslContext.update(this)
                .set(CONTAINER_ID, containerId)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ.eq(vmSeq))
                .execute()
        }
    }

    fun getByBuildIdAndVMSeq(
        dslContext: DSLContext,
        buildId: String,
        vmSeq: String
    ): TIdcTaskHistoryRecord? {
        with(TIdcTaskHistory.T_IDC_TASK_HISTORY) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ.eq(vmSeq))
                .fetchOne()
        }
    }

    fun getByBuildId(
        dslContext: DSLContext,
        buildId: String
    ): Result<TIdcTaskHistoryRecord> {
        with(TIdcTaskHistory.T_IDC_TASK_HISTORY) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetch()
        }
    }
}
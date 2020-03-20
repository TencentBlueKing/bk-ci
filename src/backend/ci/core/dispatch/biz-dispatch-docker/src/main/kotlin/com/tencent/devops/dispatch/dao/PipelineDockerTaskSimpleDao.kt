package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTaskSimple
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTaskSimpleRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerTaskSimpleDao @Autowired constructor() {
    fun create(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        idcIp: String,
        status: Int
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ,
                DOCKER_IP,
                STATUS,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                pipelineId,
                vmSeq,
                idcIp,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        status: Int
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.update(this)
                .set(STATUS, status)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .execute()
        }
    }

    fun updateContainerId(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        containerId: String
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.update(this)
                .set(CONTAINER_ID, containerId)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .execute()
        }
    }

    fun updateDockerIp(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String,
        dockerIp: String
    ) {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            dslContext.update(this)
                .set(DOCKER_IP, dockerIp)
                .set(GMT_MODIFIED, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .execute()
        }
    }

    fun getByPipelineIdAndVMSeq(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeq: String
    ): TDispatchPipelineDockerTaskSimpleRecord? {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ.eq(vmSeq))
                .fetchOne()
        }
    }

    fun getByPipelineId(
        dslContext: DSLContext,
        pipelineId: String
    ): Result<TDispatchPipelineDockerTaskSimpleRecord> {
        with(TDispatchPipelineDockerTaskSimple.T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }
}
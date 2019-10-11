package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.pojo.PipelineBuild
import com.tencent.devops.dispatch.pojo.PipelineBuildCreate
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchPipelineBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineBuildRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineBuildDao {

    fun exist(dslContext: DSLContext, buildId: String, vmSeqId: String): Boolean {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetch().isNotEmpty
        }
    }

    fun add(dslContext: DSLContext, pipelineBuild: PipelineBuildCreate) {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            val now = LocalDateTime.now()
            dslContext.insertInto(this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    VM_SEQ_ID,
                    VM_ID,
                    CREATED_TIME,
                    UPDATED_TIME,
                    STATUS)
                    .values(
                            pipelineBuild.projectId,
                            pipelineBuild.pipelineId,
                            pipelineBuild.buildId,
                            pipelineBuild.vmSeqId,
                            pipelineBuild.vmId,
                            now,
                            now,
                            PipelineTaskStatus.QUEUE.status
                    )
                    .execute()
        }
    }

    fun updatePipelineStatus(dslContext: DSLContext, buildId: String, vmSeqId: String, vmId: Int, status: PipelineTaskStatus): Boolean {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.update(this)
                    .set(VM_ID, vmId)
                    .set(STATUS, status.status)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .where(BUILD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .execute() == 1
        }
    }

    fun updatePipelineStatus(dslContext: DSLContext, id: Int, status: PipelineTaskStatus): Boolean {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.update(this)
                    .set(STATUS, status.status)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute() == 1
        }
    }

    fun listByPipelineAndVmSeqId(dslContext: DSLContext, pipelineId: String, vmSeqId: String, limit: Int): Result<TDispatchPipelineBuildRecord> {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(STATUS.eq(PipelineTaskStatus.DONE.status))
                    .orderBy(CREATED_TIME.desc())
                    .limit(limit)
                    .fetch()
        }
    }

    fun getPipelineByBuildIdOrNull(dslContext: DSLContext, buildId: String, vmSeqId: String?): List<TDispatchPipelineBuildRecord> {
        with(TDispatchPipelineBuild.T_DISPATCH_PIPELINE_BUILD) {
            val context = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))

            if (vmSeqId != null) {
                context.and(VM_SEQ_ID.eq(vmSeqId))
            }
            return context.fetch()
        }
    }

    fun convert(record: TDispatchPipelineBuildRecord): PipelineBuild {
        with(record) {
            return PipelineBuild(
                    projectId,
                    pipelineId,
                    buildId,
                    vmSeqId,
                    vmId,
                    createdTime.timestamp(),
                    status
            )
        }
    }
}
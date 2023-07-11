package com.tencent.devops.dispatch.dao

import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentDockerDebug
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentDockerDebugRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.JSON
import org.springframework.stereotype.Repository

@Repository
class ThirdPartyAgentDockerDebugDao {

    fun add(
        dslContext: DSLContext,
        projectId: String,
        agentId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        userId: String,
        thirdPartyAgentWorkspace: String,
        dockerInfo: JSON
    ): Long {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                AGENT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                USER_ID,
                STATUS,
                CREATED_TIME,
                UPDATED_TIME,
                WORKSPACE,
                DOCKER_INFO
            ).values(
                projectId,
                agentId,
                pipelineId,
                buildId,
                vmSeqId,
                userId,
                PipelineTaskStatus.QUEUE.status,
                now,
                now,
                thirdPartyAgentWorkspace,
                dockerInfo
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun fetchOneQueueBuild(
        dslContext: DSLContext,
        agentId: String
    ): TDispatchThirdpartyAgentDockerDebugRecord? {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            val select = dslContext.selectFrom(this.forceIndex("IDX_AGENTID_STATUS_UPDATE"))
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
            return select
                .orderBy(UPDATED_TIME.asc())
                .limit(1)
                .fetchAny()
        }
    }

    fun updateStatusById(
        dslContext: DSLContext,
        id: Long,
        status: PipelineTaskStatus,
        errMsg: String?
    ) {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            val sql = dslContext.update(this)
            if (!errMsg.isNullOrBlank()) {
                sql.set(ERR_MSG, errMsg)
            }
            sql.set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: PipelineTaskStatus,
        debugUrl: String,
        errMsg: String?
    ) {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(DEBUG_URL, debugUrl)
                .set(ERR_MSG, errMsg)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getDoneDebugById(
        dslContext: DSLContext,
        id: Long
    ): TDispatchThirdpartyAgentDockerDebugRecord? {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .and(STATUS.`in`(PipelineTaskStatus.DONE.status, PipelineTaskStatus.FAILURE.status))
                .fetchAny()
        }
    }

    fun getDebug(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        userId: String?,
        last: Boolean
    ): TDispatchThirdpartyAgentDockerDebugRecord? {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            val sql = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
            if (!userId.isNullOrBlank()) {
                sql.and(USER_ID.eq(userId))
            }
            if (last) {
                sql.orderBy(ID.desc())
            }
            return sql.fetchAny()
        }
    }

    fun getDebugById(
        dslContext: DSLContext,
        id: Long
    ): TDispatchThirdpartyAgentDockerDebugRecord? {
        with(TDispatchThirdpartyAgentDockerDebug.T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchAny()
        }
    }
}

package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceBuildStatus
import com.tencent.devops.model.dispatch.devcloud.tables.TDevcloudPersistenceBuild
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudPersistenceBuildRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DcPersistenceBuildDao {

    fun pushQueueBuild(
        dslContext: DSLContext,
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        executeCount: Int,
        projectId: String,
        containerName: String,
        persistenceAgentId: String,
        status: Int,
        containerHashId: String,
        agentId: String,
        secretKey: String
    ): Int {
        return with(TDevcloudPersistenceBuild.T_DEVCLOUD_PERSISTENCE_BUILD) {
            dslContext.insertInto(
                this,
                USER_ID,
                PIPELINE_ID,
                VM_SEQ_ID,
                PROJECT_ID,
                BUILD_ID,
                EXECUTE_COUNT,
                CONTAINER_NAME,
                PERSISTENCE_AGENT_ID,
                STATUS,
                CONTAINER_HASH_ID,
                AGENT_ID,
                SECRET_KEY,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                userId,
                pipelineId,
                vmSeqId,
                projectId,
                buildId,
                executeCount,
                containerName,
                persistenceAgentId,
                status,
                containerHashId,
                agentId,
                secretKey,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .set(VM_SEQ_ID, vmSeqId)
                .set(BUILD_ID, buildId)
                .set(EXECUTE_COUNT, executeCount)
                .set(PROJECT_ID, projectId)
                .set(CONTAINER_NAME, containerName)
                .set(PERSISTENCE_AGENT_ID, persistenceAgentId)
                .set(STATUS, status)
                .set(CONTAINER_HASH_ID, containerHashId)
                .set(AGENT_ID, agentId)
                .set(SECRET_KEY, secretKey)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: Int
    ) {
        with(TDevcloudPersistenceBuild.T_DEVCLOUD_PERSISTENCE_BUILD) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(STATUS, status)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun fetchOneQueueBuild(
        dslContext: DSLContext,
        persistenceAgentId: String
    ): TDevcloudPersistenceBuildRecord? {
        with(TDevcloudPersistenceBuild.T_DEVCLOUD_PERSISTENCE_BUILD) {
            return dslContext.selectFrom(this)
                .where(PERSISTENCE_AGENT_ID.eq(persistenceAgentId))
                .and(STATUS.eq(PersistenceBuildStatus.QUEUE.status))
                .orderBy(UPDATE_TIME.asc())
                .limit(1)
                .fetchAny()
        }
    }

    fun getPersistenceBuildInfo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int? = null
    ): TDevcloudPersistenceBuildRecord? {
        with(TDevcloudPersistenceBuild.T_DEVCLOUD_PERSISTENCE_BUILD) {
            val sql = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))

            if (executeCount != null) {
                sql.and(VM_SEQ_ID.eq(vmSeqId))
            }

            if (executeCount != null) {
                sql.and(EXECUTE_COUNT.eq(executeCount))
            }

            return sql.fetchAny()
        }
    }
}

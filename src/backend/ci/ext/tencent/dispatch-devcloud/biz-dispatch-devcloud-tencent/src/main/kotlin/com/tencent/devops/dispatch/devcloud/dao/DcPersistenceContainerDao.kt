package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch.devcloud.tables.TDevcloudPersistenceContainer
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudPersistenceContainerRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DcPersistenceContainerDao {

    fun createOrUpdate(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int,
        projectId: String,
        containerName: String,
        persistenceAgentId: String,
        status: Int,
        buildStatus: Int,
        userId: String
    ): Int {
        return with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ_ID,
                POOL_NO,
                PROJECT_ID,
                CONTAINER_NAME,
                PERSISTENCE_AGENT_ID,
                CONTAINER_STATUS,
                BUILD_STATUS,
                CREATE_TIME,
                UPDATE_TIME,
                USER_ID
            ).values(
                pipelineId,
                vmSeqId,
                poolNo,
                projectId,
                containerName,
                persistenceAgentId,
                status,
                buildStatus,
                LocalDateTime.now(),
                LocalDateTime.now(),
                userId
            ).onDuplicateKeyUpdate()
                .set(PROJECT_ID, projectId)
                .set(CONTAINER_NAME, containerName)
                .set(PERSISTENCE_AGENT_ID, persistenceAgentId)
                .set(CONTAINER_STATUS, status)
                .set(BUILD_STATUS, buildStatus)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateContainerStatus(
        dslContext: DSLContext,
        persistenceAgentId: String,
        status: Int
    ) {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(CONTAINER_STATUS, status)
                .where(PERSISTENCE_AGENT_ID.eq(persistenceAgentId))
                .execute()
        }
    }

    fun updateBuildStatus(
        dslContext: DSLContext,
        persistenceAgentId: String,
        status: Int
    ) {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(BUILD_STATUS, status)
                .where(PERSISTENCE_AGENT_ID.eq(persistenceAgentId))
                .execute()
        }
    }

    fun updateContainerName(
        dslContext: DSLContext,
        containerName: String,
        persistenceAgentId: String,
        status: Int? = null
    ) {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            val sql = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(CONTAINER_NAME, containerName)
            if (status != null) {
                sql.set(CONTAINER_STATUS, status)
            }
            sql.where(PERSISTENCE_AGENT_ID.eq(persistenceAgentId))
                .execute()
        }
    }

    fun getByPersistenceAgentId(
        dslContext: DSLContext,
        persistenceAgentId: String
    ): TDevcloudPersistenceContainerRecord? {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(PERSISTENCE_AGENT_ID.eq(persistenceAgentId))
                .fetchAny()
        }
    }

    fun getPersistenceContainer(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        poolNo: Int? = null
    ): Result<TDevcloudPersistenceContainerRecord> {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            val sql = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))

            if (poolNo != null) {
                sql.and(POOL_NO.eq(poolNo))
            }
            return sql.fetch()
        }
    }
}

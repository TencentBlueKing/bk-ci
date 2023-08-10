package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch.devcloud.tables.TDevcloudPersistenceContainer
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudPersistenceContainerRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DcPersistenceContainerDao {

    fun createOrUpdate(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String,
        projectId: String,
        containerName: String,
        status: Int,
        userId: String
    ): Int {
        return with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            dslContext.insertInto(
                this,
                PIPELINE_ID,
                VM_SEQ_ID,
                PROJECT_ID,
                CONTAINER_NAME,
                CONTAINER_STATUS,
                CREATE_TIME,
                UPDATE_TIME,
                USER_ID
            ).values(
                pipelineId,
                vmSeqId,
                projectId,
                containerName,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                userId
            ).onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .set(VM_SEQ_ID, vmSeqId)
                .set(PROJECT_ID, projectId)
                .set(CONTAINER_NAME, containerName)
                .set(CONTAINER_STATUS, status)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        containerName: String,
        status: Int
    ) {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(CONTAINER_STATUS, status)
                .where(CONTAINER_NAME.eq(containerName))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): TDevcloudPersistenceContainerRecord? {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .fetchAny()
        }
    }

    fun getContainerStatus(
        dslContext: DSLContext,
        containerName: String
    ): TDevcloudPersistenceContainerRecord? {
        with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(CONTAINER_NAME.eq(containerName))
                .fetchAny()
        }
    }

    fun delete(
        dslContext: DSLContext,
        containerName: String
    ): Int {
        return with(TDevcloudPersistenceContainer.T_DEVCLOUD_PERSISTENCE_CONTAINER) {
            dslContext.delete(this)
                .where(CONTAINER_NAME.eq(containerName))
                .execute()
        }
    }
}

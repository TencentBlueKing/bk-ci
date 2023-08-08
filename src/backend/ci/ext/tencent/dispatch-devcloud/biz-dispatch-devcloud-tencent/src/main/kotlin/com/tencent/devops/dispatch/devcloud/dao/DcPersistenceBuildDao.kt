package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceTaskStatus
import com.tencent.devops.model.dispatch.devcloud.tables.TDevcloudPersistenceBuild
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudPersistenceBuildRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DcPersistenceBuildDao {

    private val logger = LoggerFactory.getLogger(DcPersistenceBuildDao::class.java)

    fun pushQueueBuild(
        dslContext: DSLContext,
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String,
        projectId: String,
        containerName: String,
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
                CONTAINER_NAME,
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
                containerName,
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
                .set(PROJECT_ID, projectId)
                .set(CONTAINER_NAME, containerName)
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
        containerName: String,
        status: Int
    ) {
        with(TDevcloudPersistenceBuild.T_DEVCLOUD_PERSISTENCE_BUILD) {
            val sql = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(STATUS, status)
                .where(CONTAINER_NAME.eq(containerName))
                .execute()
        }
    }

    fun fetchOneQueueBuild(
        dslContext: DSLContext,
        containerName: String
    ): TDevcloudPersistenceBuildRecord? {
        with(TDevcloudPersistenceBuild.T_DEVCLOUD_PERSISTENCE_BUILD) {
            return dslContext.selectFrom(this)
                .where(CONTAINER_NAME.eq(containerName))
                .and(STATUS.eq(PersistenceTaskStatus.QUEUE.status))
                .orderBy(UPDATE_TIME.asc())
                .limit(1)
                .fetchAny()
        }
    }
}

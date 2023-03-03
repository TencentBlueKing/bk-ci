package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch.devcloud.tables.TDevcloudBuildHis
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildHisRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class DevCloudBuildHisDao {

    private val logger = LoggerFactory.getLogger(DevCloudBuildHisDao::class.java)

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        poolNo: String,
        secretKey: String,
        containerName: String,
        cpu: Int,
        memory: String,
        disk: String,
        executeCount: Int
    ): Long {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            val preRecord = dslContext.selectFrom(this)
                .where(BUIDLD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .fetch()
            if (preRecord != null && preRecord.size > 0) {
                dslContext.deleteFrom(this)
                    .where(BUIDLD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .execute()
                /*return dslContext.update(this)
                    .set(POOL_NO, poolNo)
                    .set(SECRET_KEY, secretKey)
                    .set(CONTAINER_NAME, containerName)
                    .set(CPU, cpu)
                    .set(MEMORY, memory)
                    .set(DISK, disk)
                    .execute().toLong()*/
            }

            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUIDLD_ID,
                VM_SEQ_ID,
                POOL_NO,
                SECRET_KEY,
                CONTAINER_NAME,
                CPU,
                MEMORY,
                DISK,
                EXECUTE_COUNT
            ).values(
                projectId,
                pipelineId,
                buildId,
                vmSeqId,
                poolNo,
                secretKey,
                containerName,
                cpu,
                memory,
                disk,
                executeCount
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun get(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): Result<TDevcloudBuildHisRecord> {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            val select = dslContext.selectFrom(this)
                .where(BUIDLD_ID.eq(buildId))
            if (vmSeqId != null && vmSeqId.isNotEmpty()) {
                select.and(VM_SEQ_ID.eq(vmSeqId))
            }

            return select.fetch()
        }
    }

    fun getLatestBuildHistory(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): TDevcloudBuildHisRecord? {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .orderBy(GMT_CREATE.desc())
                .fetchAny()
        }
    }

    fun updateContainerName(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        containerName: String,
        executeCount: Int
    ) {
        with(TDevcloudBuildHis.T_DEVCLOUD_BUILD_HIS) {
            dslContext.update(this)
                .set(CONTAINER_NAME, containerName)
                .where(BUIDLD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .execute()
        }
    }
}

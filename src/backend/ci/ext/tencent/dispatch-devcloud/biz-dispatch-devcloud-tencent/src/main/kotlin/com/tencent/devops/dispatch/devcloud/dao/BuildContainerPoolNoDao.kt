package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch.devcloud.tables.TBuildContainerPoolNo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildContainerPoolNoDao {

    fun setDevCloudBuildLastContainer(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        containerName: String,
        poolNo: String
    ) {
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                VM_SEQ_ID,
                EXECUTE_COUNT,
                CONTAINER_NAME,
                POOL_NO,
                CREATE_TIME
            ).values(
                buildId,
                vmSeqId,
                executeCount,
                containerName,
                poolNo,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getDevCloudBuildLastContainer(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records?.forEach {
                    result.add(Pair(it.vmSeqId, it.containerName))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()
                result.add(Pair(vmSeqId, record?.containerName))
            }
        }
        return result
    }

    fun getDevCloudBuildLastPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records?.forEach {
                    result.add(Pair(it.vmSeqId, it.poolNo))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()

                result.add(Pair(vmSeqId, record?.poolNo))
            }
        }
        return result
    }

    fun deleteDevCloudBuildLastContainerPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ) {
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            if (vmSeqId == null) {
                dslContext.delete(this)
                    .where(BUILD_ID.eq(buildId))
                    .execute()
            } else {
                dslContext.delete(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .execute()
            }
        }
    }
}

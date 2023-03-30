package com.tencent.devops.dispatch.codecc.dao

import com.tencent.devops.model.dispatch.codecc.tables.TBuildContainerPoolNo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildContainerPoolNoDao {

    fun setDevCloudBuildLastContainer(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        containerName: String,
        poolNo: String
    ) {
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                VM_SEQ_ID,
                CONTAINER_NAME,
                POOL_NO,
                CREATE_TIME
            ).values(
                buildId,
                vmSeqId,
                containerName,
                poolNo,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getDevCloudBuildLastContainer(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                        .where(BUILD_ID.eq(buildId))
                        .fetch()
                records.forEach {
                    result.add(Pair(it.vmSeqId, it.containerName))
                }
            } else {
                val record = dslContext.selectFrom(this)
                        .where(BUILD_ID.eq(buildId))
                        .and(VM_SEQ_ID.eq(vmSeqId))
                        .fetchOne()
                result.add(Pair(record!!.vmSeqId, record.containerName))
            }
        }
        return result
    }

    fun getDevCloudBuildLastPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                        .where(BUILD_ID.eq(buildId))
                        .fetch()
                records.forEach {
                    result.add(Pair(it.vmSeqId, it.poolNo))
                }
            } else {
                val record = dslContext.selectFrom(this)
                        .where(BUILD_ID.eq(buildId))
                        .and(VM_SEQ_ID.eq(vmSeqId))
                        .fetchOne()
                result.add(Pair(record!!.vmSeqId, record.poolNo))
            }
        }
        return result
    }

    fun deleteDevCloudBuildLastContainerPoolNo(dslContext: DSLContext, buildId: String, vmSeqId: String?) {
        with(TBuildContainerPoolNo.T_BUILD_CONTAINER_POOL_NO) {
            if (vmSeqId == null) {
                dslContext.delete(this)
                        .where(BUILD_ID.eq(buildId))
                        .execute()
            } else {
                dslContext.delete(this)
                        .where(BUILD_ID.eq(buildId))
                        .and(VM_SEQ_ID.eq(vmSeqId))
                        .execute()
            }
        }
    }
}

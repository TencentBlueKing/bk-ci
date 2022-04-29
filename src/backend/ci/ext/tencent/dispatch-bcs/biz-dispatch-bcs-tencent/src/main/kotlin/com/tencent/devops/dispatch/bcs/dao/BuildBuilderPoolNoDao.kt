package com.tencent.devops.dispatch.bcs.dao

import com.tencent.devops.model.dispatch_bcs.tables.TBuildBuilderPoolNo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BuildBuilderPoolNoDao {

    fun setBcsBuildLastBuilder(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        builderName: String,
        poolNo: String
    ) {
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
            dslContext.insertInto(
                this,
                BUILD_ID,
                VM_SEQ_ID,
                EXECUTE_COUNT,
                BUILDER_NAME,
                POOL_NO,
                CREATE_TIME
            ).values(
                buildId,
                vmSeqId,
                executeCount,
                builderName,
                poolNo,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun getBcsBuildLastBuilder(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
            if (null == vmSeqId) {
                val records = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .fetch()
                records?.forEach {
                    result.add(Pair(it.vmSeqId, it.builderName))
                }
            } else {
                val record = dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .and(EXECUTE_COUNT.eq(executeCount))
                    .and(VM_SEQ_ID.eq(vmSeqId))
                    .fetchOne()
                result.add(Pair(vmSeqId, record?.builderName))
            }
        }
        return result
    }

    fun getBcsBuildLastPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ): List<Pair<String, String?>> {
        val result = mutableListOf<Pair<String, String?>>()
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
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

    fun deleteBcsBuildLastBuilderPoolNo(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int
    ) {
        with(TBuildBuilderPoolNo.T_BUILD_BUILDER_POOL_NO) {
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

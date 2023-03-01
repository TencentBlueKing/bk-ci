package com.tencent.devops.dispatch.devcloud.dao

import com.tencent.devops.model.dispatch_devcloud.tables.TDevcloudPerformanceConfig
import com.tencent.devops.model.dispatch_devcloud.tables.TDevcloudPerformanceOptions
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record5
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DevcloudPerformanceConfigDao {

    fun createOrUpdate(
        dslContext: DSLContext,
        projectId: String,
        optionId: Long
    ) {
        with(TDevcloudPerformanceConfig.T_DEVCLOUD_PERFORMANCE_CONFIG) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                OPTION_ID,
                GMT_CREATE,
                GMT_MODIFIED
            ).values(
                projectId,
                optionId,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(OPTION_ID, optionId)
                .execute()
        }
    }

    fun getByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): Record? {
        val t1 = TDevcloudPerformanceConfig.T_DEVCLOUD_PERFORMANCE_CONFIG.`as`("t1")
        val t2 = TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS.`as`("t2")
        return dslContext.select(t1.PROJECT_ID, t2.CPU, t2.MEMORY, t2.DISK)
            .from(t1).leftJoin(t2).on(t1.OPTION_ID.eq(t2.ID))
            .where(t1.PROJECT_ID.eq(projectId))
            .fetchOne()
    }

    fun getList(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Result<Record5<String, Int, Int, Int, String>>? {
        val t1 = TDevcloudPerformanceConfig.T_DEVCLOUD_PERFORMANCE_CONFIG.`as`("t1")
        val t2 = TDevcloudPerformanceOptions.T_DEVCLOUD_PERFORMANCE_OPTIONS.`as`("t2")
        return dslContext.select(t1.PROJECT_ID, t2.CPU, t2.MEMORY, t2.DISK, t2.DESCRIPTION)
            .from(t1).leftJoin(t2).on(t1.OPTION_ID.eq(t2.ID))
            .limit(pageSize).offset((page - 1) * pageSize)
            .fetch()
    }

    fun getCount(
        dslContext: DSLContext
    ): Long {
        with(TDevcloudPerformanceConfig.T_DEVCLOUD_PERFORMANCE_CONFIG) {
            return dslContext.selectCount()
                .from(this)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String
    ): Int {
        return with(TDevcloudPerformanceConfig.T_DEVCLOUD_PERFORMANCE_CONFIG) {
            dslContext.delete(this)
                    .where(PROJECT_ID.eq(projectId))
                    .execute()
        }
    }
}

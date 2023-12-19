package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TWindowsSpecResource
import com.tencent.devops.model.remotedev.tables.records.TWindowsSpecResourceRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class WindowsSpecResourceDao {
    fun createOrUpdateSpecRes(
        dslContext: DSLContext,
        projectId: String,
        size: String,
        quota: Int
    ): Boolean {
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                SIZE,
                QUOTA
            ).values(
                projectId,
                size,
                quota
            ).onDuplicateKeyUpdate()
                .set(QUOTA, quota)
                .execute() > 0
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        size: String
    ): Boolean {
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            return dslContext.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(SIZE.eq(size)).execute() > 0
        }
    }

    fun fetchQuota(
        dslContext: DSLContext,
        projectId: String,
        size: String
    ): Int? {
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId)).and(SIZE.eq(size)).fetchAny()?.quota
        }
    }

    fun fetchSpec(
        dslContext: DSLContext,
        sqlLimit: SQLLimit
    ): Result<TWindowsSpecResourceRecord> {
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            return dslContext.selectFrom(this).limit(sqlLimit.limit).offset(sqlLimit.offset).fetch()
        }
    }

    fun fetchSpecCount(
        dslContext: DSLContext
    ): Long {
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            return dslContext.selectCount().from(this).skipCheck().fetchOne(0, Long::class.java)!!
        }
    }
}

package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWindowsSpecResource
import org.jooq.DSLContext
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
}

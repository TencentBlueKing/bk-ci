package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TWindowsSpecResource
import com.tencent.devops.model.remotedev.tables.records.TWindowsSpecResourceRecord
import org.jooq.Condition
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
        projectId: String?,
        machineType: String?,
        dslContext: DSLContext,
        sqlLimit: SQLLimit
    ): Result<TWindowsSpecResourceRecord> {
        val conditions = mutableListOf<Condition>()
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            projectId?.let {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            machineType?.let {
                conditions.add(SIZE.eq(machineType))
            }
            return dslContext.selectFrom(this).where(conditions).limit(sqlLimit.limit).offset(sqlLimit.offset).fetch()
        }
    }

    fun fetchSpecCount(
        projectId: String?,
        machineType: String?,
        dslContext: DSLContext
    ): Long {
        with(TWindowsSpecResource.T_WINDOWS_SPEC_RESOURCE) {
            val conditions = mutableListOf<Condition>()
            projectId?.let {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            machineType?.let {
                conditions.add(SIZE.eq(machineType))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .skipCheck().fetchOne(0, Long::class.java)!!
        }
    }
}

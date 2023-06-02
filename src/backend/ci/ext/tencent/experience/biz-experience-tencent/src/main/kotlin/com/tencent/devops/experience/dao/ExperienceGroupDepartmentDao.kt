package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceGroupDepartment
import com.tencent.devops.model.experience.tables.records.TExperienceGroupDepartmentRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceGroupDepartmentDao {
    @SuppressWarnings("LongParameterList")
    fun create(
        dslContext: DSLContext,
        groupId: Long,
        deptId: String,
        deptLevel: Int,
        deptName: String,
        deptFullName: String,
    ) {
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                GROUP_ID,
                DEPT_ID,
                DEPT_LEVEL,
                DEPT_NAME,
                DEPT_FULL_NAME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                groupId,
                deptId,
                deptLevel,
                deptName,
                deptFullName,
                now,
                now
            ).execute()
        }
    }

    fun listByGroupIds(dslContext: DSLContext, groupIds: Collection<Long>): List<TExperienceGroupDepartmentRecord> {
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            return dslContext.selectFrom(this).where(GROUP_ID.`in`(groupIds)).limit(1000).fetch()
        }
    }

    fun listGroupIdsByDeptIds(dslContext: DSLContext, deptIds: Collection<String>): Result<Record1<Long>> {
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            return dslContext.select(GROUP_ID).from(this).where(DEPT_ID.`in`(deptIds)).limit(1000).fetch()
        }
    }

    fun deleteByDeptIds(dslContext: DSLContext, groupId: Long, deptIds: Collection<String>) {
        if (deptIds.isEmpty()) {
            return
        }
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            dslContext.deleteFrom(this)
                .where(GROUP_ID.eq(groupId))
                .and(DEPT_ID.`in`(deptIds))
                .execute()
        }
    }
}

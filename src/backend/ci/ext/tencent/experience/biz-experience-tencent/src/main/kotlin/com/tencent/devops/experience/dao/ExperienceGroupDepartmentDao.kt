package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperienceGroupDepartment
import com.tencent.devops.model.experience.tables.records.TExperienceGroupDepartmentRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceGroupDepartmentDao {
    fun add(
        dslContext: DSLContext,
        groupId: Long,
        deptId: Int,
        deptLevel: Int,
        deptFullName: String
    ) {
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                GROUP_ID,
                DEPT_ID,
                DEPT_LEVEL,
                DEPT_FULL_NAME,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                groupId,
                deptId,
                deptLevel,
                deptFullName,
                now,
                now
            ).execute()
        }
    }

    fun listAll(dslContext: DSLContext, groupId: Long): List<TExperienceGroupDepartmentRecord> {
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            return dslContext.selectFrom(this).where(GROUP_ID.eq(groupId)).limit(1000).fetch()
        }
    }

    fun delete(dslContext: DSLContext, id: Long) {
        with(TExperienceGroupDepartment.T_EXPERIENCE_GROUP_DEPARTMENT) {
            dslContext.deleteFrom(this).where(ID.eq(id)).execute()
        }
    }
}

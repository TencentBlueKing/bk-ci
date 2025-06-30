package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.DepartmentInfo
import com.tencent.devops.auth.pojo.DepartmentUserCount
import com.tencent.devops.common.db.utils.JooqUtils.count
import com.tencent.devops.model.auth.Tables.T_DEPARTMENT
import com.tencent.devops.model.auth.Tables.T_DEPARTMENT_RELATION
import com.tencent.devops.model.auth.Tables.T_USER_INFO
import com.tencent.devops.model.auth.tables.TDepartment
import com.tencent.devops.model.auth.tables.records.TDepartmentRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DepartmentDao {
    fun create(
        dslContext: DSLContext,
        departmentInfo: DepartmentInfo,
        taskId: String
    ) {
        with(TDepartment.T_DEPARTMENT) {
            dslContext.insertInto(
                this,
                DEPARTMENT_ID,
                DEPARTMENT_NAME,
                PARENT,
                LEVEL,
                HAS_CHILDREN,
                TASK_ID,
                CREATE_TIME
            ).values(
                departmentInfo.departmentId,
                departmentInfo.departmentName,
                departmentInfo.parent,
                departmentInfo.level,
                departmentInfo.hasChildren,
                taskId,
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(DEPARTMENT_NAME, departmentInfo.departmentName)
                .set(PARENT, departmentInfo.parent)
                .set(LEVEL, departmentInfo.level)
                .set(HAS_CHILDREN, departmentInfo.hasChildren)
                .set(TASK_ID, taskId)
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): List<DepartmentInfo> {
        return with(TDepartment.T_DEPARTMENT) {
            dslContext.selectFrom(this)
                .limit(limit).offset(offset)
                .fetch().map {
                    DepartmentInfo(
                        departmentId = it.departmentId,
                        departmentName = it.departmentName,
                        parent = it.parent,
                        level = it.level,
                        hasChildren = it.hasChildren
                    )
                }
        }
    }

    fun get(
        dslContext: DSLContext,
        departmentId: Int
    ): DepartmentInfo? {
        return with(TDepartment.T_DEPARTMENT) {
            dslContext.selectFrom(this)
                .where(DEPARTMENT_ID.eq(departmentId))
                .fetchOne()?.convert()
        }
    }

    fun listDepartmentChildren(
        dslContext: DSLContext,
        parentId: Int
    ): List<Int> {
        return with(TDepartment.T_DEPARTMENT) {
            dslContext.select(DEPARTMENT_ID)
                .from(this)
                .where(PARENT.eq(parentId))
                .fetch().map { it.value1() }
        }
    }

    fun getUserCountOfDepartments(
        dslContext: DSLContext,
        userIds: List<String>,
        departmentIds: List<Int>
    ): List<DepartmentUserCount> {
        val tUser = T_USER_INFO
        val tRel = T_DEPARTMENT_RELATION
        val tDept = T_DEPARTMENT

        return dslContext.select(
            tDept.DEPARTMENT_NAME,
            tRel.PARENT_ID,
            tDept.HAS_CHILDREN,
            count(tUser.USER_ID)
        )
            .from(tUser)
            .join(tRel).on(tUser.DEPARTMENT_ID.eq(tRel.CHILDREN_ID))
            .join(tDept).on(tRel.PARENT_ID.eq(tDept.DEPARTMENT_ID))
            .where(
                tUser.USER_ID.`in`(userIds),
                tRel.PARENT_ID.`in`(departmentIds)
            )
            .groupBy(tDept.DEPARTMENT_NAME, tRel.PARENT_ID)
            .fetch().map {
                DepartmentUserCount(
                    departmentName = it.value1(),
                    departmentId = it.value2(),
                    hasChildren = it.value3(),
                    userCount = it.value4()
                )
            }
    }

    fun deleteByTaskId(
        dslContext: DSLContext,
        taskId: String
    ) {
        with(TDepartment.T_DEPARTMENT) {
            dslContext.deleteFrom(this).where(TASK_ID.notEqual(taskId)).execute()
        }
    }

    fun TDepartmentRecord.convert(): DepartmentInfo {
        return DepartmentInfo(
            departmentId = departmentId,
            departmentName = departmentName,
            parent = parent,
            level = level,
            hasChildren = hasChildren
        )
    }
}

package com.tencent.devops.auth.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.UserInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.auth.tables.TUserAndDepartmentSyncRecord
import com.tencent.devops.model.auth.tables.TUserInfo
import com.tencent.devops.model.auth.tables.records.TUserAndDepartmentSyncRecordRecord
import com.tencent.devops.model.auth.tables.records.TUserInfoRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserInfoDao {
    fun create(
        dslContext: DSLContext,
        userInfo: UserInfo,
        taskId: String
    ) {
        val fullDepartments = userInfo.departments?.let { JsonUtil.toJson(it) }
        val path = userInfo.path?.joinToString("-")
        with(TUserInfo.T_USER_INFO) {
            dslContext.insertInto(
                this,
                USER_ID,
                USER_NAME,
                ENABLED,
                DEPARTMENT_NAME,
                DEPARTMENT_ID,
                FULL_DEPARTMENTS,
                PATH,
                DEPARTED,
                TASK_ID,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                userInfo.userId,
                userInfo.userName,
                userInfo.enabled,
                userInfo.departmentName,
                userInfo.departmentId,
                fullDepartments,
                path,
                userInfo.departed,
                taskId,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(ENABLED, userInfo.enabled)
                .set(DEPARTED, userInfo.departed)
                .set(DEPARTMENT_NAME, userInfo.departmentName)
                .set(DEPARTMENT_ID, userInfo.departmentId)
                .set(FULL_DEPARTMENTS, fullDepartments)
                .set(PATH, path)
                .set(TASK_ID, taskId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateUserDepartedFlag(
        dslContext: DSLContext,
        taskId: String
    ) {
        with(TUserInfo.T_USER_INFO) {
            dslContext.update(this)
                .set(DEPARTED, true)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(TASK_ID.eq(taskId))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        userId: String
    ): UserInfo? {
        return with(TUserInfo.T_USER_INFO) {
            dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .fetchOne()?.convert()
        }
    }

    fun recordSyncDataTask(
        dslContext: DSLContext,
        taskId: String,
        taskType: String
    ) {
        with(TUserAndDepartmentSyncRecord.T_USER_AND_DEPARTMENT_SYNC_RECORD) {
            dslContext.insertInto(
                this,
                TASK_ID,
                TASK_TYPE,
                START_TIME
            ).values(
                taskId,
                taskType,
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(END_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getLatestUserSyncDataRecord(
        dslContext: DSLContext,
        taskType: String
    ): TUserAndDepartmentSyncRecordRecord? {
        return with(TUserAndDepartmentSyncRecord.T_USER_AND_DEPARTMENT_SYNC_RECORD) {
            dslContext.selectFrom(this)
                .where(TASK_TYPE.eq(taskType))
                .orderBy(END_TIME.desc())
                .fetchAny()
        }
    }

    fun TUserInfoRecord.convert(): UserInfo {
        return UserInfo(
            userId = userId,
            userName = userName,
            enabled = enabled,
            departmentId = departmentId,
            departmentName = departmentName,
            departments = fullDepartments?.let {
                JsonUtil.to(it, object : TypeReference<List<BkUserDeptInfo>>() {})
            },
            path = path?.split("-")?.map { it.toInt() },
            departed = departed
        )
    }
}

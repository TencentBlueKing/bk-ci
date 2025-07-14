package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.UserProjectPermission
import com.tencent.devops.model.auth.tables.TAuthMemberProjectPermission
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthUserProjectPermissionDao {
    fun create(
        dslContext: DSLContext,
        records: List<UserProjectPermission>
    ) {
        with(TAuthMemberProjectPermission.T_AUTH_MEMBER_PROJECT_PERMISSION) {
            records.forEach { record ->
                dslContext.insertInto(
                    this,
                    MEMBER_ID,
                    PROJECT_CODE,
                    ACTION,
                    IAM_GROUP_ID,
                    EXPIRED_TIME
                ).values(
                    record.memberId,
                    record.projectCode,
                    record.action,
                    record.iamGroupId,
                    record.expireTime
                ).execute()
            }
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int
    ) {
        with(TAuthMemberProjectPermission.T_AUTH_MEMBER_PROJECT_PERMISSION) {
            dslContext.deleteFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int,
        records: List<UserProjectPermission>
    ) {
        with(TAuthMemberProjectPermission.T_AUTH_MEMBER_PROJECT_PERMISSION) {
            records.forEach {
                dslContext.deleteFrom(this)
                    .where(PROJECT_CODE.eq(projectCode))
                    .and(IAM_GROUP_ID.eq(iamGroupId))
                    .and(ACTION.eq(it.action))
                    .and(MEMBER_ID.eq(it.memberId))
                    .execute()
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int
    ): List<UserProjectPermission> {
        return with(TAuthMemberProjectPermission.T_AUTH_MEMBER_PROJECT_PERMISSION) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .fetch().map {
                    UserProjectPermission(
                        memberId = it.memberId,
                        projectCode = it.projectCode,
                        action = it.action,
                        iamGroupId = it.iamGroupId,
                        expireTime = it.expiredTime
                    )
                }
        }
    }

    fun list(
        dslContext: DSLContext,
        memberIds: List<String>,
        action: String
    ): List<String> {
        return with(TAuthMemberProjectPermission.T_AUTH_MEMBER_PROJECT_PERMISSION) {
            dslContext.select(PROJECT_CODE).from(this)
                .where(MEMBER_ID.`in`(memberIds))
                .and(ACTION.eq(action))
                .and(EXPIRED_TIME.gt(LocalDateTime.now()))
                .groupBy(PROJECT_CODE)
                .fetch().map { it.value1() }
        }
    }
}

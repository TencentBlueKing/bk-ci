package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.enum.ApplyToGroupStatus
import com.tencent.devops.model.auth.tables.TAuthResourceGroupApply
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupApplyRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthResourceGroupApplyDao {
    fun list(
        dslContext: DSLContext,
        limit: Int,
        offset: Int
    ): Result<TAuthResourceGroupApplyRecord> {
        return with(TAuthResourceGroupApply.T_AUTH_RESOURCE_GROUP_APPLY) {
            dslContext.selectFrom(this)
                .where(STATUS.eq(ApplyToGroupStatus.PENDING.value))
                .orderBy(CREATE_TIME.asc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        ids: List<Long>,
        applyToGroupStatus: ApplyToGroupStatus
    ) {
        with(TAuthResourceGroupApply.T_AUTH_RESOURCE_GROUP_APPLY) {
            ids.forEach { id ->
                dslContext.update(this)
                    .set(STATUS, applyToGroupStatus.value)
                    .set(NUMBER_OF_CHECKS, NUMBER_OF_CHECKS + 1)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute()
            }
        }
    }

    fun batchCreate(
        dslContext: DSLContext,
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ) {
        with(TAuthResourceGroupApply.T_AUTH_RESOURCE_GROUP_APPLY) {
            applyJoinGroupInfo.groupIds.forEach { groupId ->
                dslContext.insertInto(this)
                    .set(PROJECT_CODE, applyJoinGroupInfo.projectCode)
                    .set(MEMBER_ID, applyJoinGroupInfo.applicant)
                    .set(IAM_GROUP_ID, groupId)
                    .set(STATUS, ApplyToGroupStatus.PENDING.value)
                    .set(NUMBER_OF_CHECKS, 0)
                    .execute()
            }
        }
    }
}

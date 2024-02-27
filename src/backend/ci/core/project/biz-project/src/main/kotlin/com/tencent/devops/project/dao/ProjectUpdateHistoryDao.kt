package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProjectUpdateHistory
import com.tencent.devops.model.project.tables.records.TProjectUpdateHistoryRecord
import com.tencent.devops.project.pojo.ProjectUpdateHistoryInfo
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectUpdateHistoryDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        projectUpdateHistoryInfo: ProjectUpdateHistoryInfo
    ) {
        with(TProjectUpdateHistory.T_PROJECT_UPDATE_HISTORY) {
            dslContext.insertInto(
                this,
                ENGLISH_NAME,
                BEFORE_PROJECT_NAME,
                AFTER_PROJECT_NAME,
                BEFORE_PRODUCT_ID,
                AFTER_PRODUCT_ID,
                BEFORE_ORGANIZATION,
                AFTER_ORGANIZATION,
                BEFORE_SUBJECT_SCOPES,
                AFTER_SUBJECT_SCOPES,
                OPERATOR,
                APPROVAL_STATUS,
                UPDATED_AT
            ).values(
                projectUpdateHistoryInfo.englishName,
                projectUpdateHistoryInfo.beforeProjectName,
                projectUpdateHistoryInfo.afterProjectName,
                projectUpdateHistoryInfo.beforeProductId,
                projectUpdateHistoryInfo.afterProductId,
                projectUpdateHistoryInfo.beforeOrganization,
                projectUpdateHistoryInfo.afterOrganization,
                projectUpdateHistoryInfo.beforeSubjectScopes,
                projectUpdateHistoryInfo.afterSubjectScopes,
                projectUpdateHistoryInfo.operator,
                projectUpdateHistoryInfo.approvalStatus,
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(BEFORE_PROJECT_NAME, projectUpdateHistoryInfo.beforeProjectName)
                .set(AFTER_PROJECT_NAME, projectUpdateHistoryInfo.afterProjectName)
                .set(BEFORE_PRODUCT_ID, projectUpdateHistoryInfo.beforeProductId)
                .set(AFTER_PRODUCT_ID, projectUpdateHistoryInfo.afterProductId)
                .set(BEFORE_ORGANIZATION, projectUpdateHistoryInfo.beforeOrganization)
                .set(AFTER_ORGANIZATION, projectUpdateHistoryInfo.afterOrganization)
                .set(BEFORE_SUBJECT_SCOPES, projectUpdateHistoryInfo.beforeSubjectScopes)
                .set(AFTER_SUBJECT_SCOPES, projectUpdateHistoryInfo.afterSubjectScopes)
                .set(OPERATOR, projectUpdateHistoryInfo.operator)
                .set(APPROVAL_STATUS, projectUpdateHistoryInfo.approvalStatus)
                .set(UPDATED_AT, LocalDateTime.now())
                .execute()
        }
    }

    fun updateProjectHistoryStatus(
        dslContext: DSLContext,
        approvalStatus: Int,
        englishName: String
    ) {
        with(TProjectUpdateHistory.T_PROJECT_UPDATE_HISTORY) {
            dslContext.update(this)
                .set(APPROVAL_STATUS, approvalStatus)
                .where(ENGLISH_NAME.eq(englishName)).execute()
        }
    }

    fun listByProductIdChange(
        dslContext: DSLContext
    ): Result<TProjectUpdateHistoryRecord> {
        val currentTime = LocalDateTime.now()
        val twentyFourHoursAgo = currentTime.minusHours(24)
        return with(TProjectUpdateHistory.T_PROJECT_UPDATE_HISTORY) {
            dslContext.selectFrom(this)
                .where(BEFORE_PRODUCT_ID.ne(AFTER_PRODUCT_ID))
                .and(UPDATED_AT.between(twentyFourHoursAgo, currentTime))
                .and(APPROVAL_STATUS.eq(ProjectApproveStatus.APPROVED.status))
                .fetch()
        }
    }

    fun listByOrganizationChange(
        dslContext: DSLContext
    ): Result<TProjectUpdateHistoryRecord> {
        val currentTime = LocalDateTime.now()
        val twentyFourHoursAgo = currentTime.minusHours(24)
        return with(TProjectUpdateHistory.T_PROJECT_UPDATE_HISTORY) {
            dslContext.selectFrom(this)
                .where(BEFORE_ORGANIZATION.ne(AFTER_ORGANIZATION))
                .and(UPDATED_AT.between(twentyFourHoursAgo, currentTime))
                .and(APPROVAL_STATUS.eq(ProjectApproveStatus.APPROVED.status))
                .fetch()
        }
    }
}

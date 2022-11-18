package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProjectApprovalCallback
import com.tencent.devops.model.project.tables.records.TProjectApprovalCallbackRecord
import com.tencent.devops.project.pojo.enums.ApproveType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class ProjectApprovalCallbackDao {
    fun create(
        dslContext: DSLContext,
        applicant: String,
        englishName: String,
        callbackId: String,
        sn: String,
        subjectScopes: String,
        approveType: Int? = null
    ): Int {
        with(TProjectApprovalCallback.T_PROJECT_APPROVAL_CALLBACK) {
            return dslContext.insertInto(
                this,
                APPLICANT,
                ENGLISH_NAME,
                CALLBACK_ID,
                SN,
                SUBJECT_SCOPES,
                APPROVE_TYPE
            ).values(
                applicant,
                englishName,
                callbackId,
                sn,
                subjectScopes,
                approveType ?: ApproveType.CREATE_APPROVE.type
            ).execute()
        }
    }

    fun getCallbackBySn(
        dslContext: DSLContext,
        sn: String
    ): TProjectApprovalCallbackRecord? {
        with(TProjectApprovalCallback.T_PROJECT_APPROVAL_CALLBACK){
            return dslContext.selectFrom(this).where(SN.eq(sn)).fetchAny()
        }
    }

    fun getCallbackByEnglishName(
        dslContext: DSLContext,
        projectCode: String
    ): TProjectApprovalCallbackRecord? {
        with(TProjectApprovalCallback.T_PROJECT_APPROVAL_CALLBACK) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.eq(projectCode)).fetchAny()
        }
    }
}

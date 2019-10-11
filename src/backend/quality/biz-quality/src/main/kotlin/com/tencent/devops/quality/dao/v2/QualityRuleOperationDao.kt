package com.tencent.devops.quality.dao.v2

import com.tencent.devops.model.quality.tables.TQualityRuleOperation
import com.tencent.devops.model.quality.tables.records.TQualityRuleOperationRecord
import com.tencent.devops.quality.pojo.enum.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class QualityRuleOperationDao {
    fun saveEndOperation(dslContext: DSLContext, ruleId: Long, notifyUserList: List<String>, notifyGroupList: List<Long>, notifyTypeList: List<NotifyType>) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.insertInto(this,
                    this.TYPE,
                    this.NOTIFY_USER,
                    this.NOTIFY_GROUP_ID,
                    this.NOTIFY_TYPES,
                    this.RULE_ID)
                    .values(
                            RuleOperation.END.name,
                            notifyUserList.joinToString(","),
                            notifyGroupList.joinToString(","),
                            notifyTypeList.joinToString(","),
                            ruleId
                    )
                    .execute()
        }
    }

    fun saveAuditOperation(dslContext: DSLContext, ruleId: Long, auditUserList: List<String>, auditTimeoutMinutes: Int) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.insertInto(this,
                    this.TYPE,
                    this.AUDIT_USER,
                    this.AUDIT_TIMEOUT,
                    this.RULE_ID)
                    .values(
                            RuleOperation.AUDIT.name,
                            auditUserList.joinToString(","),
                            auditTimeoutMinutes,
                            ruleId
                    )
                    .execute()
        }
    }

    fun updateEndOperation(dslContext: DSLContext, ruleId: Long, notifyUserList: List<String>, notifyGroupList: List<Long>, notifyTypeList: List<NotifyType>) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.update(this)
                    .set(this.NOTIFY_USER, notifyUserList.joinToString(","))
                    .set(this.NOTIFY_GROUP_ID, notifyGroupList.joinToString(","))
                    .set(this.NOTIFY_TYPES, notifyTypeList.joinToString(","))
                    .set(this.TYPE, RuleOperation.END.name)
                    .where(RULE_ID.eq(ruleId))
                    .execute()
        }
    }

    fun updateAuditOperation(dslContext: DSLContext, ruleId: Long, auditUserList: List<String>, auditTimeoutMinutes: Int) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.update(this)
                    .set(this.AUDIT_USER, auditUserList.joinToString(","))
                    .set(this.AUDIT_TIMEOUT, auditTimeoutMinutes)
                    .set(this.TYPE, RuleOperation.AUDIT.name)
                    .where(RULE_ID.eq(ruleId))
                    .execute()
        }
    }

    fun get(dslContext: DSLContext, ruleId: Long): TQualityRuleOperationRecord {
        return with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.selectFrom(this)
                    .where(RULE_ID.eq(ruleId))
                    .fetchOne()
        }
    }
}
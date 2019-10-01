package com.tencent.devops.quality.service.v2

import com.tencent.devops.model.quality.tables.records.TQualityRuleOperationRecord
import com.tencent.devops.quality.dao.v2.QualityRuleOperationDao
import com.tencent.devops.quality.pojo.enum.NotifyType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityRuleOperationService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleOperationDao: QualityRuleOperationDao
) {
    fun serviceSaveEndOperation(ruleId: Long, notifyUserList: List<String>, notifyGroupList: List<Long>, notifyTypeList: List<NotifyType>) {
        ruleOperationDao.saveEndOperation(dslContext, ruleId, notifyUserList, notifyGroupList, notifyTypeList)
    }

    fun serviceSaveAuditOperation(ruleId: Long, auditUserList: List<String>, auditTimeoutMinutes: Int) {
        ruleOperationDao.saveAuditOperation(dslContext, ruleId, auditUserList, auditTimeoutMinutes)
    }

    fun serviceUpdateEndOperation(ruleId: Long, notifyUserList: List<String>, notifyGroupList: List<Long>, notifyTypeList: List<NotifyType>) {
        ruleOperationDao.updateEndOperation(dslContext, ruleId, notifyUserList, notifyGroupList, notifyTypeList)
    }

    fun serviceUpdateAuditOperation(ruleId: Long, auditUserList: List<String>, auditTimeoutMinutes: Int) {
        ruleOperationDao.updateAuditOperation(dslContext, ruleId, auditUserList, auditTimeoutMinutes)
    }

    fun serviceGet(dslContext: DSLContext, ruleId: Long): TQualityRuleOperationRecord {
        return ruleOperationDao.get(dslContext, ruleId)
    }
}
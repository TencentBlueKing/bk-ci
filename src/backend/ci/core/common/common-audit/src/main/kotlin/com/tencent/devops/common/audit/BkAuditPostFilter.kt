package com.tencent.devops.common.audit

import com.tencent.bk.audit.filter.AuditPostFilter
import com.tencent.bk.audit.model.AuditEvent

class BkAuditPostFilter : AuditPostFilter {
    override fun map(auditEvent: AuditEvent): AuditEvent {
        auditEvent.scopeType = "project"
        return auditEvent
    }
}

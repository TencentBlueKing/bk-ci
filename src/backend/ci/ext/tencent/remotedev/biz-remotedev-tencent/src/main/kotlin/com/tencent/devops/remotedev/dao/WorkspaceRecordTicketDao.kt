package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TWorkspaceRecordTicket
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecordTicketRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WorkspaceRecordTicketDao {
    fun create(dslContext: DSLContext, workspaceName: String, cert: String) {
        with(TWorkspaceRecordTicket.T_WORKSPACE_RECORD_TICKET) {
            dslContext.insertInto(this, WORKSPACE_NAME, CERT).values(workspaceName, cert)
                .onDuplicateKeyIgnore().execute()
        }
    }

    fun fetchAny(dslContext: DSLContext, workspaceName: String): TWorkspaceRecordTicketRecord? {
        with(TWorkspaceRecordTicket.T_WORKSPACE_RECORD_TICKET) {
            return dslContext.selectFrom(this).where(WORKSPACE_NAME.eq(workspaceName)).fetchAny()
        }
    }
}
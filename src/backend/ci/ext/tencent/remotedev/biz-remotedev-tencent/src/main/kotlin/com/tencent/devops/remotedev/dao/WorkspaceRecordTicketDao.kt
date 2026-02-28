package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.remotedev.tables.TWorkspaceRecordTicket
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecordTicketRecord
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordTicketType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class WorkspaceRecordTicketDao {
    fun create(
        dslContext: DSLContext,
        workspaceName: String,
        cert: String,
        type: WorkspaceRecordTicketType,
        enable: Boolean = true
    ) {
        with(TWorkspaceRecordTicket.T_WORKSPACE_RECORD_TICKET) {
            dslContext.insertInto(this, WORKSPACE_NAME, CERT, TYPE, ENABLE)
                .values(workspaceName, cert, type.name, ByteUtils.bool2Byte(enable))
                .onDuplicateKeyIgnore().execute()
        }
    }

    fun fetchAny(
        dslContext: DSLContext,
        workspaceName: String,
        type: WorkspaceRecordTicketType,
        enable: Boolean? = null
    ): TWorkspaceRecordTicketRecord? {
        with(TWorkspaceRecordTicket.T_WORKSPACE_RECORD_TICKET) {
            val query = dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(TYPE.eq(type.name))
            return if (enable != null) {
                query.and(ENABLE.eq(ByteUtils.bool2Byte(enable))).fetchAny()
            } else {
                query.fetchAny()
            }
        }
    }

    fun updateEnable(
        dslContext: DSLContext,
        workspaceName: String,
        type: WorkspaceRecordTicketType,
        enable: Boolean
    ): Int {
        with(TWorkspaceRecordTicket.T_WORKSPACE_RECORD_TICKET) {
            return dslContext.update(this)
                .set(ENABLE, ByteUtils.bool2Byte(enable))
                .where(WORKSPACE_NAME.eq(workspaceName))
                .and(TYPE.eq(type.name))
                .execute()
        }
    }
}

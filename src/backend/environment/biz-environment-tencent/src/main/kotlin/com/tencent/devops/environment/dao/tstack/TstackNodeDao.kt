package com.tencent.devops.environment.dao.tstack

import com.tencent.devops.model.environment.tables.TTstackNode
import com.tencent.devops.model.environment.tables.records.TTstackNodeRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TstackNodeDao {
    fun insertVm(
        dslContext: DSLContext,
        tstackVmId: String,
        vmIp: String,
        vmName: String,
        vmOs: String,
        vmOsVersion: String,
        vmCpu: String,
        vmMemory: String,
        available: Boolean
    ): Long {
        with(TTstackNode.T_TSTACK_NODE) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(this,
                    TSTACK_VM_ID,
                    VM_IP,
                    VM_NAME,
                    VM_OS,
                    VM_OS_VERSION,
                    VM_CPU,
                    VM_MEMORY,
                    AVAILABLE,
                    CREATED_TIME,
                    UPDATED_TIME)
                    .values(
                            tstackVmId,
                            vmIp,
                            vmName,
                            vmOs,
                            vmOsVersion,
                            vmCpu,
                            vmMemory,
                            available,
                            now,
                            now
                    )
                    .returning(ID)
                    .fetchOne().id.toLong()
        }
    }

    fun deleteVm(dslContext: DSLContext, tstackVmId: String) {
        // todo
    }

    fun getOrNull(dslContext: DSLContext, projectId: String, id: Long): TTstackNodeRecord? {
        with(TTstackNode.T_TSTACK_NODE) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .and(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun getByNodeId(dslContext: DSLContext, projectId: String, nodeId: Long): TTstackNodeRecord? {
        with(TTstackNode.T_TSTACK_NODE) {
            return dslContext.selectFrom(this)
                    .where(NODE_ID.eq(nodeId))
                    .and(PROJECT_ID.eq(projectId))
                    .fetchOne()
        }
    }

    fun getOrNull(dslContext: DSLContext, id: Long): TTstackNodeRecord? {
        with(TTstackNode.T_TSTACK_NODE) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }

    fun updateAvailable(dslContext: DSLContext, id: Long, available: Boolean) {
        with(TTstackNode.T_TSTACK_NODE) {
            dslContext.update(this)
                    .set(AVAILABLE, available)
                    .set(UPDATED_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun setNodeIdAndProjectId(dslContext: DSLContext, id: Long, nodeId: Long, projectId: String) {
        with(TTstackNode.T_TSTACK_NODE) {
            dslContext.update(this)
                    .set(NODE_ID, nodeId)
                    .set(PROJECT_ID, projectId)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun cleanNodeIdAndProjectId(dslContext: DSLContext, id: Long) {
        with(TTstackNode.T_TSTACK_NODE) {
            dslContext.update(this)
                    .set(NODE_ID, null as Long?)
                    .set(PROJECT_ID, null as String?)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    fun listAvailableVm(dslContext: DSLContext, projectId: String): Result<TTstackNodeRecord> {
        with(TTstackNode.T_TSTACK_NODE) {
            return dslContext.selectFrom(this)
                    .where(AVAILABLE.eq(true))
                    .and(PROJECT_ID.eq(projectId))
                    .fetch()
        }
    }
}
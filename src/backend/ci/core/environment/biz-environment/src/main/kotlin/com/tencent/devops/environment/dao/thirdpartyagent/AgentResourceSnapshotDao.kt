package com.tencent.devops.environment.dao.thirdpartyagent

import com.tencent.devops.environment.pojo.AgentResourceSnapshot
import com.tencent.devops.model.environment.tables.TAgentResourceSnapshot.T_AGENT_RESOURCE_SNAPSHOT
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AgentResourceSnapshotDao {
    fun save(context: DSLContext, projectId: String, agentId: Long, value: AgentResourceSnapshot) {
        with(T_AGENT_RESOURCE_SNAPSHOT) {
            val old = context.selectFrom(this).where(AGENT_ID.eq(agentId)).fetchOne()
            val watermark = old?.validAfter ?: 0L
            fun newer(time: Long?, previous: Long?) = time != null && time > watermark && time > (previous ?: 0L)
            val cpuNew = newer(value.cpuTime, old?.cpuTime)
            val memoryNew = newer(value.memoryTime, old?.memoryTime)
            val diskNew = newer(value.diskTime, old?.diskTime)
            if (!cpuNew && !memoryNew && !diskNew) return
            val row = context.newRecord(this)
            row.agentId = agentId
            row.projectId = projectId
            row.cpuIdle = if (cpuNew) value.cpu else old?.cpuIdle
            row.cpuTime = if (cpuNew) value.cpuTime else old?.cpuTime
            row.memoryAvailable = if (memoryNew) value.memory else old?.memoryAvailable
            row.memoryTime = if (memoryNew) value.memoryTime else old?.memoryTime
            row.diskFree = if (diskNew) value.disk else old?.diskFree
            row.diskTime = if (diskNew) value.diskTime else old?.diskTime
            row.validAfter = watermark
            row.updatedTime = System.currentTimeMillis()
            context.insertInto(this).set(row).onDuplicateKeyUpdate().set(row).execute()
        }
    }

    fun list(context: DSLContext, agentIds: Collection<Long>): Map<Long, AgentResourceSnapshot> {
        if (agentIds.isEmpty()) return emptyMap()
        return agentIds.chunked(500).flatMap { ids ->
            with(T_AGENT_RESOURCE_SNAPSHOT) {
                context.selectFrom(this).where(AGENT_ID.`in`(ids))
                    .and(UPDATED_TIME.ge(System.currentTimeMillis() - AgentResourceSnapshot.MAX_AGE_MILLIS)).fetch()
            }
        }.associate { row ->
            row.agentId to AgentResourceSnapshot(
                row.cpuIdle, row.cpuTime, row.memoryAvailable, row.memoryTime, row.diskFree, row.diskTime
            )
        }
    }

    companion object {
        /** Called after locking/updating the agent row, in the SAME transaction as the status change. */
        fun invalidate(context: DSLContext, projectId: String, ids: Collection<Long>) {
            val now = System.currentTimeMillis()
            with(T_AGENT_RESOURCE_SNAPSHOT) {
                context.batched { configuration ->
                    ids.forEach { id ->
                        configuration.dsl().insertInto(this)
                            .set(AGENT_ID, id).set(PROJECT_ID, projectId).set(VALID_AFTER, now).set(UPDATED_TIME, now)
                            .onDuplicateKeyUpdate()
                            .set(CPU_IDLE, null as Double?).set(CPU_TIME, null as Long?)
                            .set(MEMORY_AVAILABLE, null as Double?).set(MEMORY_TIME, null as Long?)
                            .set(DISK_FREE, null as Double?).set(DISK_TIME, null as Long?)
                            .set(VALID_AFTER, now).set(UPDATED_TIME, now).execute()
                    }
                }
            }
        }

        fun delete(context: DSLContext, projectId: String, ids: Collection<Long>) {
            if (ids.isEmpty()) return
            with(T_AGENT_RESOURCE_SNAPSHOT) {
                context.deleteFrom(this).where(PROJECT_ID.eq(projectId)).and(AGENT_ID.`in`(ids)).execute()
            }
        }
    }
}

package com.tencent.devops.dispatch.macos.dao

import com.tencent.devops.model.dispatch.macos.tables.TDebugHistory
import com.tencent.devops.model.dispatch.macos.tables.records.TDebugHistoryRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DebugHistoryDao {

    companion object {
        private const val STATUS_DEBUGGING = "DEBUGGING"
        private const val STATUS_STOPPED = "STOPPED"
    }

    /**
     * 保存调试记录
     * @param dslContext DSL上下文
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     * @param vmSeqId 虚拟机序列ID
     * @param executeCount 执行次数
     * @param taskId 调试使用的taskId
     * @param newCreatedVm 是否是新创建的VM
     * @param userId 用户ID
     * @return 记录ID
     */
    fun saveDebugHistory(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        taskId: String,
        newCreatedVm: Boolean,
        userId: String
    ): Long {
        with(TDebugHistory.T_DEBUG_HISTORY) {
            return dslContext.insertInto(this)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(BUILD_ID, buildId)
                .set(VM_SEQ_ID, vmSeqId)
                .set(EXECUTE_COUNT, executeCount)
                .set(TASK_ID, taskId)
                .set(NEW_CREATED_VM, newCreatedVm)
                .set(USER_ID, userId)
                .set(CREATE_TIME, LocalDateTime.now())
                .set(STATUS, STATUS_DEBUGGING)
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    /**
     * 根据流水线ID和虚拟机序列ID查询最新的调试中记录
     * @param dslContext DSL上下文
     * @param pipelineId 流水线ID
     * @param vmSeqId 虚拟机序列ID
     * @return 调试记录，不存在返回null
     */
    fun getLatestDebuggingRecord(
        dslContext: DSLContext,
        pipelineId: String,
        vmSeqId: String
    ): TDebugHistoryRecord? {
        with(TDebugHistory.T_DEBUG_HISTORY) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(STATUS.eq(STATUS_DEBUGGING))
                .orderBy(CREATE_TIME.desc())
                .fetchAny()
        }
    }

    /**
     * 根据构建ID和虚拟机序列ID查询最新的调试中记录
     * @param dslContext DSL上下文
     * @param buildId 构建ID
     * @param vmSeqId 虚拟机序列ID
     * @param executeCount 执行次数
     * @return 调试记录，不存在返回null
     */
    fun getDebuggingRecord(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): TDebugHistoryRecord? {
        with(TDebugHistory.T_DEBUG_HISTORY) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(EXECUTE_COUNT.eq(executeCount))
                .and(STATUS.eq(STATUS_DEBUGGING))
                .orderBy(CREATE_TIME.desc())
                .fetchAny()
        }
    }

    /**
     * 更新调试记录状态为已停止
     * @param dslContext DSL上下文
     * @param id 记录ID
     * @return 是否更新成功
     */
    fun updateStatusToStopped(
        dslContext: DSLContext,
        id: Long
    ): Boolean {
        with(TDebugHistory.T_DEBUG_HISTORY) {
            return dslContext.update(this)
                .set(STATUS, STATUS_STOPPED)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute() > 0
        }
    }

    /**
     * 查询超过指定小时数仍处于DEBUGGING状态的记录
     * @param dslContext DSL上下文
     * @param timeoutHours 超时小时数
     * @return 超时的调试记录列表
     */
    fun listTimeoutDebuggingRecords(
        dslContext: DSLContext,
        timeoutHours: Long
    ): List<TDebugHistoryRecord> {
        with(TDebugHistory.T_DEBUG_HISTORY) {
            return dslContext.selectFrom(this)
                .where(STATUS.eq(STATUS_DEBUGGING))
                .and(CREATE_TIME.lessThan(LocalDateTime.now().minusHours(timeoutHours)))
                .and(CREATE_TIME.greaterThan(LocalDateTime.now().minusDays(7)))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }
}

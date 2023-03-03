package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatcher.macos.dao.BuildHistoryDao
import com.tencent.devops.dispatcher.macos.dao.BuildTaskDao
import com.tencent.devops.dispatcher.macos.dao.MacHostMachineDao
import com.tencent.devops.dispatcher.macos.dao.VirtualMachineDao
import com.tencent.devops.dispatcher.macos.dao.VirtualMachineTypeDao
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.model.dispatcher.macos.tables.records.TBuildHistoryRecord
import com.tencent.devops.model.dispatcher.macos.tables.records.TBuildTaskRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
open class BuildHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val virtualMachineDao: VirtualMachineDao,
    private val macHostMachineDao: MacHostMachineDao,
    private val virtualMachineTypeDao: VirtualMachineTypeDao,
    private val buildHistoryDao: BuildHistoryDao,
    private val buildTaskDao: BuildTaskDao,
    private val passwordHelper: PasswordHelper
) {

    private val logger = LoggerFactory.getLogger(BuildHistoryService::class.java)

    fun saveBuildHistory(
        dispatchMessage: DispatchMessage,
        vmIp: String,
        vmId: Int,
        resourceType: String = ""
    ) {
        val rec = TBuildHistoryRecord()
        rec.projectId = dispatchMessage.projectId
        rec.pipelineId = dispatchMessage.pipelineId
        rec.buildId = dispatchMessage.buildId
        rec.vmSeqId = dispatchMessage.vmSeqId
        rec.vmIp = vmIp
        rec.vmId = vmId
        rec.startTime = LocalDateTime.now()
        rec.status = MacJobStatus.Running.name
        rec.resourceType = resourceType
        rec.containerHashId = dispatchMessage.containerHashId
        rec.executeCount = dispatchMessage.executeCount
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val resultBuildHistory = buildHistoryDao.saveBuildHistory(context, rec)
            if (resultBuildHistory > 0) {
                val buildHistoryRecord = buildHistoryDao.getByBuildIdAndVmSeqId(context, dispatchMessage.buildId, dispatchMessage.vmSeqId)
                val buildTaskRecord = TBuildTaskRecord()
                buildTaskRecord.projectId = dispatchMessage.projectId
                buildTaskRecord.pipelineId = dispatchMessage.pipelineId
                buildTaskRecord.buildId = dispatchMessage.buildId
                buildTaskRecord.vmSeqId = dispatchMessage.vmSeqId
                buildTaskRecord.vmIp = vmIp
                buildTaskRecord.vmId = vmId
                buildTaskRecord.buildHistoryId = buildHistoryRecord.id
                buildTaskRecord.resourceType = resourceType
                buildTaskRecord.containerHashId = dispatchMessage.containerHashId
                buildTaskRecord.executeCount = dispatchMessage.executeCount
                logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] save buildHistoryId: ${buildHistoryRecord.id}, " +
                        "vmIp: $vmIp")
                val taskId = buildTaskDao.save(context, buildTaskRecord)
            } else {
                logger.error("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] fail to save buildTask.")
                throw RuntimeException("Insert into build history table failed.")
            }
        }
    }

    fun findBuildVmIps(event: PipelineAgentShutdownEvent): Result<TBuildHistoryRecord>? {
        return buildHistoryDao.findByBuildIdAndVmSeqId(dslContext, event.buildId, event.vmSeqId)
    }

    fun endBuild(status: MacJobStatus, buildHistoryId: Long, buildTaskId: Long) {
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val taskResult = buildTaskDao.deleteById(context, buildTaskId)
            if (!taskResult) {
                throw RuntimeException("Fail to delete build task,buildTaskId=$buildTaskId.")
            }
            logger.info("success delete build task,buildTaskId=$buildTaskId")
            val historyResult = buildHistoryDao.endStatus(context, status.name, buildHistoryId)
            if (!historyResult) {
                throw RuntimeException("Fail to end build history,buildHistoryId=$buildHistoryId.")
            }
            logger.info("success end build history,buildHistoryId=$buildHistoryId.")
        }
    }

    fun listRunningBuld(vmIp: String): Result<TBuildHistoryRecord> {
        return buildHistoryDao.findRunningBuilds(dslContext, vmIp)
    }

    fun listCancelHistory(): Result<TBuildHistoryRecord> {
        return buildHistoryDao.listCancelHistory(dslContext)
    }
}

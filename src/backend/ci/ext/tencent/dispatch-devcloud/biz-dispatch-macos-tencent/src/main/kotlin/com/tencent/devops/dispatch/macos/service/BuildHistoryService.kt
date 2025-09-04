package com.tencent.devops.dispatch.macos.service

import com.tencent.devops.dispatch.macos.dao.BuildHistoryDao
import com.tencent.devops.dispatch.macos.dao.BuildTaskDao
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildHistoryRecord
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BuildHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildHistoryDao: BuildHistoryDao,
    private val buildTaskDao: BuildTaskDao
) {

    private val logger = LoggerFactory.getLogger(BuildHistoryService::class.java)

    fun saveBuildHistory(
        event: PipelineAgentStartupEvent,
        resourceType: String = "DEVCLOUD"
    ): Long {
        val buildHistory = buildHistoryDao.getBuildHistory(
            dslContext = dslContext,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            executeCount = event.executeCount ?: 1
        )

        // 检查此任务是否已消费,如已有消费记录且非主动发起的event重试，则返回存储异常标志
        if (buildHistory?.isNotEmpty == true && (event.retryTime ?: 0) <= 1) {
            logger.error("$event has been consumed.")
            return -1L
        }

        val rec = TBuildHistoryRecord()
        rec.projectId = event.projectId
        rec.pipelineId = event.pipelineId
        rec.buildId = event.buildId
        rec.vmSeqId = event.vmSeqId
        rec.vmIp = ""
        rec.vmId = 0
        rec.startTime = LocalDateTime.now()
        rec.status = MacJobStatus.Running.name
        rec.resourceType = resourceType
        rec.containerHashId = event.containerHashId
        rec.executeCount = event.executeCount

        return buildHistoryDao.saveBuildHistory(dslContext, rec)
    }

    fun saveBuildTask(
        vmIp: String,
        vmId: Int,
        buildHistoryId: Long,
        event: PipelineAgentStartupEvent
    ) {
        val buildTaskRecord = TBuildTaskRecord()
        buildTaskRecord.projectId = event.projectId
        buildTaskRecord.pipelineId = event.pipelineId
        buildTaskRecord.buildId = event.buildId
        buildTaskRecord.vmSeqId = event.vmSeqId
        buildTaskRecord.vmIp = vmIp
        buildTaskRecord.vmId = vmId
        buildTaskRecord.buildHistoryId = buildHistoryId
        buildTaskRecord.resourceType = "DEVCLOUD"
        buildTaskRecord.containerHashId = event.containerHashId
        buildTaskRecord.executeCount = event.executeCount
        logger.info("[${event.buildId}]|[${event.vmSeqId}] save buildTask vmIp: $vmIp")

        // 插入构建运行时，shutdown时依赖这个记录
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            buildTaskDao.save(context, buildTaskRecord)
            buildHistoryDao.updateVmIP(vmIp, vmId, buildHistoryId, context)
        }
    }

    fun endBuild(status: MacJobStatus, buildHistoryId: Long, buildTaskId: Long) {
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            buildTaskDao.deleteById(context, buildTaskId)
            buildHistoryDao.updateBuildHistoryStatus(context, status.name, buildHistoryId)
            logger.info("Success end build, buildHistoryId=$buildHistoryId, buildTaskId: $buildTaskId")
        }
    }
}

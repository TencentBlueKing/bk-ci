package com.tencent.devops.dispatch.macos.service

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.macos.dao.BuildHistoryDao
import com.tencent.devops.dispatch.macos.dao.BuildTaskDao
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildHistoryRecord
import com.tencent.devops.model.dispatch.macos.tables.records.TBuildTaskRecord
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
        dispatchMessage: DispatchMessage,
        resourceType: String = "DEVCLOUD"
    ): Long {
        // 检查此任务是否已消费
        val buildHistory = buildHistoryDao.getBuildHistory(
            dslContext = dslContext,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            executeCount = dispatchMessage.executeCount ?: 1
        )

        if (buildHistory != null && buildHistory.isNotEmpty) {
            logger.error("$dispatchMessage has been consumed.")
            return -1L
        }

        val rec = TBuildHistoryRecord()
        rec.projectId = dispatchMessage.projectId
        rec.pipelineId = dispatchMessage.pipelineId
        rec.buildId = dispatchMessage.buildId
        rec.vmSeqId = dispatchMessage.vmSeqId
        rec.vmIp = ""
        rec.vmId = 0
        rec.startTime = LocalDateTime.now()
        rec.status = MacJobStatus.Running.name
        rec.resourceType = resourceType
        rec.containerHashId = dispatchMessage.containerHashId
        rec.executeCount = dispatchMessage.executeCount

        return buildHistoryDao.saveBuildHistory(dslContext, rec)
    }

    fun saveBuildTask(
        vmIp: String,
        vmId: Int,
        buildHistoryId: Long,
        dispatchMessage: DispatchMessage
    ) {
        val buildTaskRecord = TBuildTaskRecord()
        buildTaskRecord.projectId = dispatchMessage.projectId
        buildTaskRecord.pipelineId = dispatchMessage.pipelineId
        buildTaskRecord.buildId = dispatchMessage.buildId
        buildTaskRecord.vmSeqId = dispatchMessage.vmSeqId
        buildTaskRecord.vmIp = vmIp
        buildTaskRecord.vmId = vmId
        buildTaskRecord.buildHistoryId = buildHistoryId
        buildTaskRecord.resourceType = "DEVCLOUD"
        buildTaskRecord.containerHashId = dispatchMessage.containerHashId
        buildTaskRecord.executeCount = dispatchMessage.executeCount
        logger.info("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] " +
                        "save buildTask vmIp: $vmIp")

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

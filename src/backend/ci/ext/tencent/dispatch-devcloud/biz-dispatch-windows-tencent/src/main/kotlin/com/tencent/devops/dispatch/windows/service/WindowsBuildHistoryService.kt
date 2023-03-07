package com.tencent.devops.dispatch.windows.service

import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.dispatch.windows.dao.WindowsBuildHistoryDao
import com.tencent.devops.dispatch.windows.enums.WindowsJobStatus
import com.tencent.devops.dispatch.windows.pojo.DevCloudWindowsCreateInfo
import com.tencent.devops.model.dispatch.windows.tables.records.TBuildHistoryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WindowsBuildHistoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val windowsBuildHistoryDao: WindowsBuildHistoryDao,
) {

    private val logger = LoggerFactory.getLogger(WindowsBuildHistoryService::class.java)

    fun getByBuildIdAndVmSeqId(
        buildId: String,
        vmSeqId: String?
    ): Result<TBuildHistoryRecord>? {
        var buildRecord = windowsBuildHistoryDao.findByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId)
        // 如果构建记录为空，可能是因为取消时分配构建IP接口还未完成，等待30s
        if (buildRecord.isNullOrEmpty()) {
            Thread.sleep(30000)
            buildRecord = windowsBuildHistoryDao.findByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId)
        }

        return buildRecord
    }

    // 保存构建历史
    fun saveBuildHistory(
        dispatchMessage: DispatchMessage,
        createInfo: DevCloudWindowsCreateInfo,
        resourceType: String = ""
    ) {
        val rec = TBuildHistoryRecord()
        rec.projectId = dispatchMessage.projectId
        rec.pipelineId = dispatchMessage.pipelineId
        rec.buildId = dispatchMessage.buildId
        rec.vmSeqId = dispatchMessage.vmSeqId
        rec.vmIp = createInfo.ip
        rec.startTime = LocalDateTime.now()
        rec.status = WindowsJobStatus.Running.name
        rec.resourceType = resourceType
        rec.taskGuid = createInfo.taskGuid

        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val resultBuildHistory = windowsBuildHistoryDao.saveBuildHistory(context, rec)
            if (resultBuildHistory > 0) {
                val buildHistoryRecord =
                    windowsBuildHistoryDao.getByBuildIdAndVmSeqId(context, dispatchMessage.buildId, dispatchMessage.vmSeqId)
                logger.info(
                    "[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] save " +
                        "buildHistoryId: ${buildHistoryRecord.id}, vmIp: $createInfo.ip"
                )
            } else {
                logger.error("[${dispatchMessage.buildId}]|[${dispatchMessage.vmSeqId}] fail to save buildTask.")
                throw RuntimeException("Insert into build history table failed.")
            }
        }
    }

    fun endBuild(status: WindowsJobStatus, buildHistoryId: Long) {
        dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            logger.info("success delete build task,buildHistoryId=$buildHistoryId")
            val historyResult = windowsBuildHistoryDao.endStatus(context, status.name, buildHistoryId)
            if (!historyResult) {
                throw RuntimeException("Fail to end build history,buildHistoryId=$buildHistoryId.")
            }
            logger.info("success end build history,buildHistoryId=$buildHistoryId.")
        }
    }

}

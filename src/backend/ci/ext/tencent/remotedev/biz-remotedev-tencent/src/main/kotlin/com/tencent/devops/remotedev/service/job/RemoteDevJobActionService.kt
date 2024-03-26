package com.tencent.devops.remotedev.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.remotedev.dao.RemoteDevCronJobDao
import com.tencent.devops.remotedev.dao.RemoteDevJobExecRecordDao
import com.tencent.devops.remotedev.dao.RemoteDevJobSchemaDao
import com.tencent.devops.remotedev.pojo.job.JobRecordStatus
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RemoteDevJobActionService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val remoteDevJobSchemaDao: RemoteDevJobSchemaDao,
    private val remoteDevJobExecRecordDao: RemoteDevJobExecRecordDao,
    private val remoteDevCronJobDao: RemoteDevCronJobDao
) {

    // 一键通知云桌面
    fun notifyRemoteDevDesktop(id: Long, param: NotifyRemoteDevDesktopParam) {
        // TODO: 执行过程，暂时先按成功算走流程
        remoteDevJobExecRecordDao.updateStatus(
            dslContext = dslContext,
            id = id,
            status = JobRecordStatus.SUCCESS,
            errMsg = null,
            endTime = LocalDateTime.now()
        )
    }

    // 定时开机
    fun cronPowerOn(cron: String, param: CronPowerOnParam) {
        // TODO: 注册定时任务
    }

    // 执行流水线任务
    fun startPipeline(id: Long, param: PipelineParam) {
        // 将ID加到启动参数中，方便流水线执行完后回写
        val mVars = param.variables.toMutableMap()
        mVars[PIPELINE_JOB_CALLBACK_ID] = id.toString()

        val res = try {
            client.get(ServiceBuildResource::class).manualStartupNew(
                userId = param.userId,
                projectId = param.projectId,
                pipelineId = param.pipelineId,
                values = mVars,
                channelCode = ChannelCode.BS,
                buildNo = null,
                startType = StartType.SERVICE
            )
        } catch (e: Exception) {
            remoteDevJobExecRecordDao.updateStatus(
                dslContext = dslContext,
                id = id,
                status = JobRecordStatus.FAIL,
                errMsg = "start pipeline error $e",
                endTime = LocalDateTime.now()
            )
            return
        }
        // 流水线任务需要再执行后更新对应的流水线信息
        remoteDevJobExecRecordDao.updateReceiptInfo(
            dslContext = dslContext,
            id = id,
            info = PipelineJobReceiptInfo(res.data?.id ?: "", res.data?.num)
        )
    }

    companion object {
        private const val PIPELINE_JOB_CALLBACK_ID = "REMOTEDEV_PIPELINE_JOB_CALLBACK_ID"
    }
}

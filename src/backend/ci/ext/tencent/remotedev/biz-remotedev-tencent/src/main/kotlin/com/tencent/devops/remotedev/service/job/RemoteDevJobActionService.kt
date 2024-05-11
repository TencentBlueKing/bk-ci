package com.tencent.devops.remotedev.service.job

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.RemoteDevJobExecRecordDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.async.AsyncJobPipeline
import com.tencent.devops.remotedev.pojo.job.CronPowerOnParam
import com.tencent.devops.remotedev.pojo.job.JobRecordStatus
import com.tencent.devops.remotedev.pojo.job.JobSchemaParam
import com.tencent.devops.remotedev.pojo.job.JobScope
import com.tencent.devops.remotedev.pojo.job.NotifyRemoteDevDesktopParam
import com.tencent.devops.remotedev.pojo.job.PipelineJobReceiptInfo
import com.tencent.devops.remotedev.pojo.job.PipelineParam
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class RemoteDevJobActionService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val remoteDevJobExecRecordDao: RemoteDevJobExecRecordDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val rabbitTemplate: RabbitTemplate
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

    fun startPipeline(projectId: String, id: Long, param: PipelineParam) {
        AsyncExecute.dispatch(rabbitTemplate, AsyncJobPipeline(projectId, id, param))
    }

    // 执行流水线任务，异步
    fun doStartPipeline(projectId: String, id: Long, param: PipelineParam) {
        val ips = fetchIpByJobScope(projectId, param)
        // 将ID加到启动参数中，方便流水线执行完后回写
        val mVars = param.variables.toMutableMap()
        mVars[PIPELINE_JOB_CALLBACK_ID] = id.toString()
        mVars[PIPELINE_JOB_IPS] = ips.joinToString(",") { it.substringAfter(".") }

        logger.info("remotedev start pipeline job $id ${param.pipelineId}")
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

    // 根据JobScope拿到真正需要执行的Ip
    private fun fetchIpByJobScope(projectId: String, param: JobSchemaParam): Set<String> {
        return when (param.scope) {
            JobScope.ALL -> {
                workspaceJoinDao.fetchIp(dslContext, projectId, null, null)
            }

            JobScope.OWNER -> {
                workspaceJoinDao.fetchIp(dslContext, projectId, null, param.owners)
            }

            JobScope.MACHINE_TYPE -> {
                workspaceJoinDao.fetchIp(dslContext, projectId, param.machineType, null)
            }
        }
    }

    companion object {
        private const val PIPELINE_JOB_CALLBACK_ID = "REMOTEDEV_PIPELINE_JOB_CALLBACK_ID"
        private const val PIPELINE_JOB_IPS = "REMOTEDEV_PIPELINE_JOB_IPS"
        private val logger = LoggerFactory.getLogger(RemoteDevJobActionService::class.java)
    }
}

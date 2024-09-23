package com.tencent.devops.remotedev.service.job

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.RemoteDevJobExecRecordDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.async.AsyncJobPipeline
import com.tencent.devops.remotedev.pojo.job.CronPowerOnParam
import com.tencent.devops.remotedev.pojo.job.JobPipelineDetail
import com.tencent.devops.remotedev.pojo.job.JobPipelineSopDetail
import com.tencent.devops.remotedev.pojo.job.JobRecordStatus
import com.tencent.devops.remotedev.pojo.job.JobSchemaParam
import com.tencent.devops.remotedev.pojo.job.JobScope
import com.tencent.devops.remotedev.pojo.job.NotifyRemoteDevDesktopParam
import com.tencent.devops.remotedev.pojo.job.PipelineJobReceiptInfo
import com.tencent.devops.remotedev.pojo.job.PipelineParam
import org.jooq.DSLContext
import org.jooq.JSON
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Base64

@Service
class RemoteDevJobActionService @Autowired constructor(
    private val objectMapper: ObjectMapper,
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
            val errMsg = "start pipeline error $e"
            remoteDevJobExecRecordDao.updateStatus(
                dslContext = dslContext,
                id = id,
                status = JobRecordStatus.FAIL,
                errMsg = if (errMsg.length >= 255) {
                    errMsg.substring(0, 255)
                } else {
                    errMsg
                },
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
                workspaceJoinDao.fetchRunningIp(dslContext, projectId, null, null)
            }

            JobScope.OWNER -> {
                workspaceJoinDao.fetchRunningIp(dslContext, projectId, null, param.owners)
            }

            JobScope.MACHINE_TYPE -> {
                workspaceJoinDao.fetchRunningIp(dslContext, projectId, param.machineType, null)
            }
        }
    }

    @Suppress("NestedBlockDepth")
    fun fetchPipelineJobDetail(param: PipelineParam, receiptInfo: JSON): JobPipelineDetail? {
        val receipt = objectMapper.readValue<PipelineJobReceiptInfo>(receiptInfo.data())
        val detail = JobPipelineDetail(null)
        PIPELINE_JOB_DETAIL_OUTPUT_SUBTAG_KEYS.forEach { k ->
            val logs = client.get(ServiceLogResource::class).getInitLogs(
                userId = param.userId,
                projectId = param.projectId,
                pipelineId = param.pipelineId,
                buildId = receipt.buildId,
                tag = null,
                containerHashId = null,
                executeCount = null,
                subTag = k,
                jobId = null,
                stepId = null
            ).data?.logs ?: return@forEach
            when (k) {
                PIPELINE_JOB_DETAIL_SOP_OUTPUT_SUBTAG_KEY -> {
                    // 同一个 step 的日志可能存在太长被截断的情况，需要在这里拼接
                    val sopStrMap = mutableMapOf<String, MutableList<String>>()
                    logs.forEach { log ->
                        if (sopStrMap.containsKey(log.tag)) {
                            sopStrMap[log.tag]?.add(log.message)
                        } else {
                            sopStrMap[log.tag] = mutableListOf(log.message)
                        }
                    }
                    val sops = mutableListOf<JobPipelineSopDetail>()
                    sopStrMap.values.forEach { v ->
                        kotlin.runCatching {
                            sops.add(
                                objectMapper.readValue<JobPipelineSopDetail>(
                                    Base64.getDecoder().decode(v.joinToString(""))
                                )
                            )
                        }.onFailure {
                            logger.error("fetchPipelineJobDetail decode sop error", it)
                        }
                    }
                    detail.sopData = sops
                }

                else -> return@forEach
            }
        }

        return detail
    }

    companion object {
        private const val PIPELINE_JOB_CALLBACK_ID = "REMOTEDEV_PIPELINE_JOB_CALLBACK_ID"
        private const val PIPELINE_JOB_IPS = "REMOTEDEV_PIPELINE_JOB_IPS"
        private val logger = LoggerFactory.getLogger(RemoteDevJobActionService::class.java)
        private const val PIPELINE_JOB_DETAIL_SOP_OUTPUT_SUBTAG_KEY = "SOP_OUTPUT"
        private val PIPELINE_JOB_DETAIL_OUTPUT_SUBTAG_KEYS = setOf(PIPELINE_JOB_DETAIL_SOP_OUTPUT_SUBTAG_KEY)
    }
}

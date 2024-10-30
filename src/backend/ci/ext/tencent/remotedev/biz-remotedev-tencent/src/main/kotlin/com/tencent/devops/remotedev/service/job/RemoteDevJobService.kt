package com.tencent.devops.remotedev.service.job

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.async.AsyncExecute
import com.tencent.devops.remotedev.dao.RemoteDevCronJobDao
import com.tencent.devops.remotedev.dao.RemoteDevJobExecRecordDao
import com.tencent.devops.remotedev.dao.RemoteDevJobSchemaDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.async.AsyncJobEndEvent
import com.tencent.devops.remotedev.pojo.job.CronJob
import com.tencent.devops.remotedev.pojo.job.CronJobSearchParam
import com.tencent.devops.remotedev.pojo.job.CronPowerOnParam
import com.tencent.devops.remotedev.pojo.job.JobActionType
import com.tencent.devops.remotedev.pojo.job.JobBackendActionExtraParam
import com.tencent.devops.remotedev.pojo.job.JobCreateData
import com.tencent.devops.remotedev.pojo.job.JobDetail
import com.tencent.devops.remotedev.pojo.job.JobPipelineActionExtraParam
import com.tencent.devops.remotedev.pojo.job.JobRecord
import com.tencent.devops.remotedev.pojo.job.JobRecordSearchParam
import com.tencent.devops.remotedev.pojo.job.JobRecordStatus
import com.tencent.devops.remotedev.pojo.job.JobSchemaParam
import com.tencent.devops.remotedev.pojo.job.KeyMapDataType
import com.tencent.devops.remotedev.pojo.job.NotifyRemoteDevDesktopParam
import com.tencent.devops.remotedev.pojo.job.PipelineJobReceiptInfo
import com.tencent.devops.remotedev.pojo.job.PipelineParam
import com.tencent.devops.remotedev.service.PermissionService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class RemoteDevJobService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val remoteDevJobSchemaDao: RemoteDevJobSchemaDao,
    private val remoteDevJobExecRecordDao: RemoteDevJobExecRecordDao,
    private val remoteDevCronJobDao: RemoteDevCronJobDao,
    private val remoteDevActionService: RemoteDevJobActionService,
    private val workspaceDao: WorkspaceDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val streamBridge: StreamBridge,
    private val permissionService: PermissionService
) {
    fun getMachineTypes(projectId: String): Set<String> {
        return workspaceJoinDao.fetchProjectMachineType(dslContext, projectId)
    }

    fun getOwners(projectId: String): Set<String> {
        return workspaceDao.fetchWorkspaceOwnerInProject(
            dslContext = dslContext,
            projectId = projectId
        ).filter { it.isNotBlank() }.toSet()
    }

    fun createJob(
        userId: String,
        data: JobCreateData
    ) {
        val record = remoteDevJobSchemaDao.getSchema(dslContext, data.jobSchemaId) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
            params = arrayOf(I18nUtil.getCodeLanMessage(REMOTEDEV_JOB_NOT_FOUND, params = arrayOf(data.jobSchemaId)))
        )

        // 根据不同的执行类型区分参数解析
        val actionType = JobActionType.fromStr(record.jobActionType) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
            params = arrayOf(
                I18nUtil.getCodeLanMessage(
                    REMOTEDEV_ACTION_NOT_FOUND,
                    params = arrayOf(record.jobActionType)
                )
            )
        )

        val now = LocalDateTime.now()
        when (actionType) {
            JobActionType.NOTIFY_REMOTEDEV_DESKTOP -> {
                val exParam = JsonUtil.to(
                    record.jobActionExtraParam.data(),
                    object : TypeReference<JobBackendActionExtraParam>() {}
                )

                val title = data.schemaValue[exParam.keyMap["title"]?.name ?: throw keyNotFound("title")].toString()
                val content =
                    data.schemaValue[exParam.keyMap["content"]?.name ?: throw keyNotFound("content")].toString()
                val param = NotifyRemoteDevDesktopParam(
                    scope = data.jobScope,
                    machineType = data.machineType,
                    owners = data.owners,
                    title = title,
                    content = content
                )
                val id = remoteDevJobExecRecordDao.insertRecord(
                    dslContext = dslContext,
                    projectId = data.projectId,
                    name = "$ONCE_JOB_RECORD_NAME_PREFIX${record.jobName}_${jobNameFormatter.format(now)}",
                    creator = userId,
                    createTime = now,
                    status = JobRecordStatus.RUNNING,
                    jobSchemaId = record.jobId,
                    jobSchemaParam = param
                )
                remoteDevActionService.notifyRemoteDevDesktop(id, param)
            }

            JobActionType.CRON_POWER_ON -> {
                val exParam = JsonUtil.to(
                    record.jobActionExtraParam.data(),
                    object : TypeReference<JobBackendActionExtraParam>() {}
                )
                val cron = data.schemaValue[exParam.keyMap["cron"]?.name ?: throw keyNotFound("cron")].toString()
                val param = CronPowerOnParam(
                    scope = data.jobScope,
                    machineType = data.machineType,
                    owners = data.owners
                )
                remoteDevCronJobDao.createCronJob(
                    dslContext = dslContext,
                    projectId = data.projectId,
                    jobName = data.cronJobName
                        ?: "$CRON_JOB_RECORD_NAME_PREFIX${record.jobName}_${jobNameFormatter.format(now)}",
                    creator = userId,
                    cronExp = cron,
                    jobSchemaId = record.jobId,
                    jobSchemaParam = param
                )
                remoteDevActionService.cronPowerOn(cron, param)
            }

            JobActionType.PIPELINE -> {
                val exParam = JsonUtil.to(
                    record.jobActionExtraParam.data(),
                    object : TypeReference<JobPipelineActionExtraParam>() {}
                )
                val vars = mutableMapOf<String, String>()
                exParam.keyMap.forEach { (key, param) ->
                    val va = data.schemaValue[param.name]
                    if (va == null) {
                        if (param.required) {
                            throw keyNotFound(param.name)
                        }
                        vars[key] = ""
                        return@forEach
                    }
                    vars[key] = if (param.type == KeyMapDataType.ARRAY) {
                        transValue<List<String>>(va, param.type).joinToString(",")
                    } else {
                        va.toString()
                    }
                }

                val param = PipelineParam(
                    scope = data.jobScope,
                    machineType = data.machineType,
                    owners = data.owners,
                    userId = exParam.userId,
                    projectId = exParam.projectId,
                    pipelineId = exParam.pipelineId,
                    variables = vars
                )
                val id = remoteDevJobExecRecordDao.insertRecord(
                    dslContext = dslContext,
                    projectId = data.projectId,
                    name = "$ONCE_JOB_RECORD_NAME_PREFIX${record.jobName}_${jobNameFormatter.format(now)}",
                    creator = userId,
                    createTime = now,
                    status = JobRecordStatus.RUNNING,
                    jobSchemaId = record.jobId,
                    jobSchemaParam = param
                )
                remoteDevActionService.startPipeline(data.projectId, id, param)
            }
        }
    }

    fun fetchJobRecord(
        searchParam: JobRecordSearchParam
    ): Page<JobRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(searchParam.page, searchParam.pageSize)
        val count = remoteDevJobExecRecordDao.countRecord(
            dslContext,
            searchParam.projectId,
            searchParam.creator,
            searchParam.status,
            searchParam.name,
            searchParam.id
        )
        if (count == 0L) {
            return Page(
                page = searchParam.page,
                pageSize = searchParam.pageSize,
                count = count,
                records = emptyList()
            )
        }
        val records = remoteDevJobExecRecordDao.fetchRecord(
            dslContext,
            searchParam.projectId,
            sqlLimit,
            searchParam.creator,
            searchParam.status,
            searchParam.name,
            searchParam.id
        ).map {
            JobRecord(
                id = it.id,
                jobName = it.name,
                creator = it.creator,
                status = JobRecordStatus.valueOf(it.status),
                startTime = it.createTime,
                endTime = it.endTime
            )
        }
        return Page(
            page = searchParam.page,
            pageSize = searchParam.pageSize,
            count = count,
            records = records
        )
    }

    fun fetchCronJob(
        search: CronJobSearchParam
    ): Page<CronJob> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(search.page, search.pageSize)
        val count = remoteDevCronJobDao.countCronJob(dslContext, search.projectId)
        if (count == 0L) {
            return Page(search.page, search.pageSize, count, emptyList())
        }
        val records = remoteDevCronJobDao.fetchCronJon(dslContext, search.projectId, sqlLimit).map {
            CronJob(
                id = it.id,
                jobName = it.jobName,
                cronExp = it.cronExp,
                lastRunTime = it.lastRunTime,
                creator = it.creator,
                updater = it.updater,
                updateTime = it.updateTime,
                runTimes = it.runTimes
            )
        }
        return Page(search.page, search.pageSize, count, records)
    }

    fun recordRerun(
        userId: String,
        id: Long
    ) {
        val record = remoteDevJobExecRecordDao.getRecord(dslContext, id) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
            params = arrayOf(I18nUtil.getCodeLanMessage(REMOTEDEV_JOB_NOT_FOUND, params = arrayOf(id.toString())))
        )

        if (!permissionService.checkUserVisitPermission(userId, record.projectId)) {
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.FORBIDDEN.errorCode,
                params = arrayOf("You need permission to access project ${record.projectId}")
            )
        }
        remoteDevJobExecRecordDao.updateStatus(dslContext, id, JobRecordStatus.RUNNING, null, null)
        when (val param = objectMapper.readValue<JobSchemaParam>(record.jobSchemaParam.data())) {
            is NotifyRemoteDevDesktopParam -> {
                remoteDevActionService.notifyRemoteDevDesktop(id, param)
            }

            is CronPowerOnParam -> {
                remoteDevJobExecRecordDao.updateStatus(dslContext, id, JobRecordStatus.RUNNING, null, null)
                // TODO: 周期任务的执行器
            }

            is PipelineParam -> {
                remoteDevJobExecRecordDao.updateStatus(dslContext, id, JobRecordStatus.RUNNING, null, null, true)
                remoteDevActionService.startPipeline(record.projectId, id, param)
            }
        }
    }

    fun pipelineJobEnd(
        id: Long
    ) {
        AsyncExecute.dispatch(streamBridge, AsyncJobEndEvent(id))
    }

    fun doPipelineJobEnd(
        id: Long
    ) {
        val record = remoteDevJobExecRecordDao.getRecord(dslContext, id) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
            params = arrayOf(I18nUtil.getCodeLanMessage(REMOTEDEV_JOB_NOT_FOUND, params = arrayOf(id.toString())))
        )
        val info = objectMapper.readValue<PipelineParam>(record.jobSchemaParam.data())
        val receiptInfo = objectMapper.readValue<PipelineJobReceiptInfo>(
            record.receiptInfo?.data() ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
                defaultMessage = "job $id receipt is null"
            )
        )
        repeat(5) {
            // 先暂停60s看看，因为是从finalStage发送的，发送到的时候流水线的状态还是运行中
            Thread.sleep(1000 * 30)
            // 获取流水线状态
            val build = client.get(ServiceBuildResource::class).getBuildStatusWithoutPermission(
                userId = info.userId,
                projectId = info.projectId,
                pipelineId = info.pipelineId,
                buildId = receiptInfo.buildId,
                channelCode = ChannelCode.BS
            ).data ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
                defaultMessage = "build ${receiptInfo.buildId} status is null"
            )
            val status = BuildStatus.parse(build.status)
            if (!status.isFinish()) {
                return@repeat
            }
            remoteDevJobExecRecordDao.updateStatus(
                dslContext = dslContext,
                id = id,
                status = if (status.isFailure()) {
                    JobRecordStatus.FAIL
                } else {
                    JobRecordStatus.SUCCESS
                },
                // 太长了而且可以通过流水线看，不存了
                errMsg = null,
                endTime = LocalDateTime.now()
            )
            return
        }
        remoteDevJobExecRecordDao.updateStatus(
            dslContext = dslContext,
            id = id,
            status = JobRecordStatus.UNKNOWN,
            errMsg = I18nUtil.getCodeLanMessage(REMOTEDEV_PIPELINE_JOB_STATUS_UNKNOWN),
            endTime = LocalDateTime.now()
        )
    }

    fun fetchJobDetail(jobId: Long): JobDetail? {
        val record = remoteDevJobExecRecordDao.getRecord(dslContext, jobId) ?: return null
        return when (val param = objectMapper.readValue<JobSchemaParam>(record.jobSchemaParam.data())) {
            is PipelineParam -> {
                val data = remoteDevActionService.fetchPipelineJobDetail(
                    param = param,
                    receiptInfo = record.receiptInfo ?: return null
                )
                return JobDetail(JobActionType.PIPELINE, data)
            }

            else -> null
        }
    }

    companion object {
        private const val REMOTEDEV_JOB_NOT_FOUND = "remotedevJobNotFound"
        private const val REMOTEDEV_ACTION_NOT_FOUND = "remotedevActionNotFound"
        private const val REMOTEDEV_ACTION_KEY_NOT_FOUND = "remotedevActionKeyNotFound"
        private const val REMOTEDEV_ACTION_VALUE_TRANS_ERROR = "remotedevActionValueTransError"
        private const val REMOTEDEV_PIPELINE_JOB_STATUS_UNKNOWN = "remotedevPipelineJobStatusUnknown"
        private const val ONCE_JOB_RECORD_NAME_PREFIX = "【一次性任务】"
        private const val CRON_JOB_RECORD_NAME_PREFIX = "【周期任务】"
        private fun keyNotFound(key: String): ErrorCodeException {
            return ErrorCodeException(
                errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
                params = arrayOf(I18nUtil.getCodeLanMessage(REMOTEDEV_ACTION_KEY_NOT_FOUND, params = arrayOf(key)))
            )
        }

        private inline fun <reified T> transValue(value: Any, type: KeyMapDataType): T {
            return try {
                value as T
            } catch (e: Exception) {
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.REMOTEDEV_JOB_ERROR.errorCode,
                    params = arrayOf(
                        I18nUtil.getCodeLanMessage(
                            REMOTEDEV_ACTION_VALUE_TRANS_ERROR,
                            params = arrayOf(value.toString(), type.name)
                        )
                    )
                )
            }
        }

        private val jobNameFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}

package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.cos.ServicePluginCosResource
import com.tencent.devops.plugin.pojo.cos.CdnUploadFileInfo
import com.tencent.devops.common.pipeline.element.CosCdnDistributionElementDev
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.AtomErrorCode.USER_TASK_OPERATE_FAIL
import com.tencent.devops.process.engine.common.BS_ATOM_START_TIME_MILLS
import com.tencent.devops.process.engine.common.BS_ATOM_STATUS_REFRESH_DELAY_MILLS
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class CosCdnDistributionDevTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate,
    private val redisOperation: RedisOperation
) : IAtomTask<CosCdnDistributionElementDev> {

    override fun getParamElement(task: PipelineBuildTask): CosCdnDistributionElementDev {
        return JsonUtil.mapTo(task.taskParams, CosCdnDistributionElementDev::class.java)
    }

    override fun execute(
        task: PipelineBuildTask,
        param: CosCdnDistributionElementDev,
        runVariables: Map<String, String>
    ): AtomResponse {
        logger.info("Enter CosCdnDistributionDelegate run...")

        val taskId = task.taskId
        val buildId = task.buildId
        if (param.regexPaths.isBlank()) {
            logger.error("regexPaths is not initialized of build($buildId)")
            LogUtils.addRedLine(
                rabbitTemplate,
                buildId,
                "regexPaths is not initialized",
                taskId,
                task.executeCount ?: 1
            )
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "regexPaths is not initialized"
            )
        }

        if (param.ticketId.isBlank()) {
            logger.error("ticketId is not initialized of build($buildId)")
            LogUtils.addRedLine(rabbitTemplate, buildId, "ticketId is not initialized", taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "ticketId is not initialized"
            )
        }

        val regexPathsStr = parseVariable(param.regexPaths, runVariables)
        val isCustom = param.customize
        val ticketId = parseVariable(param.ticketId, runVariables)
        val cdnPathPrefix = parseVariable(param.cdnPathPrefix, runVariables)
        val maxRunningMins = param.maxRunningMins
        val userId = runVariables[PIPELINE_START_USER_ID]

        if (userId == null) {
            logger.warn("The start user is empty")
            LogUtils.addRedLine(rabbitTemplate, buildId, "启动用户名为空", taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_INPUT_INVAILD,
                errorMsg = "启动用户名为空"
            )
        }

        val projectId = task.projectId
        val pipelineId = task.pipelineId

        val cdnUploadFileInfo = CdnUploadFileInfo(regexPathsStr, isCustom, ticketId, cdnPathPrefix)
        val taskResult = client.get(ServicePluginCosResource::class)
            .uploadCdn(projectId, pipelineId, buildId, taskId, task.executeCount ?: 1, cdnUploadFileInfo)
        if (taskResult.isNotOk() || taskResult.data == null) {
            logger.warn("Start upload to cdn task failed.msg:${taskResult.message}")
            LogUtils.addRedLine(rabbitTemplate, buildId, "上传CDN失败", taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_TASK_OPERATE_FAIL,
                errorMsg = "上传CDN失败"
            )
        }

        val startTime = System.currentTimeMillis()
        val cdnUploadTaskId = taskResult.data!!.uploadTaskKey
        val buildStatus = checkStatus(startTime, maxRunningMins, buildId, cdnUploadTaskId, task.executeCount ?: 1)
        if (!BuildStatus.isFinish(buildStatus)) {
            task.taskParams["bsCdnUploadTaskId"] = cdnUploadTaskId
            task.taskParams[BS_ATOM_START_TIME_MILLS] = startTime
            task.taskParams[BS_ATOM_STATUS_REFRESH_DELAY_MILLS] = 5000
            LogUtils.addLine(rabbitTemplate, buildId, "等待上传结果", taskId, task.executeCount ?: 1)
        }
        return if (buildStatus == BuildStatus.FAILED)
            AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_TASK_OPERATE_FAIL,
                errorMsg = "上传CDN失败"
            )
        else AtomResponse(buildStatus)
    }

    override fun tryFinish(
        task: PipelineBuildTask,
        param: CosCdnDistributionElementDev,
        runVariables: Map<String, String>,
        force: Boolean
    ): AtomResponse {
        val buildId = task.buildId
        if (task.taskParams["bsCdnUploadTaskId"] == null) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "找不到CDN任务ID，请联系管理员", task.taskId, task.executeCount ?: 1)
            return AtomResponse(
                buildStatus = BuildStatus.FAILED,
                errorType = ErrorType.USER,
                errorCode = USER_TASK_OPERATE_FAIL,
                errorMsg = "找不到CDN任务ID，请联系管理员"
            )
        }
        val cdnUploadTaskId = task.taskParams["bsCdnUploadTaskId"].toString()
        val startTime = task.taskParams[BS_ATOM_START_TIME_MILLS].toString().toLong()
        val maxRunningMins = param.maxRunningMins
        return AtomResponse(checkStatus(startTime, maxRunningMins, buildId, cdnUploadTaskId, task.executeCount ?: 1))
    }

    private fun checkStatus(
        startTime: Long,
        maxRunningMins: Int,
        buildId: String,
        taskId: String,
        executeCount: Int
    ): BuildStatus {
        logger.info("Waiting for upload task done...")
        if (System.currentTimeMillis() - startTime > maxRunningMins * 60 * 1000) {
            logger.warn("Upload to cdn timeout. timeout minutes:$maxRunningMins")
            LogUtils.addRedLine(rabbitTemplate, buildId, "上传CDN失败超时，超时时间:$maxRunningMins", taskId, executeCount)
            return BuildStatus.FAILED
        }

        val status = redisOperation.get(taskId + "_status")
        if (status == null) {
            logger.info("Upload cdn process is empty, continue...")
        } else if (status.toInt() == 1) {
            val index = redisOperation.get(taskId + "_count")
            logger.info("Upload cdn running, file index: $index")
        } else if (status.toInt() == 0) {
            val count = redisOperation.get(taskId + "_count")
            val uploadDetail = redisOperation.get(taskId + "_result")
            if (null != uploadDetail) {
                logger.info("Upload to cdn success, total file: ${count?.toInt()}, detail information: $uploadDetail")
                LogUtils.addLine(
                    rabbitTemplate,
                    buildId,
                    "Upload to cdn success, total file: ${count?.toInt()}, detail: $uploadDetail%",
                    taskId,
                    executeCount
                )
                return BuildStatus.SUCCEED
            }
        } else if (status.toInt() == 2) {
            logger.info("Upload to cdn failed!")
            LogUtils.addRedLine(rabbitTemplate, buildId, "上传CDN失败", taskId, executeCount)
            return BuildStatus.FAILED
        }
        return BuildStatus.LOOP_WAITING
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CosCdnDistributionDevTaskAtom::class.java)
    }
}

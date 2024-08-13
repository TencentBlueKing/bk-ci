package com.tencent.devops.dispatch.service.tpaqueue

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_FAILED_START_BUILD_MACHINE
import com.tencent.devops.common.api.constant.CommonMessageCode.JOB_BUILD_STOPS
import com.tencent.devops.common.api.constant.CommonMessageCode.UNABLE_GET_PIPELINE_JOB_STATUS
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.dispatch.sdk.utils.DispatchLogRedisUtils
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.dao.TPAQueueDao
import com.tencent.devops.dispatch.dao.ThirdPartyAgentQueueSqlData
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.QueueFailureException
import com.tencent.devops.dispatch.pojo.QueueRetryException
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.pojo.TPAQueueEvent
import com.tencent.devops.dispatch.pojo.TPAQueueEventContext
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentSqlQueueType
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.ThirdPartyAgentQueueEnvLock
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.engine.common.VMUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class TPAQueueService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val rabbitTemplate: RabbitTemplate,
    private val client: Client,
    private val commonUtil: TPACommonUtil,
    private val tpaQueueDao: TPAQueueDao,
    private val thirdPartyAgentEnvQueueService: ThirdPartyAgentEnvQueueService
) {
    fun queue(data: ThirdPartyAgentDispatchData) {
        logger.info("queue|${data.toLog()}")
        val (sqlData, dataType) = when (data.dispatchType) {
            // 目前只做环境排队
            is ThirdPartyAgentEnvDispatchType -> Pair(data.genEnv()!!, ThirdPartyAgentSqlQueueType.ENV)
            else -> throw InvalidParamException("Unknown agent type - ${data.dispatchType}")
        }
        tpaQueueDao.add(
            dslContext = dslContext,
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            buildId = data.buildId,
            vmSeqId = data.vmSeqId,
            data = sqlData,
            dataType = dataType,
            info = data.genSqlJsonData(),
            retryTime = 0
        )
        val event = TPAQueueEvent(
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            data = sqlData,
            dataType = dataType,
            sendData = data,
            delayMills = 5000,
            lockValue = UUID.randomUUID().toString()
        )
        dispatch(event)
    }

    /**
     * 因为每个消费消息的线程数量有限制，如果线程慢了，其他消息不会消失，只会等待消费
     * 所以生产和消费共用一把锁，这样才可以做到每次只有一个消息在消费
     * TODO: 还需要再想想看看，比如 uuid 一直用会不会重复，把 error 日志汇总下方便告警
     */
    private fun dispatch(event: TPAQueueEvent) {
        logger.info("queue_dispatch|${event.toLog()}")
        // 每个排队队列中的最长的只会是Job排队时间，7天
        // 目前只有ENV
        val lock = ThirdPartyAgentQueueEnvLock(
            redisOperation = redisOperation,
            projectId = event.projectId,
            queueKey = event.data,
            expiredTimeInSeconds = ENV_LOCK_TIME_OUT_7D,
            lockValue = event.lockValue
        )
        if (!lock.tryLock(timeout = 5000, interval = 1000)) {
            if (event.sendData != null) {
                commonUtil.logDebug(event.sendData!!, "do queue no lock wait other queue")
                logger.info("doQueue|${event.sendData?.toLog()}|no lock wait other queue")
                event.sendData = null
            }
            return
        }
        try {
            if (event.sendData != null) {
                commonUtil.logDebug(event.sendData!!, "do queue get lock in queue")
                logger.info("doQueue|${event.sendData?.toLog()}|get lock in queue")
                event.sendData = null
            }
            logger.info("queue_dispatch|${event.toLog()}")
            send(event)
        } catch (e: Throwable) {
            // 只可能是发送消息错误或者抓到的异常处理逻辑错误，但是为了防止没解锁
            logger.error("dispatch|${event.toLog()}|error", e)
            lock.unlock()
        }
    }

    private fun send(event: TPAQueueEvent) {
        val eventType = event::class.java.annotations.find { s -> s is Event } as Event
        rabbitTemplate.convertAndSend(eventType.exchange, eventType.routeKey, event) { message ->
            // 事件中的变量指定
            when {
                event.delayMills > 0 -> message.messageProperties.setHeader("x-delay", event.delayMills)
                eventType.delayMills > 0 -> // 事件类型固化默认值
                    message.messageProperties.setHeader("x-delay", eventType.delayMills)

                else -> // 非延时消息的则8小时后过期，防止意外发送的消息无消费端ACK处理从而堆积过多消息导致MQ故障
                    message.messageProperties.expiration = "28800000"
            }
            message
        }
    }

    /**
     * 进入排队，每次 sql 数据变更，都应该持有锁并发送消息（在队列循环结束后也会重新拿取一次 sql，即是消息更新）
     * 这样在不处理完所有数据前，队列永远存在
     * 同时将排队信息保存到 sql 中，每次结束排队都更新，这样不论哪个消息进来了，信息都是持久化的
     */
    fun doQueue(event: TPAQueueEvent) {
        inQueue(event)
        val lock = ThirdPartyAgentQueueEnvLock(
            redisOperation = redisOperation,
            projectId = event.projectId,
            queueKey = event.data,
            expiredTimeInSeconds = ENV_LOCK_TIME_OUT_7D,
            lockValue = event.lockValue
        )
        lock.unlock()
        // 解锁后去查询，如果还有没有下发完的那么就再发送一条记录
        try {
            val count = tpaQueueDao.fetchProjectDataCount(
                dslContext = dslContext,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                data = event.data,
                dataType = event.dataType
            )
            if (count > 0) {
                dispatch(event)
            }
        } catch (e: Throwable) {
            // 只可能是Sql错误或者抓到的异常处理逻辑错误，但是为了防止丢失消息，还是抓一下重发
            logger.error("doQueue|${event.toLog()}|error", e)
            dispatch(event)
        }
    }

    fun inQueue(event: TPAQueueEvent) {
        // context 只能初始化一次，但是因为初始化过程中也可能出现报错，所以需要把可能的报错分摊给每个消息，防止一次报错整个队列没了
        val eventContext = TPAQueueEventContext()
        try {
            tpaQueueDao.fetchProjectData(
                dslContext = dslContext,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                data = event.data,
                dataType = event.dataType
            ).forEach { sqlData ->
                // 目前只做环境排队
                if (event.dataType == ThirdPartyAgentSqlQueueType.ENV) {
                    inEnvQueue(eventContext, sqlData)
                }
            }
            // 循环事件结束收尾，删除调度成功的，增加重试测试
            tpaQueueDao.deleteByIds(dslContext, eventContext.needDeleteRecord)
            tpaQueueDao.addRetryTimeByIds(dslContext, eventContext.needRetryRecord)
        } catch (e: Throwable) {
            // 只可能是Sql错误或者抓到的异常处理逻辑错误，但是为了防止丢失消息，还是抓一下重发
            logger.error("doEnvQueue|${event.toLog()}|error", e)
            dispatch(event)
        }
    }

    /**
     * 对 inEnvQueue 的包装，主要用来整合异常,例如结束和重试异常
     */
    fun inEnvQueue(
        eventContext: TPAQueueEventContext,
        sqlData: ThirdPartyAgentQueueSqlData
    ) {
        kotlin.runCatching {
            val dataContext = QueueDataContext(sqlData.data, sqlData.retryTime)
            if (!checkRunning(dataContext)) {
                eventContext.addDelete(sqlData.recordId)
                return
            }
            thirdPartyAgentEnvQueueService.inEnvQueue(eventContext, dataContext)
            // 只有调度成功才能走到这一步，到这一步就删除,同时删除数据库
            eventContext.addDelete(sqlData.recordId)
        }.onFailure { queueEnd(eventContext, sqlData, it) }
    }

    private fun checkRunning(dataContext: QueueDataContext): Boolean {
        if (doCheckRunning(dataContext)) {
            return true
        }
        if (dataContext.retryTime > 1) {
            // 重试的请求如果流水线已结束，主动把配额记录删除
            SpringContextUtil.getBean(JobQuotaService::class.java).removeRunningJob(
                projectId = dataContext.data.projectId,
                pipelineId = dataContext.data.pipelineId,
                buildId = dataContext.data.buildId,
                vmSeqId = dataContext.data.vmSeqId,
                executeCount = dataContext.data.executeCount
            )
        }
        return false
    }

    private fun doCheckRunning(dataContext: QueueDataContext): Boolean {
        val data = dataContext.data
        // 判断流水线当前container是否在运行中
        val statusResult = client.get(ServicePipelineTaskResource::class).getContainerStartupInfo(
            projectId = data.projectId,
            buildId = data.buildId,
            containerId = data.vmSeqId,
            taskId = VMUtils.genStartVMTaskId(data.vmSeqId)
        )
        val startBuildTask = statusResult.data?.startBuildTask
        val buildContainer = statusResult.data?.buildContainer
        if (statusResult.isNotOk() || startBuildTask == null || buildContainer == null) {
            logger.warn(
                "The build ${data.toLog()} fail to check if pipeline task is running " +
                        "because of statusResult(${statusResult.message})"
            )
            val errorMessage = I18nUtil.getCodeLanMessage(UNABLE_GET_PIPELINE_JOB_STATUS)
            throw QueueFailureException(
                errorType = ErrorType.SYSTEM,
                errorCode = UNABLE_GET_PIPELINE_JOB_STATUS.toInt(),
                formatErrorMessage = errorMessage,
                errorMessage = errorMessage
            )
        }

        var needStart = true
        if (data.executeCount != startBuildTask.executeCount) {
            // 如果已经重试过或执行次数不匹配则直接丢弃
            needStart = false
        } else if (startBuildTask.status.isFinish() && buildContainer.status.isRunning()) {
            // 如果Job已经启动在运行或则直接丢弃
            needStart = false
        } else if (!buildContainer.status.isRunning() && !buildContainer.status.isReadyToRun()) {
            needStart = false
        }

        if (!needStart) {
            logger.warn("The build ${data.toLog()} is not running")
            // dispatch主动发起的重试或者用户已取消的流水线忽略异常报错
            if (dataContext.retryTime > 1 || buildContainer.status.isCancel()) {
                return false
            }

            val errorMessage = I18nUtil.getCodeLanMessage(JOB_BUILD_STOPS)
            throw QueueFailureException(
                errorType = ErrorType.USER,
                errorCode = JOB_BUILD_STOPS.toInt(),
                formatErrorMessage = errorMessage,
                errorMessage = errorMessage
            )
        }
        return true
    }

    fun queueEnd(
        eventContext: TPAQueueEventContext,
        sqlData: ThirdPartyAgentQueueSqlData,
        e: Throwable
    ) {
        val data = sqlData.data
        when (e) {
            // 未来架构稳定后整合到一起
            is QueueRetryException, is DispatchRetryMQException -> {
                // 用时间做判断，避免 retryTime 的加减
                val timeOut = data.queueTimeoutMinutes ?: 10
                if (sqlData.createTime.plusMinutes(timeOut.toLong()) > LocalDateTime.now()) {
                    eventContext.addRetry(sqlData.recordId)
                    return
                }
                // 超时就是结束
                onFailure(
                    data = data,
                    exception = QueueFailureException(
                        errorType = ErrorType.SYSTEM,
                        errorCode = DispatchSdkErrorCode.RETRY_STARTUP_FAIL,
                        formatErrorMessage = e.message ?: "Fail to start up the job after $timeOut minutes",
                        errorMessage = e.message ?: "Fail to start up the job after $timeOut minutes"
                    )
                )
            }

            is QueueFailureException -> {
                onFailure(data, e)
            }

            // 未来架构稳定后整合到一起
            is BuildFailureException -> {
                onFailure(
                    data, QueueFailureException(
                        errorType = e.errorType,
                        errorCode = e.errorCode,
                        formatErrorMessage = e.formatErrorMessage,
                        errorMessage = e.message ?: ""
                    )
                )
            }

            else -> {
                logger.error("doEnvQueue|unknowError|${data.toLog()}", e)
                onFailure(
                    data = data,
                    exception = QueueFailureException(
                        errorType = ErrorType.SYSTEM,
                        errorCode = DispatchSdkErrorCode.SDK_SYSTEM_ERROR,
                        formatErrorMessage = "Fail to handle the start up message",
                        errorMessage = e.message ?: ""
                    )
                )
            }
        }
        eventContext.addDelete(sqlData.recordId)
    }

    private fun onFailure(
        data: ThirdPartyAgentDispatchData,
        exception: QueueFailureException
    ) {
        commonUtil.logError(
            data = data,
            message = "${I18nUtil.getCodeLanMessage("$BK_FAILED_START_BUILD_MACHINE")}-${exception.message}"
        )
        try {
            client.get(ServiceBuildResource::class).setVMStatus(
                projectId = data.projectId,
                pipelineId = data.pipelineId,
                buildId = data.buildId,
                vmSeqId = data.vmSeqId,
                status = BuildStatus.FAILED,
                errorType = exception.errorType,
                errorCode = exception.errorCode,
                errorMsg = exception.formatErrorMessage
            )
        } catch (ignore: ClientException) {
            logger.error("SystemErrorLogMonitor|onContainerFailure|${data.toLog()}|error=$exception")
        }
        DispatchLogRedisUtils.removeRedisExecuteCount(data.buildId)
    }

    companion object {
        private const val ENV_LOCK_TIME_OUT_7D = 60 * 60 * 24 * 7L
        private val logger = LoggerFactory.getLogger(TPAQueueService::class.java)
    }
}
package com.tencent.devops.dispatch.service.tpaqueue

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_FAILED_START_BUILD_MACHINE
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.service.DispatchService
import com.tencent.devops.common.dispatch.sdk.service.JobQuotaService
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.dao.TPAQueueDao
import com.tencent.devops.dispatch.dao.ThirdPartyAgentQueueSqlData
import com.tencent.devops.dispatch.exception.DispatchRetryMQException
import com.tencent.devops.dispatch.pojo.QueueDataContext
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentDispatchData
import com.tencent.devops.dispatch.pojo.TPAQueueEvent
import com.tencent.devops.dispatch.pojo.TPAQueueEventContext
import com.tencent.devops.dispatch.pojo.ThirdPartyAgentSqlQueueType
import com.tencent.devops.dispatch.utils.TPACommonUtil
import com.tencent.devops.dispatch.utils.TPACommonUtil.Companion.tagError
import com.tencent.devops.dispatch.utils.ThirdPartyAgentQueueEnvLock
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
    private val tpaEnvQueueService: TPAEnvQueueService,
    private val dispatchService: DispatchService,
    private val jobQuotaService: JobQuotaService
) {
    fun queue(data: ThirdPartyAgentDispatchData) {
        logger.info("queue|${data.toLog()}")
        val (sqlData, dataType) = when (data.dispatchType) {
            // 目前只做环境排队
            is ThirdPartyAgentEnvDispatchType -> Pair(data.genEnv()!!, ThirdPartyAgentSqlQueueType.ENV)
            else -> throw InvalidParamException("Unknown agent type - ${data.dispatchType}")
        }
        val now = LocalDateTime.now()
        tpaQueueDao.add(
            dslContext = dslContext,
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            buildId = data.buildId,
            vmSeqId = data.vmSeqId,
            data = sqlData,
            dataType = dataType,
            info = data.genSqlJsonData(),
            retryTime = 0,
            createTime = now,
            updateTime = now
        )
        // 写入耗时，防止在排队中被取消了
        commonUtil.updateQueueTime(data, now.timestampmilli(), null)
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
            logger.tagError("dispatch|send|${event.toLog()}|error", e)
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
            logger.tagError("doQueue|fetchProjectDataCount|${event.toLog()}|error", e)
            dispatch(event)
        }
    }

    private fun inQueue(event: TPAQueueEvent) {
        try {
            val records = tpaQueueDao.fetchProjectData(
                dslContext = dslContext,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                data = event.data,
                dataType = event.dataType
            )
            // 事件开始，在这初始化时间去掉数据库查询的系统时间
            val eventContext = TPAQueueEventContext()
            // 目前只做环境排队
            records.filter { event.dataType == ThirdPartyAgentSqlQueueType.ENV }.forEachIndexed { index, sqlData ->
                inEnvQueue(eventContext, sqlData, records.size, index + 1)
            }
            // 循环事件结束收尾，增加重试测试
            tpaQueueDao.addRetryTimeByIds(dslContext, eventContext.needRetryRecord)
        } catch (e: Throwable) {
            // 只可能是Sql错误或者抓到的异常处理逻辑错误，但是为了防止丢失消息，还是抓一下重发
            logger.tagError("inQueue|fetchProjectData|${event.toLog()}|error", e)
            dispatch(event)
        }
    }

    /**
     * 对 inEnvQueue 的包装，主要用来整合异常,例如结束和重试异常
     */
    private fun inEnvQueue(
        eventContext: TPAQueueEventContext,
        sqlData: ThirdPartyAgentQueueSqlData,
        queueSize: Int,
        queueIndex: Int
    ) {
        try {
            val dataContext = QueueDataContext(sqlData.data, sqlData.retryTime)
            if (!checkRunning(dataContext)) {
                eventContext.setDelete(sqlData.recordId)
                return
            }

            val costMilliSecond = System.currentTimeMillis() - eventContext.startTimeMilliSecond
            commonUtil.logDebug(
                dataContext.data, "env queue size:$queueSize index:$queueIndex cost ${costMilliSecond}ms"
            )
            // context 只能初始化一次，但是因为初始化过程中也可能出现报错，所以需要把可能的报错分摊给每个消息，防止一次报错整个队列没了
            if (eventContext.context == null) {
                eventContext.context = tpaEnvQueueService.initEnvContext(dataContext)
            }
            tpaEnvQueueService.inEnvQueue(eventContext.context!!, dataContext)
            // 只有调度成功才能走到这一步，到这一步就删除,同时删除数据库
            eventContext.setDelete(sqlData.recordId)
        } catch (e: Throwable) {
            queueEnd(eventContext, sqlData, e)
        } finally {
            // 计算用户耗时，只能刚下发就写入，防止执行完了还没启动计算，同时也要删除，防止用户取消后计时计算错误
            if (eventContext.needDeleteRecord?.first == sqlData.recordId) {
                tpaQueueDao.delete(dslContext, sqlData.recordId)
                commonUtil.updateQueueTime(
                    data = sqlData.data,
                    createTime = sqlData.createTime.timestampmilli(),
                    endTime = eventContext.needDeleteRecord?.second
                )
            }
        }
    }

    private fun checkRunning(dataContext: QueueDataContext): Boolean {
        val data = dataContext.data
        val running = dispatchService.checkRunning(
            projectId = data.projectId,
            buildId = data.buildId,
            containerId = data.vmSeqId,
            retryTime = dataContext.retryTime,
            executeCount = data.executeCount,
            logTag = data.toLog()
        )
        if (!running && dataContext.retryTime > 1) {
            // 重试的请求如果流水线已结束，主动把配额记录删除
            jobQuotaService.removeRunningJob(
                projectId = dataContext.data.projectId,
                pipelineId = dataContext.data.pipelineId,
                buildId = dataContext.data.buildId,
                vmSeqId = dataContext.data.vmSeqId,
                executeCount = dataContext.data.executeCount
            )
        }
        return running
    }

    fun queueEnd(
        eventContext: TPAQueueEventContext,
        sqlData: ThirdPartyAgentQueueSqlData,
        e: Throwable
    ) {
        val data = sqlData.data
        val failureE = when (e) {
            is DispatchRetryMQException -> {
                // 用时间做判断，避免 retryTime 的加减
                val timeOut = data.queueTimeoutMinutes ?: 10
                if (sqlData.createTime.plusMinutes(timeOut.toLong()) > LocalDateTime.now()) {
                    eventContext.addRetry(sqlData.recordId)
                    return
                }
                // 超时就是结束
                BuildFailureException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = DispatchSdkErrorCode.RETRY_STARTUP_FAIL,
                    formatErrorMessage = e.message ?: "Fail to start up the job after $timeOut minutes",
                    errorMessage = e.message ?: "Fail to start up the job after $timeOut minutes"
                )
            }

            is BuildFailureException -> e

            else -> {
                logger.tagError("queueEnd|unKnowError|${data.toLog()}", e)
                BuildFailureException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = DispatchSdkErrorCode.SDK_SYSTEM_ERROR,
                    formatErrorMessage = e.message ?: "Fail to handle the start up message",
                    errorMessage = e.message ?: "Fail to handle the start up message"
                )
            }
        }
        eventContext.setDelete(sqlData.recordId)
        onFailure(data, failureE)
    }

    private fun onFailure(
        data: ThirdPartyAgentDispatchData,
        exception: BuildFailureException
    ) {
        commonUtil.logError(
            data = data,
            message = "${I18nUtil.getCodeLanMessage(BK_FAILED_START_BUILD_MACHINE)}-${exception.message}"
        )
        dispatchService.onFailure(
            projectId = data.projectId,
            pipelineId = data.pipelineId,
            buildId = data.buildId,
            vmSeqId = data.vmSeqId,
            e = exception
        )
    }

    fun finishQueue(buildId: String, vmSeqId: String?) {
        val now = LocalDateTime.now().timestampmilli()
        val records = tpaQueueDao.fetchTimeByBuild(dslContext, buildId, vmSeqId).ifEmpty { return }
        // 取消时兜底结束时间
        records.forEach { record ->
            commonUtil.updateQueueTime(record.data, record.createTime.timestampmilli(), now)
        }
        tpaQueueDao.deleteByIds(dslContext, records.map { it.recordId }.toSet())
    }

    companion object {
        private const val ENV_LOCK_TIME_OUT_7D = 60 * 60 * 24 * 7L
        private val logger = LoggerFactory.getLogger(TPAQueueService::class.java)
    }
}
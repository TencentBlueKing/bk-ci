package com.tencent.devops.process.trigger

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineEventRegisterDao
import com.tencent.devops.process.pojo.trigger.PipelineEventRegister
import com.tencent.devops.process.trigger.pojo.PipelineEventRegisterLock
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 外部事件注册记录：源上是否已挂回调。与具体事件源（制品库/SCM）解耦。
 *
 * 命中本地记录则不再访问外部系统；未命中才执行 [onMiss]（由调用方 list/create），再落库。
 */
@Service
class PipelineEventRegisterService(
    private val dslContext: DSLContext,
    private val pipelineEventRegisterDao: PipelineEventRegisterDao,
    private val redisOperation: RedisOperation,
    private val client: Client
) {

    fun exists(
        projectId: String,
        eventCode: String,
        eventSource: String,
        eventType: String,
        eventScopes: List<String>? = null,
        callbackUrl: String
    ): Boolean {
        return pipelineEventRegisterDao.count(
            dslContext = dslContext,
            projectId = projectId,
            eventCode = eventCode,
            eventSource = eventSource,
            eventType = eventType,
            eventScopes = eventScopes,
            callbackUrl = callbackUrl
        ) > 0
    }

    fun save(userId: String, register: PipelineEventRegister) {
        val id = client.get(ServiceAllocIdResource::class)
            .generateSegmentId(EVENT_REGISTER_BIZ_ID).data ?: 0
        pipelineEventRegisterDao.save(
            dslContext = dslContext,
            userId = userId,
            id = id,
            register = register
        )
    }

    /**
     * 本地无记录时对接外部系统并落库；已有记录则直接返回。
     *
     * @param onMiss 本地无记录时由调用方对接外部系统，返回外部 webhook ID（无可填 null）
     */
    fun saveIfAbsent(
        userId: String,
        register: PipelineEventRegister,
        onMiss: () -> String?
    ) {
        val callbackUrl = register.callbackUrl ?: return
        val eventScopes = listOf(register.eventScope.orEmpty())
        if (exists(
                projectId = register.projectId,
                eventCode = register.eventCode,
                eventSource = register.eventSource,
                eventType = register.eventType,
                eventScopes = eventScopes,
                callbackUrl = callbackUrl
            )
        ) {
            return
        }
        PipelineEventRegisterLock(
            redisOperation = redisOperation,
            projectId = register.projectId,
            eventCode = register.eventCode,
            eventSource = register.eventSource,
            eventType = register.eventType
        ).use { lock ->
            lock.lock()
            if (exists(
                    projectId = register.projectId,
                    eventCode = register.eventCode,
                    eventSource = register.eventSource,
                    eventType = register.eventType,
                    eventScopes = eventScopes,
                    callbackUrl = callbackUrl
                )
            ) {
                return
            }
            val externalId = onMiss()
            save(userId = userId, register = register.copy(externalId = externalId))
            logger.info(
                "save event register|${register.projectId}|${register.eventCode}|" +
                    "${register.eventSource}|${register.eventType}|scope=${register.eventScope}"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineEventRegisterService::class.java)
        private const val EVENT_REGISTER_BIZ_ID = "T_PIPELINE_EVENT_REGISTER"
    }
}

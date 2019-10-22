package com.tencent.devops.process.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.PipelineMessage
import com.tencent.devops.common.websocket.dispatch.message.SendMessage
import com.tencent.devops.common.websocket.dispatch.push.WebsocketPush
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.PageUtils
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.PipelineStatus
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class StatusWebsocketPush(
        val buildId: String?,
        val pipelineId: String,
        val projectId: String,
        val pipelineService: PipelineService,
        override val userId: String,
        override val pathClass: IPath,
        override val pushType: WebSocketType,
        override val redisOperation: RedisOperation,
        override val objectMapper: ObjectMapper,
        override var page: String?,
        override var notifyPost: NotifyPost
) : WebsocketPush(userId, pathClass, pushType, redisOperation, objectMapper, page, notifyPost) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun findSession(page: String): List<String>? {
        val pageList = PageUtils.createAllTagPage(page!!)

        var sessionList = mutableListOf<String>()
        pageList.forEach {
            val redisSession = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, it)
            if(redisSession != null){
                sessionList.addAll(redisSession)
            }
        }
        return sessionList
    }

    override fun buildMqMessage(): SendMessage? {
        return PipelineMessage(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                notifyPost = notifyPost,
                userId = userId,
                page = page,
                sessionList = findSession(page!!)!!
        )
    }

    override fun buildNotifyMessage(message: SendMessage) {
        val notifyPost = message.notifyPost

        val currentTimestamp = System.currentTimeMillis()

        val status = pipelineService.getPipelineAllStatus(userId, projectId, pipelineId)

        if (status != null) {
            val result = status.map {
                it.pipelineId to PipelineStatus(
                        it.taskCount,
                        it.buildCount,
                        it.lock,
                        it.canManualStartup,
                        it.latestBuildStartTime,
                        it.latestBuildEndTime,
                        it.latestBuildStatus,
                        it.latestBuildNum,
                        it.latestBuildTaskName,
                        it.latestBuildEstimatedExecutionSeconds,
                        it.latestBuildId,
                        currentTimestamp,
                        it.runningBuildCount,
                        it.hasCollect
                )
            }.toMap()

            if (notifyPost != null) {
                notifyPost.message = objectMapper.writeValueAsString(result)
            }
        }
    }
}
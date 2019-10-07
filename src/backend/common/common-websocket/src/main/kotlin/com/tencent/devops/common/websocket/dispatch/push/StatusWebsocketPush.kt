package com.tencent.devops.common.websocket.dispatch.push

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.PipelineMqMessage
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.PageUtils
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class StatusWebsocketPush(
    val buildId: String?,
    val pipelineId: String,
    val projectId: String,
    override val userId: String,
    override val pathClass: IPath,
    override val pushType: WebSocketType,
    override val redisOperation: RedisOperation,
    override var page: String?,
    override var notifyPost: NotifyPost
) : IWebsocketPush(userId, pathClass, pushType, redisOperation, page, notifyPost) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun isPushBySession(): Boolean {
        return super.isPushBySession()
    }

    override fun isPushByPage(): Boolean {
        val buildPageInfo = BuildPageInfo(
            buildId = buildId,
            projectId = projectId,
            pipelineId = pipelineId,
            atomId = null
        )
        val page = pathClass.buildPage(buildPageInfo)
        this.page = page
        val pageList = PageUtils.createAllTagPage(page)
        pageList.forEach {
            val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, it)
            logger.info("[StatusWebsocketPush]-page:$it,sessionList:$sessionList")
            if (sessionList != null) {
                return true
            }
        }
        return false
    }

    override fun mqMessage(): PipelineMqMessage {
        val message = PipelineMqMessage(
            buildId = buildId,
            pipelineId = pipelineId,
            projectId = projectId,
            userId = userId,
            page = page,
            notifyPost = notifyPost,
            pushType = pushType
        )
        return message
    }
}
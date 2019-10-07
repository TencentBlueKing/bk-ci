package com.tencent.devops.common.websocket.dispatch.push

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.IPath
import com.tencent.devops.common.websocket.dispatch.message.AmdMqMessage
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.common.websocket.utils.PageUtils
import com.tencent.devops.common.websocket.utils.RedisUtlis
import org.slf4j.LoggerFactory

@Event(exchange = MQ.EXCHANGE_WEBSOCKET_TMP_FANOUT, routeKey = MQ.ROUTE_WEBSOCKET_TMP_EVENT)
data class AmdWebsocketPush(
    val atomId: String,
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
            buildId = null,
            projectId = null,
            pipelineId = null,
            atomId = atomId
        )
        val page = pathClass.buildPage(buildPageInfo)
        this.page = page
        val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, page)
        logger.info("[AmdWebsocketPush]-page:$page,sessionList:$sessionList")
        if (sessionList != null) {
            return true
        } else {
            // 因研发商店上架、升级两个页面的url不一致。所以需要关联查询。满足其一就需要推消息
            val associationPage = PageUtils.replaceAssociationPage(page)
            if (associationPage != null) {
                val associationPageSessionList =
                    RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, associationPage)
                logger.info("[AmdWebsocketPush]-associationPage:$associationPage,sessionList:$associationPageSessionList")
                if (associationPageSessionList != null) {
                    return true
                }
            }
        }
        return false
    }

    override fun mqMessage(): AmdMqMessage {
        val mqMessage = AmdMqMessage(
            userId = userId,
            page = page,
            atomId = atomId,
            notifyPost = notifyPost,
            pushType = pushType
        )
        return mqMessage
    }
}
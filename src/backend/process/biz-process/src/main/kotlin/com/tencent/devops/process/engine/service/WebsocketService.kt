package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.process.websocket.push.DetailWebsocketPush
import com.tencent.devops.process.websocket.push.HistoryWebsocketPush
import com.tencent.devops.process.websocket.push.StatusWebsocketPush
import com.tencent.devops.process.websocket.page.DetailPageBuild
import com.tencent.devops.process.websocket.page.HistoryPageBuild
import com.tencent.devops.process.websocket.page.StatusPageBuild
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebsocketService @Autowired constructor(
		val redisOperation: RedisOperation,
		val objectMapper: ObjectMapper
) {
	fun buildDetailMessage(buildId:String, projectId: String, pipelineId: String, userId: String): DetailWebsocketPush {
		val page = DetailPageBuild().buildPage(
				buildPageInfo = BuildPageInfo(
						buildId = buildId,
						pipelineId = pipelineId,
						projectId = projectId,
						atomId = null
				))
		return DetailWebsocketPush(
				buildId = buildId,
				projectId = projectId,
				pipelineId = pipelineId,
				userId = userId,
				redisOperation = redisOperation,
				page = page,
				pushType = WebSocketType.DETAIL,
				objectMapper = objectMapper,
				notifyPost = NotifyPost(
						module = "process",
						level = NotityLevel.LOW_LEVEL.getLevel(),
						message = "",
						dealUrl = null,
						code = 200,
						webSocketType = WebSocketType.changWebType(WebSocketType.DETAIL),
						page = page
				)
		)
	}

	fun buildHistoryMessage(buildId:String, projectId: String, pipelineId: String, userId: String): HistoryWebsocketPush {
		val page = HistoryPageBuild().buildPage(
				buildPageInfo = BuildPageInfo(
						buildId = buildId,
						pipelineId = pipelineId,
						projectId = projectId,
						atomId = null
				)
		)
		return HistoryWebsocketPush(
				buildId = buildId,
				projectId = projectId,
				pipelineId = pipelineId,
				userId = userId,
				redisOperation = redisOperation,
				page = page,
				pushType = WebSocketType.HISTORY,
				objectMapper = objectMapper,
				notifyPost = NotifyPost(
						module = "process",
						level = NotityLevel.LOW_LEVEL.getLevel(),
						message = "",
						dealUrl = null,
						code = 200,
						webSocketType = WebSocketType.changWebType(WebSocketType.HISTORY),
						page = page
				)
		)
	}

	fun buildStatusMessage(buildId:String, projectId: String, pipelineId: String, userId: String): StatusWebsocketPush {
		val page = StatusPageBuild().buildPage(
				buildPageInfo = BuildPageInfo(
						buildId = buildId,
						pipelineId = pipelineId,
						projectId = projectId,
						atomId = null
				)
		)
		return StatusWebsocketPush(
				buildId = buildId,
				projectId = projectId,
				pipelineId = pipelineId,
				userId = userId,
				redisOperation = redisOperation,
				page = page,
				pushType = WebSocketType.STATUS,
				objectMapper = objectMapper,
				notifyPost = NotifyPost(
						module = "process",
						level = NotityLevel.LOW_LEVEL.getLevel(),
						message = "",
						dealUrl = null,
						code = 200,
						webSocketType = WebSocketType.changWebType(WebSocketType.STATUS),
						page = page
				)
		)
	}
}
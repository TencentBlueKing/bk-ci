package com.tencent.devops.websocket.servcie

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.utils.RedisUtlis
import com.tencent.devops.project.api.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WebsocketService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    // 用户切换页面，需调整sessionId-page,page-sessionIdList两个map
    fun changePage(userId: String, sessionId: String, newPage: String, projectId: String) {
        if (!checkProject(projectId, userId)) {
            return
        }
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            if (!checkParams(userId, sessionId)) {
                logger.warn("changPage checkFail: userId:$userId,sessionId:$sessionId")
                return
            }

            logger.info("WebsocketService-changePage:user:$userId,sessionId:$sessionId,newPage:$newPage")
            val existsSessionId = RedisUtlis.getSessionIdByUserId(redisOperation, userId)
            if (existsSessionId == null) {
                RedisUtlis.writeSessionIdByRedis(redisOperation, userId, sessionId)
            }

            val oldPage = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
            RedisUtlis.refreshSessionPage(redisOperation, sessionId, newPage)
            if (oldPage != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, oldPage, sessionId)
            }
            RedisUtlis.refreshPageSession(redisOperation, sessionId, newPage)
            logger.info(
                "userSession[user:$userId,sessionId:${RedisUtlis.getSessionIdByUserId(
                    redisOperation,
                    userId
                )}}]"
            )
            logger.info(
                "pageSession[page:$newPage,sessionId:${RedisUtlis.getSessionListFormPageSessionByPage(
                    redisOperation,
                    newPage
                )}]"
            )
            logger.info("sessionPage[session:$sessionId,page:$newPage]")
        } finally {
            redisLock.unlock()
        }
    }

    fun loginOut(userId: String, sessionId: String, oldPage: String?) {
        if (!checkParams(userId, sessionId)) {
            logger.warn("loginOut checkFail: [userId:$userId,sessionId:$sessionId")
            return
        }
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            logger.info("WebsocketService-loginOut:user:$userId,sessionId:$sessionId")
            val page = RedisUtlis.getPageFromSessionPageBySession(redisOperation, sessionId)
            if (!oldPage.isNullOrEmpty() && page != oldPage) {
                logger.warn("loginOut error: oldPage:$oldPage, redisPage:$page, userId:$userId, sessionId:$sessionId")
            }
//            RedisUtlis.cleanUserSessionBySessionId(redisOperation, userId, sessionId)
            RedisUtlis.cleanSessionPageBySessionId(redisOperation, sessionId)
//            RedisUtlis.cleanSessionTimeOutBySession(redisOperation, sessionId)
            if (oldPage != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, oldPage, sessionId)
            } else if (page != null) {
                RedisUtlis.cleanPageSessionBySessionId(redisOperation, page, sessionId)
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun clearUserSession(userId: String, sessionId: String) {
        val redisLock = lockUser(sessionId)
        try {
            redisLock.lock()
            logger.info("clearUserSession:user:$userId,sessionId:$sessionId")
            logger.info("before clearUserSession:${RedisUtlis.getSessionIdByUserId(redisOperation, userId)}")
            RedisUtlis.deleteSigelSessionByUser(redisOperation, userId, sessionId)
            RedisUtlis.cleanSessionTimeOutBySession(redisOperation, sessionId)
            logger.info("after clearUserSession:${RedisUtlis.getSessionIdByUserId(redisOperation, userId)}")
        } finally {
            redisLock.unlock()
        }
    }

    private fun checkParams(userId: String, sessionId: String): Boolean {
        if (userId == null) {
            return false
        }

        if (sessionId == null) {
            return false
        }
        return true
    }

    private fun lockUser(sessionId: String): RedisLock {
        return RedisLock(redisOperation, "websocket:changeStatus:$sessionId", 10L)
    }

    private fun checkProject(projectId: String, userId: String): Boolean {
        try {
            val projectList = client.get(ServiceProjectResource::class).getProjectByUser(userId).data
            val privilegeProjectCodeList = mutableListOf<String>()
            projectList?.map {
                privilegeProjectCodeList.add(it.projectId)
            }
            if (privilegeProjectCodeList.contains(projectId)) {
                return true
            } else {
                logger.warn("changePage checkProject fail, user:$userId,projectId:$projectId,projectList:$projectList")
                return false
            }
        } catch (e: Exception) {
            logger.error("checkProject fail,message:{}", e)
            // 此处为了解耦，假设调用超时，默认还是做changePage的操作
            return true
        }
    }
}
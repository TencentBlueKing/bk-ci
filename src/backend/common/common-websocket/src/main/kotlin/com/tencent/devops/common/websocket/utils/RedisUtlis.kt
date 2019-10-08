package com.tencent.devops.common.websocket.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import java.util.concurrent.TimeUnit

object RedisUtlis {

    // 记录user-session映射。 user: session = 1:N 。同一个user，可以在不同端登录，可能产生多个session
    val USER_SESSION_REDIS_KEY = "BK:webSocket:userId:sessionId:key:"
    // 记录session-page映射。 session: page = 1:1。 同一个session一次只能停留在一个页面。
    val SESSION_PAGE_REDIS_KEY = "BK:webSocket:sessionId:page:key:"
    // 记录page-session映射。  page : session = 1:N。 同一个页面，可能有多个session停留
    val PAGE_SESSION_REDIS_KEY = "BK:webSocket:page:sessionIdList:key:"
    // 记录session-timeout映射。  session : timeout = 1:1。 同一个session，超时于登录后5天。
    val USER_TIMEOUT_REDIS_KEY = "BK:webSocket:sessionId:timeOut:key:"

    // 写入user,session映射。并记录session超时时间。可能出现user:session 一对多的关系
    fun writeSessionIdByRedis(redisOperation: RedisOperation, userId: String, sessionId: String) {
        // 此处可能一个userId多端登录后有对应多个sessionId，但需要通过userId,找到对应的sessionId,故以userId为key
        val oldSession = redisOperation.get(USER_SESSION_REDIS_KEY + userId)

        if (oldSession == null) {
            redisOperation.set(USER_SESSION_REDIS_KEY + userId, sessionId, null, true)
        } else {
            if (!oldSession.contains(sessionId)) {
                val newSessionList = "$oldSession,$sessionId"
                redisOperation.set(USER_SESSION_REDIS_KEY + userId, newSessionList, null, true)
            }
        }
        saveSessionTimeOutBySessionId(redisOperation, sessionId, userId)
    }

    // 获取userId对应的session集合。多个session以“,”隔开，用于做切割
    fun getSessionIdByUserId(redisOperation: RedisOperation, userId: String): String? {
        return redisOperation.get(USER_SESSION_REDIS_KEY + userId)
    }

    // 根据sessionId刷新session-page，用于切换页面
    fun refreshSessionPage(redisOperation: RedisOperation, sessionId: String, page: String) {
        cleanSessionPageBySessionId(redisOperation, sessionId)
        redisOperation.set(SESSION_PAGE_REDIS_KEY + sessionId, page, null, true)
    }

    // 刷新page-sessionIdList 数据。用于用户切换页面。
    fun refreshPageSession(redisOperation: RedisOperation, sessionId: String, newPage: String) {
        val sessionListStr = redisOperation.get(PAGE_SESSION_REDIS_KEY + newPage)
        if (sessionListStr != null) {
            if (!sessionListStr.contains(sessionId)) {
                val newList = "$sessionListStr,$sessionId"
                redisOperation.set(PAGE_SESSION_REDIS_KEY + newPage, newList, null, true)
            }
        } else {
            redisOperation.set(PAGE_SESSION_REDIS_KEY + newPage, sessionId, null, true)
        }
    }

    // 根据sessionId清理sessionId-page对应的记录,sessionId：page = 1:1
    fun cleanSessionPageBySessionId(redisOperation: RedisOperation, sessionId: String) {
        redisOperation.delete(SESSION_PAGE_REDIS_KEY + sessionId)
    }

    // 根据session获取session-page记录，只能获取到一个page
    fun getPageFromSessionPageBySession(redisOperation: RedisOperation, sessionId: String): String? {
        return redisOperation.get(SESSION_PAGE_REDIS_KEY + sessionId)
    }

    // 判断一个session是否有登录页面，用于判断pipeline系列dispatch是否需要push消息到mq
    fun isSessionLoadPage(redisOperation: RedisOperation, sessionId: String): Boolean {
        val page = getPageFromSessionPageBySession(redisOperation, sessionId)
        if (page != null) {
            return true
        }
        return false
    }

    // 根据page获取session集合。可能有多个session
    fun getSessionListFormPageSessionByPage(redisOperation: RedisOperation, page: String): List<String>? {
        val sessionListStr = redisOperation.get(PAGE_SESSION_REDIS_KEY + page)
        if (sessionListStr != null) {
            return sessionListStr.split(",")
        }
        return null
    }

    // 清理page-sessionIdList记录。page：sessionId是一对多的关系。 切换页面或者用户登出需调用此方法
    fun cleanPageSessionBySessionId(redisOperation: RedisOperation, page: String, sessionId: String): Boolean {
        val sessionIdListStr = redisOperation.get(PAGE_SESSION_REDIS_KEY + page)
        if (sessionIdListStr != null) {
            var newSessionList: String? = null
            val sessionIdList = sessionIdListStr.split(",")
            if (sessionIdList.size == 1) {
                redisOperation.delete(PAGE_SESSION_REDIS_KEY + page)
            } else {
                sessionIdList.forEach {
                    if (it != sessionId) {
                        newSessionList = if (newSessionList == null) {
                            it
                        } else {
                            "$newSessionList,$it"
                        }
                    }
                }
                if (!newSessionList.isNullOrEmpty()) {
                    redisOperation.set(PAGE_SESSION_REDIS_KEY + page, newSessionList!!)
                }
            }
        }
        return true
    }

    // 根据page删除redis数据。build结束，需删除改次构建的所有session记录
    fun cleanPageSessionByPage(redisOperation: RedisOperation, page: String) {
        redisOperation.delete(PAGE_SESSION_REDIS_KEY + page)
    }

    // 清理user-sessionId映射内对应的sessionId, 登出需用。
    fun cleanUserSessionBySessionId(redisOperation: RedisOperation, userId: String, sessionId: String): Boolean {
        val sessionIdListStr = redisOperation.get(USER_SESSION_REDIS_KEY + userId)
        if (sessionIdListStr != null) {
            // 存在","说明同一个用户有多个端登录，只需删除对应的SessionId,若没有"，",只有一处登录。直接删除redis数据即可
            if (sessionIdListStr.contains(",")) {
                var newSessionList: String? = null
                val sessionIdlist = sessionIdListStr.split(",")
                sessionIdlist.forEach {
                    if (!it.equals(sessionId)) {
                        if (newSessionList == null) {
                            newSessionList = it
                        } else {
                            newSessionList = "$newSessionList,$it"
                        }
                    }
                }
                if (newSessionList != null) {
                    redisOperation.set(USER_SESSION_REDIS_KEY + userId, newSessionList!!, null, true)
                }
            } else {
                redisOperation.delete(USER_SESSION_REDIS_KEY + userId)
            }
            return true
        }
        return true
    }

    fun deleteAllUserSessionByUser(redisOperation: RedisOperation, userId: String) {
        redisOperation.delete(USER_SESSION_REDIS_KEY + userId)
    }

    fun deleteSigelSessionByUser(redisOperation: RedisOperation, userId: String, sessionId: String) {
        val sessionList = redisOperation.get(USER_SESSION_REDIS_KEY + userId)
        if (!sessionList.isNullOrBlank()) {
            if (sessionList!!.contains(",")) {
                var newSession: String? = null
                sessionList.split(",").forEach {
                    if (!it.equals(sessionId)) {
                        if (newSession == null) {
                            newSession = it
                        } else {
                            newSession = "$newSession,$it"
                        }
                    }
                }
                if (!newSession.isNullOrBlank()) {
                    redisOperation.set(USER_SESSION_REDIS_KEY + userId, newSession!!, null, true)
                }
            } else {
                redisOperation.delete(USER_SESSION_REDIS_KEY + userId)
            }
        }
    }

    fun deleteSessionPageBySession(redisOperation: RedisOperation, sessionId: String) {
        redisOperation.delete(SESSION_PAGE_REDIS_KEY + sessionId)
    }

    fun deletePageSessionByPage(redisOperation: RedisOperation, page: String) {
        redisOperation.delete(PAGE_SESSION_REDIS_KEY + page)
    }

    // 存储session对应的超时时间。所有session统一放到一个大的map内。默认超时时间是5天。此处可以做成可配置。
    fun saveSessionTimeOutBySessionId(redisOperation: RedisOperation, sessionId: String, userId: String) {
        // 默认超时时间，有清理线程定时清理已经超时的sessionId记录，防止在变更时调用loginOut失败。
        val timeout = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5)
        val objectMapper = JsonUtil.getObjectMapper()
        // 此处有一个内存隐患，get出来的map若很大，可能会导致频繁的gc
        val cacheStr = redisOperation.get(USER_TIMEOUT_REDIS_KEY)
        if (cacheStr != null) {
            val cacheMap: MutableMap<String, String> = objectMapper.readValue(cacheStr)
            cacheMap[sessionId] = "$timeout#$userId"
            saveSessionTimeOutAll(redisOperation, objectMapper.writeValueAsString(cacheMap))
        } else {
            val map = mutableMapOf<String, String>()
            map[sessionId] = "$timeout#$userId"
            saveSessionTimeOutAll(redisOperation, objectMapper.writeValueAsString(map))
        }
    }

    // 保存session-timeout数据
    fun saveSessionTimeOutAll(redisOperation: RedisOperation, sessionMap: String) {
        redisOperation.getAndSet(USER_TIMEOUT_REDIS_KEY, sessionMap)
    }

    fun getSessionTimeOutFromRedis(redisOperation: RedisOperation): String? {
        return redisOperation.get(USER_TIMEOUT_REDIS_KEY)
    }

    // 清理超时map内的sessionId，用于loginOut，清理session
    fun cleanSessionTimeOutBySession(redisOperation: RedisOperation, sessionId: String) {
        val allSessionMap = getSessionTimeOutFromRedis(redisOperation)
        if (allSessionMap != null) {
            val objectMapper = JsonUtil.getObjectMapper()
            var sessionMap: MutableMap<String, String> = objectMapper.readValue(allSessionMap)
            if (sessionMap.containsKey(sessionId)) {
                sessionMap.remove(sessionId)
            }
            saveSessionTimeOutAll(redisOperation, objectMapper.writeValueAsString(sessionMap))
        }
    }

    // 构建结束，清理所有再当前页面的websocket缓存。
    fun cleanBuildWebSocketCache(redisOperation: RedisOperation, page: String) {
        val sessionList = RedisUtlis.getSessionListFormPageSessionByPage(redisOperation, page)
        if (sessionList != null && sessionList.size > 0) {
            sessionList.forEach {
                RedisUtlis.cleanSessionPageBySessionId(redisOperation, it)
            }
        }
        RedisUtlis.cleanPageSessionByPage(redisOperation, page)
    }
}
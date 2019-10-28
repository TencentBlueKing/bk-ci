package com.tencent.devops.log.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.dao.v2.IndexDaoV2
import com.tencent.devops.log.util.IndexNameUtils.getIndexName
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

// TODO Remove this
@Service
class V2ProjectService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val indexDaoV2: IndexDaoV2,
    private val indexDao: IndexDao
) {

    companion object {
        private const val ALL_PROJECT_ENABLE = "__devops__all_projects"
        private const val V2_PROJECT_REDIS_KEY = "log:v2:projects:key"
        private val logger = LoggerFactory.getLogger(V2ProjectService::class.java)
    }

    private val projectEnableCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<String/*projectId*/, Boolean/*Enable*/>(
            object : CacheLoader<String, Boolean>() {
                override fun load(projectId: String): Boolean {
                    try {
                        val members = redisOperation.getSetMembers(V2_PROJECT_REDIS_KEY)
                        if (members != null && members.isNotEmpty()) {
                            val enable = members.contains(ALL_PROJECT_ENABLE) || members.contains(projectId)
                            if (enable) {
                                logger.info("[$projectId] The project enable in redis")
                                return enable
                            }
                        }
                    } catch (t: Throwable) {
                        logger.warn("[$projectId] Fail to get the projects from redis", t)
                    }
                    return false
                }
            }
        )

    private val buildEnableCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<String/*BuildId*/, Boolean?>()

    private val buildProjectCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .build<String/*BuildId*/, String/*ProjectId*/>(
            object : CacheLoader<String, String>() {
                override fun load(buildId: String): String {
                    val buildInfo = client.get(ServiceBuildResource::class).serviceBasic(buildId)
                    if (buildInfo.data == null) {
                        logger.warn("[$buildId] Fail to get the build info with response(${buildInfo.status}|${buildInfo.message})")
                        throw RuntimeException("Fail to get the build Info")
                    }
                    return buildInfo.data!!.projectId
                }
            }
        )

    fun buildEnable(buildId: String, projectId: String? = null): Boolean {
        val buildEnable = buildEnableCache.getIfPresent(buildId)
        if (buildEnable != null) {
            return buildEnable
        }
        val redisLock = RedisLock(redisOperation, "log:build:enable:lock:key", 10)
        try {
            val record = indexDaoV2.getBuild(dslContext, buildId)
            if (record != null) {
                logger.info("[$buildId|${record.enable}] The build already exist")
                buildEnableCache.put(buildId, record.enable)
                return record.enable
            }
            // Check from old db
            val oldRecord = indexDao.getIndex(dslContext, buildId)
            if (oldRecord != null) {
                buildEnableCache.put(buildId, false)
                return false
            }
            val pId = if (projectId.isNullOrBlank()) {
                buildProjectCache.get(buildId)
            } else {
                projectId!!
            }
            val enable = projectEnableCache.get(pId)
            logger.info("[$pId|$buildId|$enable] Add the build record")
            saveIndex(dslContext, buildId, enable)
            buildEnableCache.put(buildId, enable)
            return enable
        } catch (t: Throwable) {
            logger.warn("[$buildId] Fail to get the build enable or not", t)
            return false
        } finally {
            redisLock.unlock()
        }
    }

    fun enable(projectId: String): Boolean {
        logger.info("[$projectId] Enable the project")
        val members = redisOperation.getSetMembers(V2_PROJECT_REDIS_KEY)
        if (members != null && members.isNotEmpty()) {
            if (members.contains(ALL_PROJECT_ENABLE) || members.contains(projectId)) {
                return false
            }
        }
        redisOperation.addSetValue(V2_PROJECT_REDIS_KEY, projectId)
        return true
    }

    fun getEnableProjects() = redisOperation.getSetMembers(V2_PROJECT_REDIS_KEY) ?: emptySet()

    fun disable(projectId: String): Boolean {
        logger.info("[$projectId] Disable the project")
        val members = redisOperation.getSetMembers(V2_PROJECT_REDIS_KEY)
        if (members != null && members.isNotEmpty()) {
            // All project enable, can't disable
            if (projectId == ALL_PROJECT_ENABLE) {
                logger.info("Disable all projects")
                redisOperation.delete(V2_PROJECT_REDIS_KEY)
                return true
            }
            if (members.contains(ALL_PROJECT_ENABLE)) {
                return false
            }
            redisOperation.removeSetMember(V2_PROJECT_REDIS_KEY, projectId)
            return true
        }
        return false
    }

    private fun saveIndex(dslContext: DSLContext, buildId: String, enable: Boolean): String {
        val indexName = getIndexName()
        indexDaoV2.create(dslContext, buildId, indexName, enable)
        logger.info("[$buildId|$indexName|$enable] Create new index/type in db and cache")
        return indexName
    }
}
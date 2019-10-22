package com.tencent.devops.lambda.storage

import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.lambda.dao.BuildIndexDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class IndexService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildIndexDao: BuildIndexDao,
    private val redisOperation: RedisOperation
) {
    private val indexCache = CacheBuilder.newBuilder()
        .maximumSize(100000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*buildId*/, String>()

    fun getIndex(buildId: String): String {
        var index = indexCache.getIfPresent(buildId)
        if (index != null) {
            return index
        }
        index = getBuildIndexDB(buildId)
        if (index != null) {
            indexCache.put(buildId, index)
            return index
        }
        val lock = RedisLock(redisOperation, "$ES_INDEX_LOCK:$buildId", 10)
        try {
            lock.lock()
            index = getBuildIndexDB(buildId)
            if (index != null) {
                indexCache.put(buildId, index)
                return index
            }

            index = getIndexName()
            buildIndexDao.create(dslContext, buildId, index)
            indexCache.put(buildId, index)
            return index
        } finally {
            lock.unlock()
        }
    }

    fun updateTime(buildId: String, beginTime: Long, endTime: Long) {
        buildIndexDao.update(dslContext, buildId, beginTime, endTime)
    }

    fun getIndexName(date: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern(LAMBDA_INDEX_DATE_FORMAT)
        return LAMBDA_INDEX_PREFIX + formatter.format(date)
    }

    private fun getBuildIndexDB(buildId: String): String? {
        return buildIndexDao.get(dslContext, buildId)?.indexName
    }

    private fun getIndexName(): String {
        val formatter = DateTimeFormatter.ofPattern(LAMBDA_INDEX_DATE_FORMAT)
        return LAMBDA_INDEX_PREFIX + formatter.format(LocalDateTime.now())
    }

    companion object {
        private const val ES_INDEX_LOCK = "lambda:es:index:lock:key"
        private const val LAMBDA_INDEX_PREFIX = "lambda-build-"
        private const val LAMBDA_INDEX_DATE_FORMAT = "YYYY-MM-dd"
    }
}
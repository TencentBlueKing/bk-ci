package com.tencent.devops.common.webhook.util

import com.tencent.devops.common.webhook.service.code.pojo.EventRepositoryCache
import com.tencent.devops.repository.pojo.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * 如果同一个仓库配置了多个事件触发插件，那么每个插件都会调用一次scm接口，可以将返回值线程级缓存，减少调用次数
 */
object EventCacheUtil {

    private val eventThreadLocalCache =
        ThreadLocal<ConcurrentHashMap<String/* projectId_repository alias name*/, EventRepositoryCache>>()

    fun initEventCache(): ConcurrentHashMap<String, EventRepositoryCache> {
        val cache = ConcurrentHashMap<String, EventRepositoryCache>()
        eventThreadLocalCache.set(cache)
        return cache
    }

    /**
     * 将当前线程绑定到共享缓存，供并发触发任务复用同一次 webhook 事件的仓库查询结果
     */
    fun bindSharedEventCache(cache: ConcurrentHashMap<String, EventRepositoryCache>) {
        eventThreadLocalCache.set(cache)
    }

    fun getEventCache(projectId: String, repo: Repository): EventRepositoryCache? =
        eventThreadLocalCache.get()?.get(repoCacheKey(projectId, repo))

    fun putIfAbsentEventCache(projectId: String, repo: Repository, eventCache: EventRepositoryCache) =
        eventThreadLocalCache.get()?.putIfAbsent(repoCacheKey(projectId, repo), eventCache)

    /**
     * 如果不存在,则初始化空的仓库缓存
     */
    fun getOrInitRepoCache(projectId: String, repo: Repository): EventRepositoryCache? {
        val cache = eventThreadLocalCache.get() ?: return null
        return cache.computeIfAbsent(repoCacheKey(projectId, repo)) { EventRepositoryCache() }
    }

    fun remove() = eventThreadLocalCache.remove()

    fun getAll(): Map<String, EventRepositoryCache> = eventThreadLocalCache.get() ?: emptyMap()

    private fun repoCacheKey(projectId: String, repo: Repository): String = "${projectId}_${repo.aliasName}"
}

package com.tencent.devops.common.webhook.util

import com.tencent.devops.common.webhook.service.code.pojo.EventRepositoryCache
import com.tencent.devops.repository.pojo.Repository

/**
 * 如果同一个仓库配置了多个事件触发插件，那么每个插件都会调用一次scm接口，可以将返回值线程级缓存，减少调用次数
 */
object EventCacheUtil {

    private val eventThreadLocalCache =
        ThreadLocal<MutableMap<String/* repository alias name*/, EventRepositoryCache>>()

    fun initEventCache() = eventThreadLocalCache.set(mutableMapOf())

    fun getEventCache(repo: Repository): EventRepositoryCache? = eventThreadLocalCache.get()?.get(repo.aliasName)

    fun putIfAbsentEventCache(repo: Repository, eventCache: EventRepositoryCache) =
        eventThreadLocalCache.get()?.putIfAbsent(repo.aliasName, eventCache)

    /**
     * 如果不存在,则初始化空的仓库缓存
     */
    fun getOrInitRepoCache(repo: Repository): EventRepositoryCache? {
        return eventThreadLocalCache.get()?.let {
            getEventCache(repo) ?: run {
                val repoCache = EventRepositoryCache()
                eventThreadLocalCache.get()[repo.aliasName] = repoCache
                repoCache
            }
        }
    }

    fun remove() = eventThreadLocalCache.remove()

    fun getAll(): Map<String, EventRepositoryCache> = eventThreadLocalCache.get()
}

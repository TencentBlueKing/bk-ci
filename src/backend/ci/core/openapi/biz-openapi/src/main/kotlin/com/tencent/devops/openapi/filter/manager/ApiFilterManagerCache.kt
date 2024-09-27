package com.tencent.devops.openapi.filter.manager

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.springframework.stereotype.Service

@Service
class ApiFilterManagerCache {
    private val cache: LoadingCache<Class<out ApiFilterManager>, ApiFilterManager> = CacheBuilder.newBuilder()
        .maximumSize(100).build(
            object : CacheLoader<Class<out ApiFilterManager>, ApiFilterManager>() {
                override fun load(clazz: Class<out ApiFilterManager>): ApiFilterManager {
                    return SpringContextUtil.getBean(clazz)
                }
            }
        )

    fun getFilter(clazz: Class<out ApiFilterManager>): ApiFilterManager {
        return cache.get(clazz)
    }
}

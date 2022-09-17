package com.tencent.devops.common.webhook.util

/**
 * 如果同一个仓库配置了多个事件触发插件，那么每个插件都会调用一次scm接口，可以将返回值线程级缓存，减少调用次数
 */
object WebhookCacheUtil {

    private val cache = object : ThreadLocal<MutableMap<String, Any?>>() {
        override fun initialValue(): MutableMap<String, Any?> {
            return mutableMapOf()
        }
    }

    fun clear() = cache.remove()

    private fun containsKey(name: String): Boolean = cache.get().containsKey(name)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any?> get(name: String): T? = cache.get()[name] as T

    fun getAll(): Map<String, Any?> = cache.get()

    fun put(name: String, value: Any?) = cache.get().put(name, value)

    fun <T : Any?> cache(cacheKey: String, action: () -> T): T? {
        if (containsKey(cacheKey)) {
            return get(cacheKey)
        }
        val data = action.invoke()
        put(cacheKey, data)
        return data
    }
}

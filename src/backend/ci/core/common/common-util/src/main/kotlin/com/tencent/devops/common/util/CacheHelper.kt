package com.tencent.devops.common.util

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.TimeUnit

/**
 * 通用缓存助手服务
 * 封装了Caffeine缓存的创建、Key的生成以及 "get-or-load" 的通用逻辑
 */
object CacheHelper {

    /**
     * 创建一个标准的Caffeine缓存实例
     *
     * @param maxSize 最大缓存条目数
     * @param duration 过期时间
     * @param unit 时间单位
     * @return 返回一个配置好的Caffeine Cache实例
     */
    fun <K, V> createCache(
        maxSize: Long = 100000,
        duration: Long = 5,
        unit: TimeUnit = TimeUnit.MINUTES
    ): Cache<K, V> {
        return Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(duration, unit)
            .build()
    }

    /**
     * 核心方法：从缓存获取数据，如果不存在则通过加载器加载并存入缓存。
     * 这是一个高阶函数，接受一个 `loader` lambda作为参数。
     *
     * @param T 缓存值的类型 (泛型)
     * @param cache 要操作的Caffeine缓存实例
     * @param keyParts 用于构建缓存Key的动态参数列表
     * @param loader 缓存未命中时执行的数据加载逻辑，它是一个无参、返回值为T的函数
     * @return 缓存中或新加载的数据
     */
    fun <T : Any> getOrLoad(
        cache: Cache<String, T>,
        vararg keyParts: Any?,
        loader: () -> T
    ): T {
        // 使用 keyParts 构建唯一的缓存键
        val cacheKey = buildCacheKey(*keyParts)
        return cache.get(cacheKey) { loader() }
    }

    /**
     * 将多个部分组合成一个稳定的、唯一的字符串Key。
     * 使用MD5确保Key的长度固定且分布均匀。
     *
     * @param parts 构成Key的各个部分
     * @return MD5哈希后的字符串Key
     */
    fun buildCacheKey(vararg parts: Any?): String {
        // 过滤掉null值，并用'|'连接，确保不同参数顺序/数量生成不同key
        val key = parts.filterNotNull().joinToString("|")
        return DigestUtils.md5Hex(key)
    }
}

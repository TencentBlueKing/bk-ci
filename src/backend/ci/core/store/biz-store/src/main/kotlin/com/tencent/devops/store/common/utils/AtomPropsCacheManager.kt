/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.common.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import com.tencent.devops.store.pojo.common.STORE_ATOM_TASK_JSON_KEY_PREFIX
import org.slf4j.LoggerFactory

/**
 * 插件参数定义（task.json 的 input 部分）缓存管理器
 *
 * 缓存内容为插件 task.json 中 input 参数的完整定义（含 default/type/label/options/rely 等），
 * 便于后续基于同一份缓存扩展其他能力。
 *
 * 存储结构为 Redis Hash：key = 前缀:插件标识，field = 版本号，value = 该版本 input 参数定义的 JSON。
 * 这样既可按版本粒度读写（避免单个大 value 反复全量读写），
 * 又能在插件更新时通过删除整个 key 一次性失效该插件所有版本。
 *
 * 缓存设置一天兜底过期时间，插件 PROPS 变更时主动失效；缓存不可用时降级为回源数据库。
 */
object AtomPropsCacheManager {
    private val logger = LoggerFactory.getLogger(AtomPropsCacheManager::class.java)
    // Redis 缓存兜底过期时间（秒）。
    private const val REDIS_EXPIRE_SECONDS = 24 * 60 * 60L

    /**
     * 批量获取插件参数定义缓存。
     *
     * @return key 为「插件标识@版本」，value 为该版本 input 参数定义；未命中缓存的版本不出现在结果中。
     */
    fun batchGetAtomInputProps(
        redisOperation: RedisOperation,
        params: List<ElementThirdPartySearchParam>
    ): Map<String, Map<String, Any>> {
        val result = mutableMapOf<String, Map<String, Any>>()
        params.groupBy({ it.atomCode }, { it.version }).forEach { (atomCode, versions) ->
            val distinctVersions = versions.distinct()
            val values = try {
                redisOperation.hmGet(getAtomTaskJsonKey(atomCode), distinctVersions)
            } catch (ignored: Throwable) {
                logger.warn("get atom input props from redis failed|$atomCode", ignored)
                null
            } ?: return@forEach
            distinctVersions.forEachIndexed { index, version ->
                val value = values.getOrNull(index)
                if (value.isNullOrBlank()) return@forEachIndexed
                val input = try {
                    JsonUtil.toMap(value)
                } catch (ignored: Throwable) {
                    logger.warn("parse atom input props failed|$atomCode@$version", ignored)
                    null
                } ?: return@forEachIndexed
                result["$atomCode@$version"] = input
            }
        }
        return result
    }

    /**
     * 回填指定版本的插件参数定义。
     */
    fun putAtomInputProps(
        redisOperation: RedisOperation,
        atomCode: String,
        version: String,
        input: Map<String, Any>
    ) {
        try {
            val key = getAtomTaskJsonKey(atomCode)
            redisOperation.hset(key, version, JsonUtil.toJson(input, formatted = false))
            redisOperation.expire(key, REDIS_EXPIRE_SECONDS)
        } catch (ignored: Throwable) {
            logger.warn("cache atom input props failed|$atomCode@$version", ignored)
        }
    }

    /**
     * 失效插件的参数定义缓存，插件 PROPS 变更后调用。
     */
    fun invalidate(redisOperation: RedisOperation, atomCode: String) {
        try {
            redisOperation.delete(getAtomTaskJsonKey(atomCode))
        } catch (ignored: Throwable) {
            logger.warn("invalidate atom input props cache failed|$atomCode", ignored)
        }
    }

    private fun getAtomTaskJsonKey(atomCode: String) = "$STORE_ATOM_TASK_JSON_KEY_PREFIX:$atomCode"
}

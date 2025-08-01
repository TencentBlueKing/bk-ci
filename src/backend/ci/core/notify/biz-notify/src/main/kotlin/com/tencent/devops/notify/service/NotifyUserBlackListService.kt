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

package com.tencent.devops.notify.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.notify.dao.NotifyUserBlacklistDao
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class NotifyUserBlackListService @Autowired constructor(
    val dslContext: DSLContext,
    val notifyUserBlacklistDao: NotifyUserBlacklistDao
) {

    @Value("\${notify.userBlackList.cacheSize:50000}")
    val notifyUserBlackListCacheSize: Long = 50000

    companion object {
        val logger = LoggerFactory.getLogger(NotifyUserBlackListService::class.java)
        const val NOTIFY_USER_BLACK_LIST_CACHE_KEY_PREFIX = "notify_user_blacklist_cache_key_"
    }

    private val notifyUserBlackListCache = Caffeine.newBuilder()
        .maximumSize(notifyUserBlackListCacheSize)
        .expireAfterWrite(3, TimeUnit.HOURS)
        .build<String, String>()

    /**
     * 批量添加用户到黑名单
     * @param userIds 用户ID列表
     * @return 成功添加的数量
     */
    fun batchAddToBlacklist(userIds: List<String>): Boolean {
        return try {
            val count = notifyUserBlacklistDao.batchAddToBlacklist(dslContext, userIds)
            if (count > 0) {
                // 清除相关用户缓存
                userIds.forEach { userId ->
                    notifyUserBlackListCache.invalidate(NOTIFY_USER_BLACK_LIST_CACHE_KEY_PREFIX + userId)
                }
            }
            true
        } catch (ignored: Throwable) {
            logger.warn("Failed to batch add users to blacklist: $userIds，${ignored.message}")
            false
        }
    }

    /**
     * 批量移除黑名单用户
     * @param userIds 用户ID列表
     * @return 成功移除的数量
     */
    fun batchRemoveFromBlacklist(userIds: List<String>): Boolean {
        return try {
            val count = notifyUserBlacklistDao.batchRemoveFromBlacklist(dslContext, userIds)
            if (count > 0) {
                // 清除相关用户缓存
                userIds.forEach { userId ->
                    notifyUserBlackListCache.invalidate(NOTIFY_USER_BLACK_LIST_CACHE_KEY_PREFIX + userId)
                }
            }
            true
        } catch (ignored: Throwable) {
            logger.warn("Failed to batch remove users from blacklist: $userIds，${ignored.message}")
            false
        }
    }

    /**
     * 获取指定用户的黑名单
     * @param userId 用户ID
     * @return 黑名单用户列表
     */
    fun getBlacklistUser(userId: String): String? {
        try {
            return notifyUserBlackListCache.get(NOTIFY_USER_BLACK_LIST_CACHE_KEY_PREFIX + userId) {
                logger.info("Updating blacklist cache for user: $userId")
                notifyUserBlacklistDao.getBlacklistForUser(dslContext, userId)
            }
        } catch (ignored: Throwable) {
            logger.warn("Failed to get blacklist for user $userId，${ignored.message}")
        }
        return null
    }

    /**
     * 分页获取黑名单用户
     * @return Pair<总数量, 当前页用户列表>
     */
    fun getBlacklistByPage(page: Int = 1, pageSize: Int = 100): Page<String> {
        val (totalCount, records) = notifyUserBlacklistDao.listBlacklistUsersByPage(dslContext, page, pageSize)
        val totalPages = PageUtil.calTotalPage(pageSize, totalCount)
        return Page(
            count = totalCount,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            records = records
        )
    }
}

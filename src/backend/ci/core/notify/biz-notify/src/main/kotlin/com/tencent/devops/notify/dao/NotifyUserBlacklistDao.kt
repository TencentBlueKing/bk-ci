/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
package com.tencent.devops.notify.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.notify.tables.TNotifyUserBlacklist
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class NotifyUserBlacklistDao {

    /**
     * 批量添加用户到通知黑名单
     * @param userIds 用户ID列表
     * @return 成功添加的数量
     */
    fun batchAddToBlacklist(
        dslContext: DSLContext,
        userIds: List<String>
    ): Int {
        val table = TNotifyUserBlacklist.T_NOTIFY_USER_BLACKLIST
        with(table) {
            if (userIds.isEmpty()) return 0

            val existingUsers = listAllBlacklistUsers(dslContext)
            val newUsers = userIds.filter { !existingUsers.contains(it) }
            if (newUsers.isEmpty()) return 0

            val now = LocalDateTime.now()
            val queries = newUsers.map { userId ->
                dslContext.insertInto(
                    this,
                    ID,
                    USER_ID,
                    CREATE_TIME
                ).values(
                    UUIDUtil.generate(),
                    userId,
                    now
                )
            }

            return dslContext.batch(queries).execute().sum()
        }
    }

    /**
     * 批量移除黑名单用户
     * @param userIds 用户ID列表
     * @return 成功移除的数量
     */
    fun batchRemoveFromBlacklist(
        dslContext: DSLContext,
        userIds: List<String>
    ): Int {
        val table = TNotifyUserBlacklist.T_NOTIFY_USER_BLACKLIST
        with(table) {
            if (userIds.isEmpty()) return 0

            return dslContext.deleteFrom(table)
                .where(table.USER_ID.`in`(userIds))
                .execute()
        }
    }

    /**
     * 获取所有黑名单用户
     * @return 黑名单用户记录列表
     */
    fun listAllBlacklistUsers(
        dslContext: DSLContext
    ): List<String> {
        val table = TNotifyUserBlacklist.T_NOTIFY_USER_BLACKLIST
        with(table) {
            return dslContext.select(table.USER_ID)
                .from(table)
                .fetchInto(String::class.java)
        }
    }

    /**
     * 分页获取黑名单用户
     * @return Pair<总数量, 当前页用户列表>
     */
    fun listBlacklistUsersByPage(
        dslContext: DSLContext,
        page: Int,
        pageSize: Int
    ): Pair<Long, List<String>> {
        val table = TNotifyUserBlacklist.T_NOTIFY_USER_BLACKLIST
        with(table) {
            val total = dslContext.selectCount()
                .from(table)
                .fetchOne(0, Long::class.java) ?: 0L

            val users = dslContext.select(table.USER_ID)
                .from(table)
                .limit((page - 1) * pageSize, pageSize)
                .fetchInto(String::class.java)

            return Pair(total, users)
        }
    }
}

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

package com.tencent.devops.store.atom.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.store.tables.TAtomWhitelist
import com.tencent.devops.store.pojo.common.AtomWhitelist
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AtomWhitelistDao {

    /**
     * 根据白名单类型查询所有启用的插件code
     * @param dslContext DSL上下文
     * @param whitelistType 白名单类型
     * @return 插件code列表
     */
    fun getAtomCodesByType(
        dslContext: DSLContext,
        whitelistType: String
    ): List<String> {
        val t = TAtomWhitelist.T_ATOM_WHITELIST
        val record = dslContext.select(t.ATOM_CODES)
            .from(t)
            .where(t.WHITELIST_TYPE.eq(whitelistType).and(t.ENABLED.eq(true)))
            .fetchOne()

        return if (record != null) {
            val jsonArray = record.get(0) as? String ?: "[]"
            JsonUtil.to(jsonArray, object : TypeReference<List<String>>() {})
        } else {
            emptyList()
        }
    }

    /**
     * 添加白名单记录
     * @param dslContext DSL上下文
     * @param whitelistType 白名单类型
     * @param atomCodes 插件code列表
     * @param description 描述
     * @param userId 用户ID
     * @return 插入的行数
     */
    fun addWhitelist(
        dslContext: DSLContext,
        whitelistType: String,
        atomCodes: List<String>,
        description: String?,
        userId: String
    ): Int {
        val atomCodesJson = JsonUtil.toJson(atomCodes, formatted = false)
        val now = LocalDateTime.now()
        with(TAtomWhitelist.T_ATOM_WHITELIST) {
            return dslContext.insertInto(
                this,
                WHITELIST_TYPE,
                ATOM_CODES,
                ENABLED,
                DESCRIPTION,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                whitelistType,
                atomCodesJson,
                true,
                description,
                userId,
                userId,
                now,
                now
            ).execute()
        }
    }

    fun updateAtomCodes(
        dslContext: DSLContext,
        whitelistType: String,
        atomCodes: List<String>,
        description: String?,
        userId: String
    ): Boolean {
        val t = TAtomWhitelist.T_ATOM_WHITELIST
        return dslContext.update(t)
            .set(t.ATOM_CODES, JsonUtil.toJson(atomCodes, formatted = false))
            .set(t.DESCRIPTION, description)
            .set(t.MODIFIER, userId)
            .set(t.UPDATE_TIME, LocalDateTime.now())
            .where(t.WHITELIST_TYPE.eq(whitelistType))
            .execute() > 0
    }

    /**
     * 删除白名单记录
     * @param dslContext DSL上下文
     * @param whitelistType 白名单类型
     * @return 是否删除成功
     */
    fun deleteWhitelist(dslContext: DSLContext, whitelistType: String): Boolean {
        val t = TAtomWhitelist.T_ATOM_WHITELIST
        val deletedRows = dslContext.deleteFrom(t)
            .where(t.WHITELIST_TYPE.eq(whitelistType))
            .execute()

        return deletedRows > 0
    }

    /**
     * 更新白名单记录启用状态
     * @param dslContext DSL上下文
     * @param whitelistType 白名单类型
     * @param enabled 是否启用
     * @param userId 用户ID
     * @return 是否更新成功
     */
    fun updateWhitelistStatus(
        dslContext: DSLContext,
        whitelistType: String,
        enabled: Boolean,
        userId: String
    ): Boolean {
        val t = TAtomWhitelist.T_ATOM_WHITELIST
        val now = LocalDateTime.now()

        val updatedRows = dslContext.update(t)
            .set(t.ENABLED, enabled)
            .set(t.MODIFIER, userId)
            .set(t.UPDATE_TIME, now)
            .where(t.WHITELIST_TYPE.eq(whitelistType))
            .execute()

        return updatedRows > 0
    }

    /**
     * 查询白名单记录
     * @param dslContext DSL上下文
     * @param whitelistType 白名单类型（可选）
     * @param enabled 是否启用（可选）
     * @param page 页码
     * @param pageSize 每页大小
     * @return 白名单记录列表
     */
    fun listWhitelists(
        dslContext: DSLContext,
        whitelistType: String? = null,
        enabled: Boolean? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): List<AtomWhitelist> {
        val t = TAtomWhitelist.T_ATOM_WHITELIST
        val conditions = mutableListOf<Condition>()

        if (!whitelistType.isNullOrBlank()) {
            conditions.add(t.WHITELIST_TYPE.eq(whitelistType))
        }

        if (enabled != null) {
            conditions.add(t.ENABLED.eq(enabled))
        }

        val records = dslContext.select()
            .from(t)
            .where(conditions)
            .orderBy(t.CREATE_TIME.desc())
            .limit(pageSize)
            .offset((page - 1) * pageSize)
            .fetch()

        return records.map { record ->
            AtomWhitelist(
                whitelistType = record.get(t.WHITELIST_TYPE),
                atomCodes = parseAtomCodes(record.get(t.ATOM_CODES)),
                description = record.get(t.DESCRIPTION),
                enabled = record.get(t.ENABLED) ?: false,
                creator = record.get(t.CREATOR),
                createTime = record.get(t.CREATE_TIME),
                modifier = record.get(t.MODIFIER),
                updateTime = record.get(t.UPDATE_TIME)
            )
        }
    }

    private fun parseAtomCodes(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return JsonUtil.to(jsonStr, object : TypeReference<List<String>>() {})
    }

    /**
     * 统计白名单记录数量
     * @param dslContext DSL上下文
     * @param whitelistType 白名单类型（可选）
     * @param enabled 是否启用（可选）
     * @return 记录数量
     */
    fun countWhitelists(
        dslContext: DSLContext,
        whitelistType: String? = null,
        enabled: Boolean? = null
    ): Long {
        val t = TAtomWhitelist.T_ATOM_WHITELIST
        val conditions = mutableListOf<Condition>()

        if (!whitelistType.isNullOrBlank()) {
            conditions.add(t.WHITELIST_TYPE.eq(whitelistType))
        }

        if (enabled != null) {
            conditions.add(t.ENABLED.eq(enabled))
        }

        return dslContext.selectCount()
            .from(t)
            .where(conditions)
            .fetchOne(0, Long::class.java) ?: 0L
    }
}
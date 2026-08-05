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

package com.tencent.devops.process.dao.`var`

import com.tencent.devops.model.process.tables.TResourcePublicVarGroupVersionSummary
import com.tencent.devops.process.constant.ProcessConstants.DYNAMIC_VERSION
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupVersionSummaryPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 公共变量组版本概要（引用数）DAO。
 *
 * 设计原则：只提供"绝对值写入 + 读取"，不提供增量增减（increment/decrement）。
 * 计数由 PublicVarGroupReferManageService 在引用变更所在事务内，从引用明细表
 * T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO 重算后覆盖写入，从根本上杜绝旧增量方案的误差累积/漂移。
 */
@Repository
class PublicVarGroupVersionSummaryDao {

    /**
     * 绝对值 upsert：不存在则插入，存在则用 po.referCount 覆盖（不做累加）。
     * 用于重算后写入新出现的 (变量组, 版本) 行；并发下若行已被其他事务创建则退化为覆盖更新。
     */
    fun save(
        dslContext: DSLContext,
        po: PublicVarGroupVersionSummaryPO
    ) {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            dslContext.insertInto(this)
                .set(ID, po.id)
                .set(PROJECT_ID, po.projectId)
                .set(GROUP_NAME, po.groupName)
                .set(VERSION, po.version)
                .set(REFER_COUNT, po.referCount)
                .set(CREATOR, po.creator)
                .set(MODIFIER, po.modifier)
                .set(CREATE_TIME, po.createTime)
                .set(UPDATE_TIME, po.updateTime)
                .onDuplicateKeyUpdate()
                .set(REFER_COUNT, po.referCount)
                .set(MODIFIER, po.modifier)
                .set(UPDATE_TIME, po.updateTime)
                .execute()
        }
    }

    /**
     * 用绝对值覆盖更新引用数（不做累加）。返回受影响行数：0 表示行不存在，需走 [save] 插入。
     */
    fun updateReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        referCount: Int,
        modifier: String
    ): Int {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.update(this)
                .set(REFER_COUNT, referCount)
                .set(MODIFIER, modifier)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    /**
     * 获取变量组所有版本的引用计数总和（固定版本 + 动态版本），用于删除保护。
     */
    fun getTotalReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ): Int {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.select(REFER_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 批量获取变量组的引用计数总和。
     */
    fun batchGetTotalReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.select(GROUP_NAME, REFER_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .groupBy(GROUP_NAME)
                .fetch()
                .associate { it.value1() to (it.value2()?.toInt() ?: 0) }
        }
    }

    /**
     * 批量获取变量组的动态版本引用计数（VERSION = -1）。
     */
    fun batchGetDynamicVersionReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.select(GROUP_NAME, REFER_COUNT)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .and(VERSION.eq(DYNAMIC_VERSION))
                .fetch()
                .associate { it.value1() to (it.value2() ?: 0) }
        }
    }

    /**
     * 批量获取变量组的固定版本引用计数总和（VERSION <> -1）。
     */
    fun batchGetFixedVersionReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.select(GROUP_NAME, REFER_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .and(VERSION.ne(DYNAMIC_VERSION))
                .groupBy(GROUP_NAME)
                .fetch()
                .associate { it.value1() to (it.value2()?.toInt() ?: 0) }
        }
    }

    /**
     * 删除变量组下、版本不在 keepVersions 中的所有概要行。
     * 用于重算后清理"引用数已归零"的版本行；keepVersions 为空表示该组已无任何生效引用，删除全部行。
     */
    fun deleteByGroupNameExceptVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        keepVersions: Collection<Int>
    ) {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            val delete = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
            if (keepVersions.isNotEmpty()) {
                delete.and(VERSION.notIn(keepVersions))
            }
            delete.execute()
        }
    }

    /**
     * 删除变量组的所有版本概要信息（变量组被删除时清理）。
     */
    fun deleteByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ) {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }
}

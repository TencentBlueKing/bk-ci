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
import com.tencent.devops.model.process.tables.records.TResourcePublicVarGroupVersionSummaryRecord
import com.tencent.devops.process.constant.ProcessMessageCode.DYNAMIC_VERSION
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupVersionSummaryPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupVersionSummaryDao {

    private fun mapRecordToPO(record: TResourcePublicVarGroupVersionSummaryRecord): PublicVarGroupVersionSummaryPO {
        return PublicVarGroupVersionSummaryPO(
            id = record.id,
            projectId = record.projectId,
            groupName = record.groupName,
            version = record.version,
            referCount = record.referCount,
            creator = record.creator,
            modifier = record.modifier,
            createTime = record.createTime,
            updateTime = record.updateTime
        )
    }

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
     * 根据项目ID、变量组名和版本号查询记录
     */
    fun getByGroupNameAndVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int
    ): PublicVarGroupVersionSummaryPO? {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .fetchOne()?.let { mapRecordToPO(it) }
        }
    }

    /**
     * 获取变量组的引用计数（指定版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（-1表示动态版本）
     * @return 引用计数
     */
    fun getReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int
    ): Int {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            return dslContext.select(REFER_COUNT)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 获取变量组所有版本的引用计数总和（固定版本 + 动态版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @return 引用计数总和
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
     * 更新引用计数
     */
    fun updateReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        referCount: Int,
        modifier: String
    ) {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            dslContext.update(this)
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
     * 增量更新引用计数（支持增加或减少）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号（-1表示动态版本）
     * @param countChange 变化数量（正数增加，负数减少）
     * @param modifier 修改人
     * @return 更新的行数
     */
    fun incrementReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int,
        modifier: String
    ): Int {
        with(TResourcePublicVarGroupVersionSummary.T_RESOURCE_PUBLIC_VAR_GROUP_VERSION_SUMMARY) {
            val condition = PROJECT_ID.eq(projectId)
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))

            // 如果是减少操作，添加条件确保结果不会为负
            val finalCondition = if (countChange < 0) {
                condition.and(REFER_COUNT.plus(countChange).ge(0))
            } else {
                condition
            }

            return dslContext.update(this)
                .set(REFER_COUNT, REFER_COUNT.plus(countChange))
                .set(MODIFIER, modifier)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(finalCondition)
                .execute()
        }
    }

    /**
     * 批量获取变量组的引用计数总和
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名称列表
     * @return Map<变量组名, 引用计数总和>
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
                .associate { record ->
                    record.value1() to (record.value2()?.toInt() ?: 0)
                }
        }
    }

    /**
     * 批量获取变量组的动态版本引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名称列表
     * @return Map<变量组名, 动态版本引用计数>
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
                .associate { record ->
                    record.value1() to (record.value2() ?: 0)
                }
        }
    }

    /**
     * 批量获取变量组的固定版本引用计数总和
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名称列表
     * @return Map<变量组名, 固定版本引用计数总和>
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
                .associate { record ->
                    record.value1() to (record.value2()?.toInt() ?: 0)
                }
        }
    }

    /**
     * 删除变量组的所有版本概要信息
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

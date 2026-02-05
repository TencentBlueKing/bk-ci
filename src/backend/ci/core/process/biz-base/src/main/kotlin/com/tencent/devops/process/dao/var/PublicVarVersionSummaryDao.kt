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

import com.tencent.devops.model.process.tables.TResourcePublicVarVersionSummary
import com.tencent.devops.model.process.tables.records.TResourcePublicVarVersionSummaryRecord
import com.tencent.devops.process.pojo.`var`.po.PublicVarVersionSummaryPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarVersionSummaryDao {

    private fun mapRecordToPO(record: TResourcePublicVarVersionSummaryRecord): PublicVarVersionSummaryPO {
        return PublicVarVersionSummaryPO(
            id = record.id,
            projectId = record.projectId,
            groupName = record.groupName,
            varName = record.varName,
            version = record.version,
            referCount = record.referCount,
            creator = record.creator,
            modifier = record.modifier,
            createTime = record.createTime,
            updateTime = record.updateTime
        )
    }

    /**
     * 保存变量版本摘要记录
     */
    fun save(
        dslContext: DSLContext,
        po: PublicVarVersionSummaryPO
    ) {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            dslContext.insertInto(this)
                .set(ID, po.id)
                .set(PROJECT_ID, po.projectId)
                .set(GROUP_NAME, po.groupName)
                .set(VAR_NAME, po.varName)
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
     * 根据项目ID、变量组名、变量名和版本号查询记录
     */
    fun getByVarNameAndVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int
    ): PublicVarVersionSummaryPO? {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
                .and(VERSION.eq(version))
                .fetchOne()?.let { mapRecordToPO(it) }
        }
    }

    /**
     * 获取变量所有版本的引用计数总和
     */
    fun getTotalReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String
    ): Int {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            return dslContext.select(REFER_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
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
        varName: String,
        version: Int,
        referCount: Int,
        modifier: String
    ) {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            dslContext.update(this)
                .set(REFER_COUNT, referCount)
                .set(MODIFIER, modifier)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    /**
     * 增量更新引用计数（支持增加或减少）
     */
    fun incrementReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int,
        countChange: Int,
        modifier: String
    ): Int {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            val condition = PROJECT_ID.eq(projectId)
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
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
     * 批量获取变量的引用计数总和
     */
    fun batchGetTotalReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) return emptyMap()

        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            return dslContext.select(VAR_NAME, REFER_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.`in`(varNames))
                .groupBy(VAR_NAME)
                .fetch()
                .associate { record ->
                    record.value1() to (record.value2()?.toInt() ?: 0)
                }
        }
    }

    /**
     * 根据变量名和版本批量获取引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 版本号
     * @param varNames 变量名列表
     * @return varName -> referCount 的映射，不存在的变量返回0
     */
    fun batchGetReferCountByVarNames(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) return emptyMap()

        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            return dslContext.select(VAR_NAME, REFER_COUNT)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.`in`(varNames))
                .fetch()
                .associate { record ->
                    record.value1() to (record.value2()?.toInt() ?: 0)
                }
        }
    }

    /**
     * 删除变量的所有版本概要信息
     */
    fun deleteByVarName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String
    ) {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
                .execute()
        }
    }

    /**
     * 删除变量组下所有变量的概要信息
     */
    fun deleteByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ) {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }
}

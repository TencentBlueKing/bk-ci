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

import com.tencent.devops.model.process.tables.TResourcePublicVarGroupReferInfo
import com.tencent.devops.model.process.tables.TResourcePublicVarReferInfo
import com.tencent.devops.model.process.tables.TResourcePublicVarVersionSummary
import com.tencent.devops.model.process.tables.records.TResourcePublicVarVersionSummaryRecord
import com.tencent.devops.process.constant.ProcessMessageCode.DYNAMIC_VERSION
import com.tencent.devops.process.pojo.`var`.po.PublicVarVersionSummaryPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
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
     * 保存变量版本摘要记录（仅用于保证 (project, group, var, version) 行存在）。
     *
     * 注意：`REFER_COUNT` 字段自方案 4 起不再由代码维护，读路径全部走
     * [batchGetActiveReferCount] / [batchGetReferCountByVarNames] 实时 JOIN 聚合。
     * 本方法写入 `referCount` 仅用于新建行时填充初始值（通常传 0），不保证后续准确性。
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
     * 批量获取变量的"当前有效引用计数"（实时聚合语义）。
     *
     * 语义：统计每个变量被多少不同流水线/模板**当前最新版本**（草稿优先、无草稿则为已发布最新版）
     * 通过"动态版本 / 变量组最新版本"方式引用。
     *
     * 实现原理（不依赖 T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY 的 REFER_COUNT 缓存字段）：
     * - JOIN `T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO.LATEST_FLAG = true` 过滤，只保留每个 referId
     *   当前最新有效版本的行
     * - 对 T_RESOURCE_PUBLIC_VAR_REFER_INFO 按 `VERSION IN (-1, latestVersion)` 筛选（动态版本 +
     *   变量组当前最新版本；pin 在历史版本视为已脱节，不计入）
     * - COUNT(DISTINCT REFER_ID) 避免同 referId 跨动态/固定版本重复计数
     *
     * 这样草稿里增删引用表达式会立刻反映到 count，正式版旧引用不会被误算。
     *
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param latestVersion 变量组当前最新版本号（LATEST_FLAG=true 的那条记录对应的 VERSION）
     * @param varNames 变量名列表
     * @return Map<varName, referCount>
     */
    fun batchGetActiveReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        latestVersion: Int,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) return emptyMap()

        val r = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO
        val g = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO

        val countField = DSL.countDistinct(r.REFER_ID)
        return dslContext.select(r.VAR_NAME, countField)
            .from(r)
            .innerJoin(g)
            .on(g.PROJECT_ID.eq(r.PROJECT_ID))
            .and(g.REFER_ID.eq(r.REFER_ID))
            .and(g.REFER_TYPE.eq(r.REFER_TYPE))
            .and(g.GROUP_NAME.eq(r.GROUP_NAME))
            .and(g.REFER_VERSION.eq(r.REFER_VERSION))
            .and(g.LATEST_FLAG.eq(true))
            .where(r.PROJECT_ID.eq(projectId))
            .and(r.GROUP_NAME.eq(groupName))
            .and(r.VAR_NAME.`in`(varNames))
            .and(r.VERSION.`in`(DYNAMIC_VERSION, latestVersion))
            .groupBy(r.VAR_NAME)
            .fetch()
            .associate { record ->
                record.getValue(r.VAR_NAME) to (record.get(countField) ?: 0)
            }
    }

    /**
     * 根据变量名和指定版本批量获取引用计数（实时聚合语义）。
     *
     * 用于查看变量组某个具体版本下的引用情况：统计"当前最新版本在变量组 version=X 下引用该变量"的流水线数。
     * 同样依赖 `T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO.LATEST_FLAG = true` 过滤，保证只算"流水线的
     * 当前最新版本"（草稿优先）对该版本的 pin 引用。
     *
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param version 变量组版本号
     * @param varNames 变量名列表
     * @return varName -> referCount 的映射
     */
    fun batchGetReferCountByVarNames(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) return emptyMap()

        val r = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO
        val g = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO

        val countField = DSL.countDistinct(r.REFER_ID)
        return dslContext.select(r.VAR_NAME, countField)
            .from(r)
            .innerJoin(g)
            .on(g.PROJECT_ID.eq(r.PROJECT_ID))
            .and(g.REFER_ID.eq(r.REFER_ID))
            .and(g.REFER_TYPE.eq(r.REFER_TYPE))
            .and(g.GROUP_NAME.eq(r.GROUP_NAME))
            .and(g.REFER_VERSION.eq(r.REFER_VERSION))
            .and(g.LATEST_FLAG.eq(true))
            .where(r.PROJECT_ID.eq(projectId))
            .and(r.GROUP_NAME.eq(groupName))
            .and(r.VAR_NAME.`in`(varNames))
            .and(r.VERSION.eq(version))
            .groupBy(r.VAR_NAME)
            .fetch()
            .associate { record ->
                record.getValue(r.VAR_NAME) to (record.get(countField) ?: 0)
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

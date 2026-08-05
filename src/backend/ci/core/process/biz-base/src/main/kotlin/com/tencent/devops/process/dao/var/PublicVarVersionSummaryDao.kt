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
import com.tencent.devops.process.constant.ProcessConstants.DYNAMIC_VERSION
import com.tencent.devops.process.pojo.`var`.po.PublicVarVersionSummaryPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

/**
 * 公共变量（变量级）版本概要（引用数）DAO。
 *
 * 与组级 [PublicVarGroupVersionSummaryDao] 一致：只提供"绝对值写入 + 读取"，不做增量增减。
 * 计数由 PublicVarGroupReferManageService.refreshVarLevelSummary 从引用明细
 * （变量引用表 JOIN 变量组引用表 LATEST_FLAG=true）重算后覆盖写入，保证与明细强一致。
 * 关键：变量引用明细（T_RESOURCE_PUBLIC_VAR_REFER_INFO）在保存链路经 MQ 异步写入，
 * 因此变量级重算放在【写入变量引用明细的同一事务内】（PublicVarReferInfoService 异步链路）与删除事务内，
 * 而不是组级引用保存事务内——否则会读到尚未写入的旧明细，导致概要长期滞后一次保存。
 * 读路径直接点查本表，替换原先的实时 JOIN 聚合。
 */
@Repository
class PublicVarVersionSummaryDao {

    /**
     * 重算权威来源：按 (变量名, 版本) 聚合当前有效引用数。
     * JOIN 变量组引用表并过滤 LATEST_FLAG=true（只算每个 referId 当前生效版本），按 REFER_ID 去重。
     * 返回 Map<(varName, version), 引用数>，version=-1 表示动态版本。
     */
    fun getLatestReferCountByVarAndVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ): Map<Pair<String, Int>, Int> {
        val r = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO
        val g = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val countField = DSL.countDistinct(r.REFER_ID)
        return dslContext.select(r.VAR_NAME, r.VERSION, countField)
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
            .groupBy(r.VAR_NAME, r.VERSION)
            .fetch()
            .associate { record ->
                (record.getValue(r.VAR_NAME) to record.getValue(r.VERSION)) to (record.get(countField) ?: 0)
            }
    }

    /**
     * 绝对值 upsert：不存在则插入，存在则用 po.referCount 覆盖（不做累加）。
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
     * 用绝对值覆盖更新引用数（不做累加）。返回受影响行数：0 表示行不存在，需走 [save] 插入。
     */
    fun updateReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int,
        referCount: Int,
        modifier: String
    ): Int {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            return dslContext.update(this)
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
     * 批量获取变量的"当前有效引用计数"（动态版本 -1 + 变量组最新版本 之和），改为点查本概要表。
     * 语义与原实时聚合一致：pin 在历史版本的引用视为已脱节、不计入。
     */
    fun batchGetActiveReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        latestVersion: Int,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) return emptyMap()
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            return dslContext.select(VAR_NAME, REFER_COUNT.sum())
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.`in`(varNames))
                .and(VERSION.`in`(DYNAMIC_VERSION, latestVersion))
                .groupBy(VAR_NAME)
                .fetch()
                .associate { it.value1() to (it.value2()?.toInt() ?: 0) }
        }
    }

    /**
     * 按变量名和指定版本批量获取引用计数（点查本概要表）。
     * 用于查看变量组某个具体版本下的引用情况。
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
                .and(VAR_NAME.`in`(varNames))
                .and(VERSION.eq(version))
                .fetch()
                .associate { it.value1() to (it.value2() ?: 0) }
        }
    }

    /**
     * 删除变量组下、(变量名,版本) 不在 keep 中的所有概要行（重算后清理已归零的桶）。
     * keep 为空表示该组已无任何生效引用，删除全部行。
     */
    fun deleteByGroupNameExceptVarVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        keep: Collection<Pair<String, Int>>
    ) {
        with(TResourcePublicVarVersionSummary.T_RESOURCE_PUBLIC_VAR_VERSION_SUMMARY) {
            val delete = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
            if (keep.isNotEmpty()) {
                delete.and(
                    DSL.row(VAR_NAME, VERSION).notIn(keep.map { DSL.row(it.first, it.second) })
                )
            }
            delete.execute()
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

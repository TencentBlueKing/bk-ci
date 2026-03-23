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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.model.process.tables.TResourcePublicVarGroupReferInfo
import com.tencent.devops.model.process.tables.TResourcePublicVarReferInfo
import com.tencent.devops.model.process.tables.records.TResourcePublicVarGroupReferInfoRecord
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import org.jooq.DSLContext
import org.jooq.Select
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupReferInfoDao {

    /**
     * 构建基础查询条件
     * @param table 表对象
     * @param projectId 项目ID
     * @return 基础条件列表
     */
    private fun buildBaseConditions(
        table: TResourcePublicVarGroupReferInfo,
        projectId: String
    ) = mutableListOf(table.PROJECT_ID.eq(projectId))

    /**
     * 构建引用相关的查询条件
     * @param table 表对象
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本
     * @param referVersionName 引用版本名称
     * @return 条件列表
     */
    private fun buildReferConditions(
        table: TResourcePublicVarGroupReferInfo,
        projectId: String,
        referId: String? = null,
        referType: PublicVarGroupReferenceTypeEnum? = null,
        referVersion: Int? = null,
        referVersionName: String? = null
    ) = buildBaseConditions(table, projectId).apply {
        referId?.let { add(table.REFER_ID.eq(it)) }
        referType?.let { add(table.REFER_TYPE.eq(it.name)) }
        referVersion?.let { add(table.REFER_VERSION.eq(it)) }
        referVersionName?.let { add(table.REFER_VERSION_NAME.eq(it)) }
    }

    fun listVarGroupReferInfoByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int? = null,
        groupName: String? = null
    ): List<ResourcePublicVarGroupReferPO> {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(this, projectId, referId, referType, referVersion).apply {
                groupName?.let { add(GROUP_NAME.eq(it)) }
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.asc())
                .fetch()
                .map {
                    convertResourcePublicVarGroupReferPO(it)
                }
        }
    }

    /**
     * 查询单个变量组引用关系
     */
    fun getVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int,
        groupName: String
    ): ResourcePublicVarGroupReferPO? {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(this, projectId, referId, referType, referVersion).apply {
                add(GROUP_NAME.eq(groupName))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()
                ?.let { convertResourcePublicVarGroupReferPO(it) }
        }
    }

    /**
     * 将数据库记录转换为变量组引用PO对象
     * @param publicVarGroupReferInfoRecord 数据库记录
     * @return 变量组引用PO对象
     */
    private fun convertResourcePublicVarGroupReferPO(
        publicVarGroupReferInfoRecord: TResourcePublicVarGroupReferInfoRecord
    ) = ResourcePublicVarGroupReferPO(id = publicVarGroupReferInfoRecord.id,
        projectId = publicVarGroupReferInfoRecord.projectId,
        groupName = publicVarGroupReferInfoRecord.groupName,
        version = publicVarGroupReferInfoRecord.version,
        referId = publicVarGroupReferInfoRecord.referId,
        referName = publicVarGroupReferInfoRecord.referName,
        referType = PublicVarGroupReferenceTypeEnum.valueOf(publicVarGroupReferInfoRecord.referType),
        createTime = publicVarGroupReferInfoRecord.createTime,
        updateTime = publicVarGroupReferInfoRecord.updateTime,
        creator = publicVarGroupReferInfoRecord.creator,
        modifier = publicVarGroupReferInfoRecord.modifier,
        referVersion = publicVarGroupReferInfoRecord.referVersion,
        referVersionName = publicVarGroupReferInfoRecord.referVersionName,
        positionInfo = publicVarGroupReferInfoRecord.positionInfo?.let {
            JsonUtil.to(
                json = it,
                typeReference = object : TypeReference<List<PublicVarPositionPO>>() {}
            )
        })

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int? = null
    ) {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val deleteQuery = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))

            if (referVersion != null) {
                deleteQuery.and(REFER_VERSION.eq(referVersion))
            }

            deleteQuery.execute()
        }
    }

    /**
     * 根据引用ID和变量组名删除变量组引用记录（支持可选版本过滤）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param groupName 变量组名
     * @param referVersion 引用版本（可选，为null时删除该组所有版本的引用）
     */
    fun deleteByReferIdAndGroup(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int? = null
    ) {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val deleteQuery = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))

            if (referVersion != null) {
                deleteQuery.and(REFER_VERSION.eq(referVersion))
            }

            deleteQuery.execute()
        }
    }

    /**
     * 批量删除多个变量组的引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param groupNames 变量组名列表
     * @param referVersion 引用版本
     */
    fun batchDeleteByReferIdAndGroups(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        groupNames: List<String>,
        referVersion: Int
    ) {
        if (groupNames.isEmpty()) {
            return
        }
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.`in`(groupNames))
                .and(REFER_VERSION.eq(referVersion))
                .execute()
        }
    }

    /**
     * 批量保存变量组引用信息（支持更新）
     * @param dslContext 数据库上下文
     * @param resourcePublicVarGroupReferPOS 变量组引用PO列表
     */
    fun batchSave(
        dslContext: DSLContext,
        resourcePublicVarGroupReferPOS: List<ResourcePublicVarGroupReferPO>
    ) {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val insertSteps = resourcePublicVarGroupReferPOS.map { po ->
                dslContext.insertInto(
                    this,
                    ID,
                    PROJECT_ID,
                    GROUP_NAME,
                    VERSION,
                    REFER_ID,
                    REFER_TYPE,
                    REFER_NAME,
                    REFER_VERSION,
                    REFER_VERSION_NAME,
                    POSITION_INFO,
                    CREATOR,
                    MODIFIER,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    po.id,
                    po.projectId,
                    po.groupName,
                    po.version,
                    po.referId,
                    po.referType.name,
                    po.referName,
                    po.referVersion,
                    po.referVersionName,
                    po.positionInfo?.let { JsonUtil.toJson(it, false) },
                    po.creator,
                    po.modifier,
                    po.createTime,
                    po.updateTime
                ).onDuplicateKeyUpdate()
                    .set(REFER_NAME, po.referName)
                    .set(REFER_VERSION_NAME, po.referVersionName)
                    .set(POSITION_INFO, po.positionInfo?.let { JsonUtil.toJson(it, false) })
                    .set(MODIFIER, po.modifier)
                    .set(UPDATE_TIME, po.updateTime)
            }
            dslContext.batch(insertSteps).execute()
        }
    }

    /**
     * 构建关联变量组最新活跃版本的 UNION ALL 查询（统一入口）
     * 活跃版本定义：
     * - 对于有实际变量引用的 referId，取有变量引用版本中的最大 referVersion
     * - 对于没有变量引用的 referId，取最大 referVersion
     * 两种过滤维度：
     * - 按 groupName 过滤：传入 groupName 参数，referIds 为 null
     * - 按 referIds 过滤：传入 referIds 参数，groupName 为 null
     * 两者至少传入一个。
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名（与 referIds 二选一）
     * @param referIds 需要查询的 referId 列表（与 groupName 二选一）
     * @param referType 引用类型（可选）
     * @param referIdsWithActualVar 有实际变量引用的 referId 列表
     * @return UNION ALL 查询（未排序、未分页）
     */
    private fun buildLatestActiveReferUnionQuery(
        dslContext: DSLContext,
        projectId: String,
        groupName: String? = null,
        referIds: List<String>? = null,
        referType: PublicVarGroupReferenceTypeEnum?,
        referIdsWithActualVar: List<String>
    ): Select<*> {
        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val trpvgriSub = trpvgri.`as`("trpvgri_sub")
        val trpvri = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO

        // 构建基础过滤条件
        val conditions = mutableListOf(trpvgri.PROJECT_ID.eq(projectId))
        groupName?.let { conditions.add(trpvgri.GROUP_NAME.eq(it)) }
        referIds?.let { conditions.add(trpvgri.REFER_ID.`in`(it)) }
        referType?.let { conditions.add(trpvgri.REFER_TYPE.eq(it.name)) }

        // 存在实际变量引用的条件
        val existsVarReferCondition = DSL.exists(
            dslContext.selectOne()
                .from(trpvri)
                .where(trpvri.PROJECT_ID.eq(trpvgri.PROJECT_ID))
                .and(trpvri.REFER_ID.eq(trpvgri.REFER_ID))
                .and(trpvri.REFER_VERSION.eq(trpvgri.REFER_VERSION))
                .and(trpvri.GROUP_NAME.eq(trpvgri.GROUP_NAME))
        )

        // 构建子查询：查找同一 referId 下使用变量的更高版本不存在
        var notExistsHigherVersionWithVarQuery = dslContext.selectOne()
            .from(trpvgriSub)
            .where(trpvgriSub.PROJECT_ID.eq(trpvgri.PROJECT_ID))
            .and(trpvgriSub.REFER_ID.eq(trpvgri.REFER_ID))
            .and(trpvgriSub.GROUP_NAME.eq(trpvgri.GROUP_NAME))
            .and(trpvgriSub.REFER_VERSION.gt(trpvgri.REFER_VERSION))
            .andExists(
                dslContext.selectOne()
                    .from(trpvri)
                    .where(trpvri.PROJECT_ID.eq(trpvgriSub.PROJECT_ID))
                    .and(trpvri.REFER_ID.eq(trpvgriSub.REFER_ID))
                    .and(trpvri.REFER_VERSION.eq(trpvgriSub.REFER_VERSION))
                    .and(trpvri.GROUP_NAME.eq(trpvgriSub.GROUP_NAME))
            )
        referType?.let {
            notExistsHigherVersionWithVarQuery = notExistsHigherVersionWithVarQuery
                .and(trpvgriSub.REFER_TYPE.eq(it.name))
        }

        // 构建子查询：查找同一 referId 下更高版本不存在（不要求有变量引用）
        var notExistsHigherVersionQuery = dslContext.selectOne()
            .from(trpvgriSub)
            .where(trpvgriSub.PROJECT_ID.eq(trpvgri.PROJECT_ID))
            .and(trpvgriSub.REFER_ID.eq(trpvgri.REFER_ID))
            .and(trpvgriSub.GROUP_NAME.eq(trpvgri.GROUP_NAME))
            .and(trpvgriSub.REFER_VERSION.gt(trpvgri.REFER_VERSION))
        referType?.let {
            notExistsHigherVersionQuery = notExistsHigherVersionQuery.and(trpvgriSub.REFER_TYPE.eq(it.name))
        }

        // 分离有/无实际变量引用的 referId
        val idsWithVar = referIdsWithActualVar.ifEmpty { listOf("") }
        val idsWithoutVarCondition = if (referIdsWithActualVar.isEmpty()) {
            // 没有任何 referId 有实际变量引用，全部走 "无变量引用" 分支
            DSL.trueCondition()
        } else {
            trpvgri.REFER_ID.notIn(referIdsWithActualVar)
        }

        // 情况1: 有实际变量引用的 referId，取有变量引用版本中的最大 referVersion
        val queryWithVar = dslContext.selectFrom(trpvgri)
            .where(conditions)
            .and(trpvgri.REFER_ID.`in`(idsWithVar))
            .and(existsVarReferCondition)
            .and(DSL.notExists(notExistsHigherVersionWithVarQuery))

        // 情况2: 没有实际变量引用的 referId，取最大 referVersion
        val queryWithoutVar = dslContext.selectFrom(trpvgri)
            .where(conditions)
            .and(idsWithoutVarCondition)
            .and(DSL.notExists(notExistsHigherVersionQuery))

        return queryWithVar.unionAll(queryWithoutVar)
    }

    /**
     * 统计关联变量组的最新活跃版本记录总数（按 groupName 维度）
     * 基于与 list 方法相同的 UNION ALL 逻辑精确计数，保证 count 与 list 语义一致
     *
     * @param referIdsWithActualVar 有实际变量引用的 referId 列表
     */
    fun countLatestActiveVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVarGroupReferenceTypeEnum?,
        referIdsWithActualVar: List<String>
    ): Int {
        val unionQuery = buildLatestActiveReferUnionQuery(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            referType = referType,
            referIdsWithActualVar = referIdsWithActualVar
        )
        return dslContext.selectCount()
            .from(unionQuery.asTable("union_result"))
            .fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 查询关联变量组的资源最新版本记录（按 referId 去重，按 groupName 维度）
     * 对于有实际变量引用的 referId，取有变量引用版本中的最大 referVersion
     * 对于没有变量引用的 referId，取最大 referVersion
     * @param referIdsWithActualVar 有实际变量引用的 referId 列表
     */
    fun listLatestActiveVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVarGroupReferenceTypeEnum?,
        referIdsWithActualVar: List<String>,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> {
        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val unionQuery = buildLatestActiveReferUnionQuery(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            referType = referType,
            referIdsWithActualVar = referIdsWithActualVar
        )

        // UNION ALL 后在数据库层排序分页
        val offset = (page - 1) * pageSize
        return dslContext.selectFrom(unionQuery.asTable("union_result"))
            .orderBy(DSL.field("update_time").desc())
            .limit(pageSize)
            .offset(offset)
            .fetch()
            .map { convertResourcePublicVarGroupReferPO(it.into(trpvgri.recordType)) }
    }

    /**
     * 统计按 referId 列表筛选的最新活跃版本记录总数
     * 基于与 list 方法相同的 UNION ALL 逻辑精确计数，保证 count 与 list 语义一致
     *
     * @param referIdsWithActualVar 有实际变量引用的 referId 列表
     */
    fun countLatestActiveVarGroupReferInfoByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVarGroupReferenceTypeEnum?,
        referIdsWithActualVar: List<String>
    ): Int {
        if (referIds.isEmpty()) return 0

        val unionQuery = buildLatestActiveReferUnionQuery(
            dslContext = dslContext,
            projectId = projectId,
            referIds = referIds,
            referType = referType,
            referIdsWithActualVar = referIdsWithActualVar
        )
        return dslContext.selectCount()
            .from(unionQuery.asTable("union_result"))
            .fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 根据 referId 列表查询关联变量组的资源最新版本记录
     * 对于有实际变量引用的 referId，取有变量引用版本中的最大 referVersion
     * 对于没有变量引用的 referId，取最大 referVersion
     * @param referIdsWithActualVar 有实际变量引用的 referId 列表
     */
    fun listLatestActiveVarGroupReferInfoByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVarGroupReferenceTypeEnum?,
        referIdsWithActualVar: List<String>,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> {
        if (referIds.isEmpty()) return emptyList()

        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val unionQuery = buildLatestActiveReferUnionQuery(
            dslContext = dslContext,
            projectId = projectId,
            referIds = referIds,
            referType = referType,
            referIdsWithActualVar = referIdsWithActualVar
        )

        // UNION ALL 后在数据库层排序分页
        val offset = (page - 1) * pageSize
        return dslContext.selectFrom(unionQuery.asTable("union_result"))
            .orderBy(DSL.field("update_time").desc())
            .limit(pageSize)
            .offset(offset)
            .fetch()
            .map { convertResourcePublicVarGroupReferPO(it.into(trpvgri.recordType)) }
    }
}

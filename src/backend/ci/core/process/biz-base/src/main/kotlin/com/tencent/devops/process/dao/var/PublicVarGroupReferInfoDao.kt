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
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.model.process.tables.TResourcePublicVarGroupReferInfo
import com.tencent.devops.model.process.tables.records.TResourcePublicVarGroupReferInfoRecord
import com.tencent.devops.process.constant.ProcessMessageCode.DYNAMIC_VERSION
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import com.tencent.devops.process.pojo.`var`.po.VarGroupReferInfoUpdatePO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupReferInfoDao {

    /**
     * 构建基础查询条件
     */
    private fun buildBaseConditions(
        table: TResourcePublicVarGroupReferInfo,
        projectId: String
    ) = mutableListOf(table.PROJECT_ID.eq(projectId))

    /**
     * 构建引用相关的查询条件
     */
    private fun buildReferConditions(
        table: TResourcePublicVarGroupReferInfo,
        projectId: String,
        referId: String? = null,
        referType: PublicVerGroupReferenceTypeEnum? = null,
        referVersion: Int? = null,
        referVersionName: String? = null
    ) = buildBaseConditions(table, projectId).apply {
        referId?.let { add(table.REFER_ID.eq(it)) }
        referType?.let { add(table.REFER_TYPE.eq(it.name)) }
        referVersion?.let { add(table.REFER_VERSION.eq(it)) }
        referVersionName?.let { add(table.REFER_VERSION_NAME.eq(it)) }
    }

    /**
     * 构建变量组相关的查询条件
     */
    private fun buildGroupConditions(
        table: TResourcePublicVarGroupReferInfo,
        projectId: String,
        groupName: String? = null,
        version: Int? = null
    ) = buildBaseConditions(table, projectId).apply {
        groupName?.let { add(table.GROUP_NAME.eq(it)) }
        version?.let { add(table.VERSION.eq(it)) }
    }

    /**
     * 查询变量组引用信息（获取每个REFER_ID的最新记录）
     */
    private fun listLatestVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String? = null,
        referType: PublicVerGroupReferenceTypeEnum? = null,
        version: Int? = null,
        versions: List<Int>? = null,
        referIds: List<String>? = null,
        page: Int,
        pageSize: Int,
        orderByReferId: Boolean = false
    ): List<ResourcePublicVarGroupReferPO> {
        val t = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val t2 = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO

        // 构建主查询条件
        val conditions = mutableListOf(t.PROJECT_ID.eq(projectId))
        groupName?.let { conditions.add(t.GROUP_NAME.eq(it)) }
        referType?.let { conditions.add(t.REFER_TYPE.eq(it.name)) }
        version?.let { conditions.add(t.VERSION.eq(it)) }
        versions?.takeIf { it.isNotEmpty() }?.let { conditions.add(t.VERSION.`in`(it)) }
        referIds?.takeIf { it.isNotEmpty() }?.let { conditions.add(t.REFER_ID.`in`(it)) }

        // 构建NOT EXISTS子查询条件
        var notExistsQuery = dslContext.selectOne()
            .from(t2)
            .where(t2.PROJECT_ID.eq(t.PROJECT_ID))
            .and(t2.REFER_ID.eq(t.REFER_ID))
            .and(t2.CREATE_TIME.gt(t.CREATE_TIME))

        groupName?.let { notExistsQuery = notExistsQuery.and(t2.GROUP_NAME.eq(t.GROUP_NAME)) }
        referType?.let { notExistsQuery = notExistsQuery.and(t2.REFER_TYPE.eq(it.name)) }
        version?.let { notExistsQuery = notExistsQuery.and(t2.VERSION.eq(it)) }
        versions?.takeIf { it.isNotEmpty() }?.let { notExistsQuery = notExistsQuery.and(t2.VERSION.`in`(it)) }
        referIds?.takeIf { it.isNotEmpty() }?.let { notExistsQuery = notExistsQuery.and(t2.REFER_ID.`in`(it)) }

        val orderField = if (orderByReferId) t.REFER_ID.asc() else t.UPDATE_TIME.desc()

        return dslContext.selectFrom(t)
            .where(conditions)
            .and(DSL.notExists(notExistsQuery))
            .orderBy(orderField)
            .limit(pageSize)
            .offset((page - 1) * pageSize)
            .fetch()
            .map { convertResourcePublicVarGroupReferPO(it) }
    }

    fun listVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        version: Int? = DYNAMIC_VERSION,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> = listLatestVarGroupReferInfo(
        dslContext = dslContext,
        projectId = projectId,
        groupName = groupName,
        referType = referType,
        version = version,
        page = page,
        pageSize = pageSize
    )

    /**
     * 查询变量组引用信息（支持多个版本）
     */
    fun listVarGroupReferInfoByVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        versions: List<Int>,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> {
        if (versions.isEmpty()) return emptyList()
        return listLatestVarGroupReferInfo(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            referType = referType,
            versions = versions,
            page = page,
            pageSize = pageSize
        )
    }

    /**
     * 根据referId列表查询变量组引用信息
     */
    fun listVarGroupReferInfoByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum?,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> {
        if (referIds.isEmpty()) return emptyList()
        return listLatestVarGroupReferInfo(
            dslContext = dslContext,
            projectId = projectId,
            referType = referType,
            referIds = referIds,
            page = page,
            pageSize = pageSize,
            orderByReferId = true
        )
    }

    fun listVarGroupReferInfoByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
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
     * 批量查询多个引用ID的变量组信息
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referinfos 引用ID映射，key为引用ID，value为REFER_VERSION
     * @param referType 引用类型
     * @return 变量组引用信息列表
     */
    fun batchListVarGroupReferInfoByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referinfos: List<Pair<String, Int>>,
        referType: PublicVerGroupReferenceTypeEnum
    ): List<ResourcePublicVarGroupReferPO> {
        if (referinfos.isEmpty()) {
            return emptyList()
        }

        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val referConditions = referinfos.map { (referId, referVersion) ->
                REFER_ID.eq(referId).and(REFER_VERSION.eq(referVersion))
            }

            // 组合所有条件
            val finalCondition = PROJECT_ID.eq(projectId)
                .and(REFER_TYPE.eq(referType.name))
                .and(referConditions.reduce { acc, condition -> acc.or(condition) })

            return dslContext.selectFrom(this)
                .where(finalCondition)
                .orderBy(REFER_ID.asc(), CREATE_TIME.asc())
                .fetch()
                .map { convertResourcePublicVarGroupReferPO(it) }
        }
    }

    private fun convertResourcePublicVarGroupReferPO(
        publicVarGroupReferInfoRecord: TResourcePublicVarGroupReferInfoRecord
    ) = ResourcePublicVarGroupReferPO(id = publicVarGroupReferInfoRecord.id,
        projectId = publicVarGroupReferInfoRecord.projectId,
        groupName = publicVarGroupReferInfoRecord.groupName,
        version = publicVarGroupReferInfoRecord.version,
        referId = publicVarGroupReferInfoRecord.referId,
        referName = publicVarGroupReferInfoRecord.referName,
        sourceProjectId = publicVarGroupReferInfoRecord.groupProjectId,
        referType = PublicVerGroupReferenceTypeEnum.valueOf(publicVarGroupReferInfoRecord.referType),
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

    fun countByPublicVarGroupRef(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String? = null,
        referVersionName: String? = null
    ): Int {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(
                table = this,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersionName = referVersionName
            ).apply {
                groupName?.let { add(GROUP_NAME.eq(it)) }
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun deleteByReferIdExcludingGroupNames(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String,
        excludedGroupNames: List<String>? = null
    ) {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(
                table = this,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersionName = referVersionName
            ).apply {
                if (!excludedGroupNames.isNullOrEmpty()) {
                    add(GROUP_NAME.notIn(excludedGroupNames))
                }
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum
    ) {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .execute()
        }
    }

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
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

    fun deleteByReferIdAndGroup(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int
    ) {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))
                .and(REFER_VERSION.eq(referVersion))
                .execute()
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
        referType: PublicVerGroupReferenceTypeEnum,
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
                    GROUP_PROJECT_ID,
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
                    po.sourceProjectId,
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

    fun countByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        version: Int? = DYNAMIC_VERSION
    ): Int {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            // 当version不为null时才添加版本条件
            val conditions = buildGroupConditions(
                table = this,
                projectId = projectId,
                groupName = groupName,
                version = if (version != null) version else null
            ).apply {
                referType?.let { add(REFER_TYPE.eq(it.name)) }
            }
            return dslContext.select(DSL.countDistinct(REFER_ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 统计变量组引用数量（支持多个版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名称
     * @param referType 引用类型
     * @param versions 版本列表
     * @return 引用数量
     */
    fun countByGroupNameAndVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        versions: List<Int>
    ): Int {
        if (versions.isEmpty()) {
            return 0
        }

        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                GROUP_NAME.eq(groupName),
                VERSION.`in`(versions)
            )
            referType?.let { conditions.add(REFER_TYPE.eq(it.name)) }

            return dslContext.select(DSL.countDistinct(REFER_ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 根据 referId 列表统计总数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referIds 引用ID列表
     * @param referType 引用类型
     * @return 总数
     */
    fun countByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum?
    ): Int {
        if (referIds.isEmpty()) {
            return 0
        }

        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                REFER_ID.`in`(referIds)
            )
            referType?.let { conditions.add(REFER_TYPE.eq(it.name)) }

            return dslContext.select(DSL.countDistinct(REFER_ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 批量统计多个变量组的引用数量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名称列表
     * @param referType 引用类型（可选）
     * @return Map<String, Int> 变量组名称到引用数量的映射
     */
    fun batchCountByGroupNames(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>,
        referType: PublicVerGroupReferenceTypeEnum? = null
    ): Map<String, Int> {
        if (groupNames.isEmpty()) {
            return emptyMap()
        }

        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                GROUP_NAME.`in`(groupNames)
            )
            referType?.let { conditions.add(REFER_TYPE.eq(it.name)) }

            return dslContext.select(GROUP_NAME, DSL.countDistinct(REFER_ID))
                .from(this)
                .where(conditions)
                .groupBy(GROUP_NAME)
                .fetch()
                .associate { record ->
                    record.value1() to (record.value2() ?: 0)
                }
        }
    }

    /**
     * 查询某项目某变量组的所有引用记录（所有版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @return 引用记录列表
     */
    fun listAllReferRecordsByGroup(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ): List<TResourcePublicVarGroupReferInfoRecord> {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .fetch()
        }
    }

    /**
     * 查询某项目某变量组的实际引用数量（按 referId 去重）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @return 去重后的引用数量
     */
    fun countDistinctReferIdByGroup(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ): Int {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            return dslContext.select(DSL.countDistinct(REFER_ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 批量查询多个变量组的实际引用数量（按 referId 去重）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名列表
     * @return Map<groupName, distinctReferIdCount>
     */
    fun batchCountDistinctReferIdByGroup(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()

        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            return dslContext
                .select(GROUP_NAME, DSL.countDistinct(REFER_ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .groupBy(GROUP_NAME)
                .fetch()
                .associate { record ->
                    record.value1() to (record.value2() ?: 0)
                }
        }
    }

    /**
     * 更新变量组引用信息
     */
    fun updateVarGroupReferInfo(
        dslContext: DSLContext,
        updatePO: VarGroupReferInfoUpdatePO
    ): Int {
        with(TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(
                table = this,
                projectId = updatePO.projectId,
                referId = updatePO.referId,
                referType = updatePO.referType,
                referVersionName = updatePO.referVersionName
            ).apply {
                add(GROUP_NAME.eq(updatePO.groupName))
            }

            return dslContext.update(this)
                .set(VERSION, updatePO.version)
                .set(POSITION_INFO, updatePO.positionInfo)
                .set(MODIFIER, updatePO.modifier)
                .set(UPDATE_TIME, updatePO.updateTime)
                .where(conditions)
                .execute()
        }
    }
}
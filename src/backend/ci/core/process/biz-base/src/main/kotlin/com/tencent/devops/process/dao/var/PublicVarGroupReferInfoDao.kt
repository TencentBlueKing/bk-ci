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
import com.tencent.devops.model.process.tables.TResourcePublicVarReferInfo
import com.tencent.devops.model.process.tables.records.TResourcePublicVarGroupReferInfoRecord
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReferPO
import org.jooq.DSLContext
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
        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val trpvgriSub = trpvgri.`as`("trpvgri_sub")

        // 构建主查询条件
        val conditions = mutableListOf(trpvgri.PROJECT_ID.eq(projectId))
        groupName?.let { conditions.add(trpvgri.GROUP_NAME.eq(it)) }
        referType?.let { conditions.add(trpvgri.REFER_TYPE.eq(it.name)) }
        version?.let { conditions.add(trpvgri.VERSION.eq(it)) }
        versions?.takeIf { it.isNotEmpty() }?.let { conditions.add(trpvgri.VERSION.`in`(it)) }
        referIds?.takeIf { it.isNotEmpty() }?.let { conditions.add(trpvgri.REFER_ID.`in`(it)) }

        // 构建NOT EXISTS子查询条件
        var notExistsQuery = dslContext.selectOne()
            .from(trpvgriSub)
            .where(trpvgriSub.PROJECT_ID.eq(trpvgri.PROJECT_ID))
            .and(trpvgriSub.REFER_ID.eq(trpvgri.REFER_ID))
            .and(trpvgriSub.CREATE_TIME.gt(trpvgri.CREATE_TIME))

        groupName?.let { notExistsQuery = notExistsQuery.and(trpvgriSub.GROUP_NAME.eq(trpvgri.GROUP_NAME)) }
        referType?.let { notExistsQuery = notExistsQuery.and(trpvgriSub.REFER_TYPE.eq(it.name)) }
        version?.let { notExistsQuery = notExistsQuery.and(trpvgriSub.VERSION.eq(it)) }
        versions?.takeIf { it.isNotEmpty() }?.let { notExistsQuery = notExistsQuery.and(trpvgriSub.VERSION.`in`(it)) }
        referIds?.takeIf { it.isNotEmpty() }?.let { notExistsQuery = notExistsQuery.and(trpvgriSub.REFER_ID.`in`(it)) }

        val orderField = if (orderByReferId) trpvgri.REFER_ID.asc() else trpvgri.UPDATE_TIME.desc()

        return dslContext.selectFrom(trpvgri)
            .where(conditions)
            .and(DSL.notExists(notExistsQuery))
            .orderBy(orderField)
            .limit(pageSize)
            .offset((page - 1) * pageSize)
            .fetch()
            .map { convertResourcePublicVarGroupReferPO(it) }
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

    /**
     * 根据引用ID删除所有引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     */
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

    /**
     * 统计使用变量的资源数量（按 referId 去重，只统计有实际变量引用的记录）
     */
    fun countLatestActiveVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?
    ): Int {
        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val trpvri = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO

        val conditions = mutableListOf(
            trpvgri.PROJECT_ID.eq(projectId),
            trpvgri.GROUP_NAME.eq(groupName)
        )
        referType?.let { conditions.add(trpvgri.REFER_TYPE.eq(it.name)) }

        // 统计有实际变量引用的不同 referId 数量
        return dslContext.select(DSL.countDistinct(trpvgri.REFER_ID))
            .from(trpvgri)
            .where(conditions)
            .andExists(
                dslContext.selectOne()
                    .from(trpvri)
                    .where(trpvri.PROJECT_ID.eq(trpvgri.PROJECT_ID))
                    .and(trpvri.REFER_ID.eq(trpvgri.REFER_ID))
                    .and(trpvri.REFER_VERSION.eq(trpvgri.REFER_VERSION))
                    .and(trpvri.GROUP_NAME.eq(trpvgri.GROUP_NAME))
            )
            .fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 查询使用变量的最新版本记录（按 referId 去重，每个资源只返回使用变量的最新版本）
     */
    fun listLatestActiveVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> {
        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val trpvgriSub = trpvgri.`as`("trpvgri_sub")
        val trpvri = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO

        val conditions = mutableListOf(
            trpvgri.PROJECT_ID.eq(projectId),
            trpvgri.GROUP_NAME.eq(groupName)
        )
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
        var notExistsHigherVersionQuery = dslContext.selectOne()
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
            notExistsHigherVersionQuery = notExistsHigherVersionQuery.and(trpvgriSub.REFER_TYPE.eq(it.name))
        }

        return dslContext.selectFrom(trpvgri)
            .where(conditions)
            .and(existsVarReferCondition)
            .and(DSL.notExists(notExistsHigherVersionQuery))
            .orderBy(trpvgri.UPDATE_TIME.desc())
            .limit(pageSize)
            .offset((page - 1) * pageSize)
            .fetch()
            .map { convertResourcePublicVarGroupReferPO(it) }
    }

    /**
     * 统计按 referId 列表筛选且使用变量的资源数量（按 referId 去重）
     */
    fun countLatestActiveVarGroupReferInfoByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum?
    ): Int {
        if (referIds.isEmpty()) return 0

        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val trpvri = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO

        val conditions = mutableListOf(
            trpvgri.PROJECT_ID.eq(projectId),
            trpvgri.REFER_ID.`in`(referIds)
        )
        referType?.let { conditions.add(trpvgri.REFER_TYPE.eq(it.name)) }

        return dslContext.select(DSL.countDistinct(trpvgri.REFER_ID))
            .from(trpvgri)
            .where(conditions)
            .andExists(
                dslContext.selectOne()
                    .from(trpvri)
                    .where(trpvri.PROJECT_ID.eq(trpvgri.PROJECT_ID))
                    .and(trpvri.REFER_ID.eq(trpvgri.REFER_ID))
                    .and(trpvri.REFER_VERSION.eq(trpvgri.REFER_VERSION))
                    .and(trpvri.GROUP_NAME.eq(trpvgri.GROUP_NAME))
            )
            .fetchOne(0, Int::class.java) ?: 0
    }

    /**
     * 根据 referId 列表查询使用变量的最新版本记录
     */
    fun listLatestActiveVarGroupReferInfoByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum?,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReferPO> {
        if (referIds.isEmpty()) return emptyList()

        val trpvgri = TResourcePublicVarGroupReferInfo.T_RESOURCE_PUBLIC_VAR_GROUP_REFER_INFO
        val trpvgriSub = trpvgri.`as`("trpvgri_sub")
        val trpvri = TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO

        val conditions = mutableListOf(
            trpvgri.PROJECT_ID.eq(projectId),
            trpvgri.REFER_ID.`in`(referIds)
        )
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

        // 不存在同一 referId 使用变量的更高版本
        var notExistsHigherVersionQuery = dslContext.selectOne()
            .from(trpvgriSub)
            .where(trpvgriSub.PROJECT_ID.eq(trpvgri.PROJECT_ID))
            .and(trpvgriSub.REFER_ID.eq(trpvgri.REFER_ID))
            .and(trpvgriSub.REFER_VERSION.gt(trpvgri.REFER_VERSION))
            .andExists(
                dslContext.selectOne()
                    .from(trpvri)
                    .where(trpvri.PROJECT_ID.eq(trpvgriSub.PROJECT_ID))
                    .and(trpvri.REFER_ID.eq(trpvgriSub.REFER_ID))
                    .and(trpvri.REFER_VERSION.eq(trpvgriSub.REFER_VERSION))
            )
        referType?.let {
            notExistsHigherVersionQuery = notExistsHigherVersionQuery.and(trpvgriSub.REFER_TYPE.eq(it.name))
        }

        return dslContext.selectFrom(trpvgri)
            .where(conditions)
            .and(existsVarReferCondition)
            .and(DSL.notExists(notExistsHigherVersionQuery))
            .orderBy(trpvgri.UPDATE_TIME.desc())
            .limit(pageSize)
            .offset((page - 1) * pageSize)
            .fetch()
            .map { convertResourcePublicVarGroupReferPO(it) }
    }

}
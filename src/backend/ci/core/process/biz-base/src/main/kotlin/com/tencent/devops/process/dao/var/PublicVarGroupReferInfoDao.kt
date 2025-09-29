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
import com.tencent.devops.model.process.tables.TPipelinePublicVarGroupReferInfo
import com.tencent.devops.model.process.tables.records.TPipelinePublicVarGroupReferInfoRecord
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPositionPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupReferInfoDao {

    /**
     * 构建基础查询条件
     */
    private fun buildBaseConditions(
        table: TPipelinePublicVarGroupReferInfo,
        projectId: String
    ) = mutableListOf(table.PROJECT_ID.eq(projectId))

    /**
     * 构建引用相关的查询条件
     */
    private fun buildReferConditions(
        table: TPipelinePublicVarGroupReferInfo,
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
        table: TPipelinePublicVarGroupReferInfo,
        projectId: String,
        groupName: String? = null,
        version: Int? = null
    ) = buildBaseConditions(table, projectId).apply {
        groupName?.let { add(table.GROUP_NAME.eq(it)) }
        version?.let { add(table.VERSION.eq(it)) }
    }

    fun listVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        version: Int? = null,
        page: Int,
        pageSize: Int
    ): List<PipelinePublicVarGroupReferPO> {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            referType?.let { conditions.add(REFER_TYPE.eq(it.name)) }
            conditions.add(VERSION.eq(version))
            
            // 子查询：获取每个REFER_ID对应的最大CREATE_TIME
            val subQuery = dslContext.select(REFER_ID, DSL.max(CREATE_TIME))
                .from(this)
                .where(conditions)
                .groupBy(REFER_ID)
                .limit(pageSize)
                .offset((page - 1) * pageSize)

            return dslContext.selectFrom(this)
                .where(
                    DSL.row(REFER_ID, CREATE_TIME).`in`(
                        subQuery
                    )
                )
                .orderBy(CREATE_TIME.desc())
                .fetch()
                .map {
                    convertPipelinePublicVarGroupReferPO(it)
                }
        }
    }

    fun listVarGroupReferInfoByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int? = null
    ): List<PipelinePublicVarGroupReferPO> {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(this, projectId, referId, referType, referVersion)
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.asc())
                .fetch()
                .map {
                    convertPipelinePublicVarGroupReferPO(it)
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
        referinfos:  List<Pair<String, Int>>,
        referType: PublicVerGroupReferenceTypeEnum
    ): List<PipelinePublicVarGroupReferPO> {
        if (referinfos.isEmpty()) {
            return emptyList()
        }

        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
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
                .map { convertPipelinePublicVarGroupReferPO(it) }
        }
    }

    private fun convertPipelinePublicVarGroupReferPO(
        publicVarGroupReferInfoRecord: TPipelinePublicVarGroupReferInfoRecord
    ) = PipelinePublicVarGroupReferPO(id = publicVarGroupReferInfoRecord.id,
        projectId = publicVarGroupReferInfoRecord.projectId,
        groupName = publicVarGroupReferInfoRecord.groupName,
        version = publicVarGroupReferInfoRecord.version,
        referId = publicVarGroupReferInfoRecord.referId,
        referName = publicVarGroupReferInfoRecord.referName,
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
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(this, projectId, referId, referType, referVersionName = referVersionName).apply {
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
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(this, projectId, referId, referType, referVersionName = referVersionName).apply {
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
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
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
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
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
        version: Int
    ) {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    fun batchSave(
        dslContext: DSLContext,
        pipelinePublicVarGroupReferPOs: List<PipelinePublicVarGroupReferPO>
    ) {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val insertSteps = pipelinePublicVarGroupReferPOs.map { po ->
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

    fun countByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVerGroupReferenceTypeEnum?,
        version: Int? = null
    ): Int {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildGroupConditions(this, projectId, groupName, version).apply {
                referType?.let { add(REFER_TYPE.eq(it.name)) }
            }
            return dslContext.select(DSL.countDistinct(REFER_ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 更新变量组引用信息
     */
    fun updateVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String,
        referVersionName: String?,
        version: Int?,
        positionInfo: String?,
        modifier: String,
        updateTime: java.time.LocalDateTime
    ): Int {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = buildReferConditions(this, projectId, referId, referType, referVersionName = referVersionName).apply {
                add(GROUP_NAME.eq(groupName))
            }

            return dslContext.update(this)
                .set(VERSION, version)
                .set(POSITION_INFO, positionInfo)
                .set(MODIFIER, modifier)
                .set(UPDATE_TIME, updateTime)
                .where(conditions)
                .execute()
        }
    }
}
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

import com.tencent.devops.model.process.tables.TPipelinePublicVarGroupReferInfo
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReferPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupReferInfoDao {

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
            if (referType != null) {
                conditions.add(REFER_TYPE.eq(referType.name))
            }
            if (version != null) {
                conditions.add(VERSION.eq(version))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.asc())
                .limit(pageSize)
                .offset((page - 1) * pageSize)
                .fetch()
                .map {
                    PipelinePublicVarGroupReferPO(
                        id = it.id,
                        projectId = it.projectId,
                        groupName = it.groupName,
                        version = it.version,
                        referId = it.referId,
                        referName= it.referName,
                        referType = PublicVerGroupReferenceTypeEnum.valueOf(it.referType),
                        createTime = it.createTime,
                        updateTime = it.updateTime,
                        creator = it.creator,
                        modifier = it.modifier,
                        referVersionName = it.referVersionName,
                        positionInfo = it.positionInfo
                    )
                }
        }
    }

    fun listVarGroupReferInfoByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String? = null
    ): List<PipelinePublicVarGroupReferPO> {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_TYPE.eq(referType.name))
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.asc())
                .fetch()
                .map {
                    PipelinePublicVarGroupReferPO(
                        id = it.id,
                        projectId = it.projectId,
                        groupName = it.groupName,
                        version = it.version,
                        referId = it.referId,
                        referName= it.referName,
                        referType = PublicVerGroupReferenceTypeEnum.valueOf(it.referType),
                        createTime = it.createTime,
                        updateTime = it.updateTime,
                        creator = it.creator,
                        modifier = it.modifier,
                        referVersionName = it.referVersionName,
                        positionInfo = it.positionInfo
                    )
                }
        }
    }

    fun countByPublicVarGroupRef(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String? = null,
        referVersionName: String? = null
    ): Int {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_TYPE.eq(referType.name))
            if (groupName != null) {
                conditions.add(GROUP_NAME.eq(groupName))
            }
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
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
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(REFER_TYPE.eq(referType.name))
            conditions.add(REFER_ID.eq(referId))
            if (!excludedGroupNames.isNullOrEmpty()) {
                conditions.add(GROUP_NAME.notIn(excludedGroupNames))
            }
            conditions.add(REFER_VERSION_NAME.eq(referVersionName))
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

    fun deleteByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String? = null
    ) {
        if (referIds.isEmpty()) {
            return
        }
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val deleteQuery = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.`in`(referIds))
                .and(REFER_TYPE.eq(referType.name))

            if (referVersionName != null) {
                deleteQuery.and(REFER_VERSION_NAME.eq(referVersionName))
            }

            deleteQuery.execute()
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
                    po.referVersionName,
                    po.positionInfo,
                    po.creator,
                    po.modifier,
                    po.createTime,
                    po.updateTime
                )
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
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            if (referType != null) {
                conditions.add(REFER_TYPE.eq(referType.name))
            }
            if (version != null) {
                conditions.add(VERSION.eq(version))
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
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_TYPE.eq(referType.name))
            conditions.add(GROUP_NAME.eq(groupName))
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
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
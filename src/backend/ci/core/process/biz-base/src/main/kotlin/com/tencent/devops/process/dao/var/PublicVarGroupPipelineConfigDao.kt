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

import com.tencent.devops.model.process.tables.TPipelinePublicVarGroupPipelineConfig
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPipelineConfigPO
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupPipelineConfigDao {

    fun save(
        dslContext: DSLContext,
        publicVarGroupPipelineConfigPO: PublicVarGroupPipelineConfigPO
    ) {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            dslContext.insertInto(this)
                .set(ID, publicVarGroupPipelineConfigPO.id)
                .set(PROJECT_ID, publicVarGroupPipelineConfigPO.projectId)
                .set(REFER_ID, publicVarGroupPipelineConfigPO.referId)
                .set(REFER_VERSION_NAME, publicVarGroupPipelineConfigPO.referVersionName)
                .set(GROUP_NAME, publicVarGroupPipelineConfigPO.groupName)
                .set(GROUP_VERSION, publicVarGroupPipelineConfigPO.groupVersion)
                .set(POSITION_INFO, publicVarGroupPipelineConfigPO.positionInfo)
                .set(REFER_TYPE, publicVarGroupPipelineConfigPO.referType)
                .set(CREATOR, publicVarGroupPipelineConfigPO.creator)
                .set(MODIFIER, publicVarGroupPipelineConfigPO.modifier)
                .set(UPDATE_TIME, publicVarGroupPipelineConfigPO.updateTime)
                .set(CREATE_TIME, publicVarGroupPipelineConfigPO.createTime)
                .execute()
        }
    }

    fun batchSave(
        dslContext: DSLContext,
        publicVarGroupPipelineConfigPOs: List<PublicVarGroupPipelineConfigPO>
    ) {
        if (publicVarGroupPipelineConfigPOs.isEmpty()) {
            return
        }
        
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val insertStep = dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                REFER_ID,
                REFER_VERSION_NAME,
                GROUP_NAME,
                GROUP_VERSION,
                POSITION_INFO,
                REFER_TYPE,
                CREATOR,
                MODIFIER,
                UPDATE_TIME,
                CREATE_TIME
            )
            
            publicVarGroupPipelineConfigPOs.forEach { po ->
                insertStep.values(
                    po.id,
                    po.projectId,
                    po.referId,
                    po.referVersionName,
                    po.groupName,
                    po.groupVersion,
                    po.positionInfo,
                    po.referType,
                    po.creator,
                    po.modifier,
                    po.updateTime,
                    po.createTime
                )
            }
            
            insertStep.execute()
        }
    }

    fun getByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referVersionName: String? = null
    ): List<PublicVarGroupPipelineConfigPO> {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            }
            
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch { record ->
                PublicVarGroupPipelineConfigPO(
                    id = record.id,
                    projectId = record.projectId,
                    referId = record.referId,
                    referVersionName = record.referVersionName,
                    groupName = record.groupName,
                    groupVersion = record.groupVersion,
                    positionInfo = record.positionInfo,
                    referType = record.referType,
                    creator = record.creator,
                    modifier = record.modifier,
                    createTime = record.createTime,
                    updateTime = record.updateTime
                )
            }
        }
    }

    fun getByGroupNameAndVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        groupVersion: Int
    ): List<PublicVarGroupPipelineConfigPO> {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            conditions.add(GROUP_VERSION.eq(groupVersion))
            
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch { record ->
                    PublicVarGroupPipelineConfigPO(
                        id = record.id,
                        projectId = record.projectId,
                        referId = record.referId,
                        referVersionName = record.referVersionName,
                        groupName = record.groupName,
                        groupVersion = record.groupVersion,
                        positionInfo = record.positionInfo,
                        referType = record.referType,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
        }
    }

    fun getByReferIdAndGroupName(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referVersionName: String,
        groupName: String
    ): PublicVarGroupPipelineConfigPO? {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            conditions.add(GROUP_NAME.eq(groupName))
            
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()?.let { record ->
                    PublicVarGroupPipelineConfigPO(
                        id = record.id,
                        projectId = record.projectId,
                        referId = record.referId,
                        referVersionName = record.referVersionName,
                        groupName = record.groupName,
                        groupVersion = record.groupVersion,
                        positionInfo = record.positionInfo,
                        referType = record.referType,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referVersionName: String,
        groupName: String,
        groupVersion: Int,
        positionInfo: String?,
        referType: String?,
        modifier: String
    ) {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val update = dslContext.update(this)
                .set(GROUP_VERSION, groupVersion)
                .set(MODIFIER, modifier)
                .set(UPDATE_TIME, LocalDateTime.now())

            if (positionInfo != null) {
                update.set(POSITION_INFO, positionInfo)
            }
            
            if (referType != null) {
                update.set(REFER_TYPE, referType)
            }

            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            conditions.add(GROUP_NAME.eq(groupName))
            
            update.where(conditions)
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referVersionName: String? = null
    ) {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            }
            
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String
    ) {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deleteByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ) {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun countByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        groupVersion: Int? = null
    ): Int {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            if (groupVersion != null) {
                conditions.add(GROUP_VERSION.eq(groupVersion))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun listRefersByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        groupVersion: Int? = null
    ): List<Triple<String, String, String>> {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            if (groupVersion != null) {
                conditions.add(GROUP_VERSION.eq(groupVersion))
            }
            return dslContext.select(REFER_ID, REFER_VERSION_NAME, REFER_TYPE).from(this)
                .where(conditions)
                .fetch { record ->
                    Triple(record.get(REFER_ID), record.get(REFER_VERSION_NAME), record.get(REFER_TYPE))
                }
        }
    }

    fun batchDeleteByGroupNames(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referVersionName: String,
        groupNames: List<String>
    ) {
        if (groupNames.isEmpty()) {
            return
        }
        
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            conditions.add(GROUP_NAME.`in`(groupNames))
            
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deleteByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referVersionName: String
    ) {
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun batchDeleteByReferIds(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>
    ) {
        if (referIds.isEmpty()) {
            return
        }
        
        with(TPipelinePublicVarGroupPipelineConfig.T_PIPELINE_PUBLIC_VAR_GROUP_PIPELINE_CONFIG) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.`in`(referIds))
            
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }
}
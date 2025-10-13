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

import com.tencent.devops.model.process.tables.TPipelinePublicVarReferInfo
import com.tencent.devops.process.pojo.`var`.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarReferPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarReferInfoDao {

    fun batchSave(
        dslContext: DSLContext,
        pipelinePublicVarReferPOs: List<PipelinePublicVarReferPO>
    ) {
        if (pipelinePublicVarReferPOs.isEmpty()) {
            return
        }
        
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            val insertSteps = pipelinePublicVarReferPOs.map { po ->
                dslContext.insertInto(this)
                    .set(ID, po.id)
                    .set(PROJECT_ID, po.projectId)
                    .set(GROUP_NAME, po.groupName)
                    .set(VAR_NAME, po.varName)
                    .set(VERSION, po.version)
                    .set(REFER_ID, po.referId)
                    .set(REFER_TYPE, po.referType.name)
                    .set(REFER_VERSION_NAME, po.referVersionName)
                    .set(CREATOR, po.creator)
                    .set(MODIFIER, po.modifier)
                    .set(CREATE_TIME, po.createTime)
                    .set(UPDATE_TIME, po.updateTime)
            }
            dslContext.batch(insertSteps).execute()
        }
    }

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String? = null
    ) {
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                REFER_ID.eq(referId),
                REFER_TYPE.eq(referType.name)
            )
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun deleteByReferIdWithoutVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum
    ) {
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .execute()
        }
    }

    fun deleteByReferIdsWithoutVersion(
        dslContext: DSLContext,
        projectId: String,
        referIds: List<String>,
        referType: PublicVerGroupReferenceTypeEnum
    ) {
        if (referIds.isEmpty()) {
            return
        }
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.`in`(referIds))
                .and(REFER_TYPE.eq(referType.name))
                .execute()
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
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(REFER_ID.eq(referId))
            conditions.add(REFER_TYPE.eq(referType.name))
            conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            if (!excludedGroupNames.isNullOrEmpty()) {
                conditions.add(GROUP_NAME.notIn(excludedGroupNames))
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    /**
     * 计算指定引用的实际变量数量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersionName 引用版本名称
     * @return 实际变量引用数量
     */
    fun countActualVarReferencesByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String? = null
    ): Int {
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                REFER_ID.eq(referId),
                REFER_TYPE.eq(referType.name)
            )
            if (referVersionName != null) {
                conditions.add(REFER_VERSION_NAME.eq(referVersionName))
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }


}
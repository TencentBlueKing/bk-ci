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
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            val insertSteps = pipelinePublicVarReferPOs.map { po ->
                dslContext.insertInto(
                    this,
                    ID,
                    PROJECT_ID,
                    GROUP_NAME,
                    VAR_NAME,
                    VERSION,
                    REFER_ID,
                    REFER_TYPE,
                    REFER_VERSION_NAME,
                    CREATOR,
                    MODIFIER,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    po.id,
                    po.projectId,
                    po.groupName,
                    po.varName,
                    po.version,
                    po.referId,
                    po.referType.name,
                    po.referVersionName,
                    po.creator,
                    po.modifier,
                    po.createTime,
                    po.updateTime
                )
            }
            dslContext.batch(insertSteps).execute()
        }
    }

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String
    ) {
        with(TPipelinePublicVarReferInfo.T_PIPELINE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION_NAME.eq(referVersionName))
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
}
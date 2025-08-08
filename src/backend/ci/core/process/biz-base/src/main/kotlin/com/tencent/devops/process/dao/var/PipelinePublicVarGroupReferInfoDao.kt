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
import org.springframework.stereotype.Repository

@Repository
class PipelinePublicVarGroupReferInfoDao {

    fun listVarGroupReferInfo(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        page: Int,
        pageSize: Int
    ): List<PipelinePublicVarGroupReferPO> {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
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
                        referVersionName = it.referVersionName
                    )
                }
        }
    }

    fun countByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String ?= null
    ): Int {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            val conditions = mutableListOf(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            if (varName != null) {
                conditions.add(VAR_NAME.eq(varName))
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .and(GROUP_NAME.eq(groupName))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun countByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String,
        varName: String
    ): Int {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun deleteByGroupName(dslContext: DSLContext, projectId: String, groupName: String) {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupName))
                .execute()
        }
    }

fun save(
        dslContext: DSLContext,
        pipelinePublicVarGroupReferPO: PipelinePublicVarGroupReferPO
    ) {
        with(TPipelinePublicVarGroupReferInfo.T_PIPELINE_PUBLIC_VAR_GROUP_REFER_INFO) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                GROUP_NAME,
                VAR_NAME,
                VAR_TYPE,
                VERSION,
                REFER_ID,
                REFER_TYPE,
                REFER_NAME,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                pipelinePublicVarGroupReferPO.id,
                pipelinePublicVarGroupReferPO.projectId,
                pipelinePublicVarGroupReferPO.groupName,
                pipelinePublicVarGroupReferPO.varName,
                pipelinePublicVarGroupReferPO.varType.name,
                pipelinePublicVarGroupReferPO.version,
                pipelinePublicVarGroupReferPO.referId,
                pipelinePublicVarGroupReferPO.referType.name,
                pipelinePublicVarGroupReferPO.referName,
                pipelinePublicVarGroupReferPO.creator,
                pipelinePublicVarGroupReferPO.modifier,
                pipelinePublicVarGroupReferPO.createTime,
                pipelinePublicVarGroupReferPO.updateTime
            ).execute()
        }
    }
}
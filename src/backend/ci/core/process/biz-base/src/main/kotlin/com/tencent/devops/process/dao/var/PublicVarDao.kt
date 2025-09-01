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

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.model.process.tables.TPipelinePublicVar
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarDao {

    fun batchSave(
        dslContext: DSLContext,
        publicVarGroupPOs: List<PublicVarPO>
    ) {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            val setup = publicVarGroupPOs.map {
                dslContext.insertInto(this,
                    ID,
                    PROJECT_ID,
                    VAR_NAME,
                    ALIAS,
                    TYPE,
                    VALUE_TYPE,
                    DEFAULT_VALUE,
                    DESC,
                    REFER_COUNT,
                    GROUP_NAME,
                    VERSION,
                    BUILD_FORM_PROPERTY,
                    CREATOR,
                    MODIFIER,
                    CREATE_TIME,
                    UPDATE_TIME,
                ).values(
                        it.id,
                        it.projectId,
                        it.varName,
                        it.alias,
                        it.type.name,
                        it.valueType.value,
                        it.defaultValue?.toString(),
                        it.desc,
                        it.referCount,
                        it.groupName,
                        it.version,
                        it.buildFormProperty,
                        it.creator,
                        it.modifier,
                        it.createTime,
                        it.updateTime,
                    )
            }
            dslContext.batch(setup).execute()
        }
    }

    fun listGroupNamesByVarName(
        dslContext: DSLContext,
        projectId: String,
        keyword: String
    ): List<String> {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            return dslContext.selectDistinct(GROUP_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VAR_NAME.like("%$keyword%"))
                .fetchInto(String::class.java)
        }
    }

    fun listGroupNamesByVarAlias(
        dslContext: DSLContext,
        projectId: String,
        keyword: String
    ): List<String> {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            return dslContext.selectDistinct(GROUP_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ALIAS.like("%$keyword%"))
                .fetchInto(String::class.java)
        }
    }

    fun listGroupNamesByVarType(
        dslContext: DSLContext,
        projectId: String,
        type: String
    ): List<String> {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            return dslContext.selectDistinct(GROUP_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(TYPE.eq(type))
                .fetchInto(String::class.java)
        }
    }

    fun listVarByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varNameList: List<String>? = null
    ): List<PublicVarPO> {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            val query = dslContext.selectFrom(this)
                .where(GROUP_NAME.eq(groupName))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(version))
                .apply {
                    varNameList?.takeIf { it.isNotEmpty() }?.let {
                        and(VAR_NAME.`in`(it))
                    }
                }

            return query.fetch().map {
                PublicVarPO(
                    id = it.id,
                    projectId = it.projectId,
                    varName = it.varName,
                    alias = it.alias,
                    type = PublicVarTypeEnum.valueOf(it.type),
                    valueType = BuildFormPropertyType.fromValue(it.valueType),
                    defaultValue = it.defaultValue,
                    desc = it.desc,
                    referCount = it.referCount,
                    groupName = it.groupName,
                    version = it.version,
                    buildFormProperty = it.buildFormProperty,
                    creator = it.creator,
                    modifier = it.modifier,
                    createTime = it.createTime,
                    updateTime = it.updateTime
                )
            }
        }
    }

    fun deleteByGroupName(dslContext: DSLContext, projectId: String, groupName: String) {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }

    fun queryVarNamesByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int
    ): List<String> {
        with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
            with(TPipelinePublicVar.T_PIPELINE_PUBLIC_VAR) {
                return dslContext.select(VAR_NAME).from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(GROUP_NAME.eq(groupName))
                    .and(VERSION.eq(version))
                    .groupBy(VAR_NAME)
                    .fetch().map { it.value1() }
            }
        }
    }
}

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
import com.tencent.devops.model.process.tables.TResourcePublicVar
import com.tencent.devops.model.process.tables.TResourcePublicVarGroup
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarDao {

    /**
     * 批量保存公共变量
     */
    fun batchSave(
        dslContext: DSLContext,
        publicVarPOs: List<PublicVarPO>
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            if (publicVarPOs.isEmpty()) return

            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                VAR_NAME,
                ALIAS,
                TYPE,
                VALUE_TYPE,
                DEFAULT_VALUE,
                DESC,
                GROUP_NAME,
                VERSION,
                BUILD_FORM_PROPERTY,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            ).also { insert ->
                publicVarPOs.forEach { record ->
                    insert.values(
                        record.id,
                        record.projectId,
                        record.varName,
                        record.alias,
                        record.type.name,
                        record.valueType.name,
                        record.defaultValue?.toString(),
                        record.desc,
                        record.groupName,
                        record.version,
                        record.buildFormProperty,
                        record.creator,
                        record.modifier,
                        record.createTime,
                        record.updateTime
                    )
                }
            }.onDuplicateKeyUpdate()
                .set(ALIAS, org.jooq.util.mysql.MySQLDSL.values(ALIAS))
                .set(TYPE, org.jooq.util.mysql.MySQLDSL.values(TYPE))
                .set(VALUE_TYPE, org.jooq.util.mysql.MySQLDSL.values(VALUE_TYPE))
                .set(DEFAULT_VALUE, org.jooq.util.mysql.MySQLDSL.values(DEFAULT_VALUE))
                .set(DESC, org.jooq.util.mysql.MySQLDSL.values(DESC))
                .set(BUILD_FORM_PROPERTY, org.jooq.util.mysql.MySQLDSL.values(BUILD_FORM_PROPERTY))
                .set(MODIFIER, org.jooq.util.mysql.MySQLDSL.values(MODIFIER))
                .set(UPDATE_TIME, org.jooq.util.mysql.MySQLDSL.values(UPDATE_TIME))
                .execute()
        }
    }

    /**
     * 根据变量名关键字查询变量组名称列表
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param keyword 变量名关键字
     * @return 变量组名称列表（去重）
     */
    fun listGroupNamesByVarName(
        dslContext: DSLContext,
        projectId: String,
        keyword: String
    ): List<String> {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            return dslContext.selectDistinct(GROUP_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(VAR_NAME.contains(keyword))
                .fetchInto(String::class.java)
        }
    }

    /**
     * 根据变量别名关键字查询变量组名称列表
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param keyword 变量别名关键字
     * @return 变量组名称列表（去重）
     */
    fun listGroupNamesByVarAlias(
        dslContext: DSLContext,
        projectId: String,
        keyword: String
    ): List<String> {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            return dslContext.selectDistinct(GROUP_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ALIAS.contains(keyword))
                .fetchInto(String::class.java)
        }
    }

    /**
     * 根据变量组名和版本查询变量列表
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号，-1表示查询最新版本
     * @param varNameList 变量名列表（可选，用于过滤特定变量）
     * @return 公共变量PO列表
     */
    fun listVarByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varNameList: List<String>? = null
    ): List<PublicVarPO> {
        // 当 version == -1 时，查询变量组最新版本
        val actualVersion = if (version == -1) {
            val varGroup = TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP
            dslContext.select(varGroup.VERSION)
                .from(varGroup)
                .where(varGroup.PROJECT_ID.eq(projectId))
                .and(varGroup.GROUP_NAME.eq(groupName))
                .and(varGroup.LATEST_FLAG.eq(true))
                .fetchOne(0, Int::class.java) ?: return emptyList()
        } else {
            version
        }

        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            val query = dslContext.selectFrom(this)
                .where(GROUP_NAME.eq(groupName))
                .and(PROJECT_ID.eq(projectId))
                .and(VERSION.eq(actualVersion))
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
                    valueType = BuildFormPropertyType.valueOf(it.valueType),
                    defaultValue = it.defaultValue,
                    desc = it.desc,
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

    /**
     * 根据变量组名删除所有版本的变量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     */
    fun deleteByGroupName(dslContext: DSLContext, projectId: String, groupName: String) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }
}

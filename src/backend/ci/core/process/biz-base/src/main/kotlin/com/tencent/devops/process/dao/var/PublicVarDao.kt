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
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarDao {

    /**
     * 批量保存公共变量
     */
    fun batchSave(
        dslContext: DSLContext,
        publicVarGroupPOs: List<PublicVarPO>
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            if (publicVarGroupPOs.isEmpty()) return
            
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
                REFER_COUNT,
                GROUP_NAME,
                VERSION,
                BUILD_FORM_PROPERTY,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            ).also { insert ->
                publicVarGroupPOs.forEach { record ->
                    insert.values(
                        record.id,
                        record.projectId,
                        record.varName,
                        record.alias,
                        record.type.name,
                        record.valueType.name,
                        record.defaultValue?.toString(),
                        record.desc,
                        record.referCount,
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
     * @param version 版本号
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
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
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
                    valueType = BuildFormPropertyType.valueOf(it.valueType),
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

    /**
     * 查询指定变量组和版本的所有变量名
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @return 变量名列表（去重）
     */
    fun queryVarNamesByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int
    ): List<String> {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
                return dslContext.select(VAR_NAME).from(this)
                    .where(PROJECT_ID.eq(projectId))
                    .and(GROUP_NAME.eq(groupName))
                    .and(VERSION.eq(version))
                    .groupBy(VAR_NAME)
                    .fetch().map { it.value1() }
            }
        }
    }

    /**
     * 更新变量引用计数（增量更新）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @param varName 变量名
     * @param change 计数变化量（正数表示增加，负数表示减少）
     */
    fun updateReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varName: String,
        change: Int
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            val updateQuery = dslContext.update(this)
                .set(REFER_COUNT, REFER_COUNT.plus(change))
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.eq(varName))

            // 减少引用计数时，确保不会变成负数
            if (change < 0) {
                updateQuery.and(REFER_COUNT.ge(-change))
            }

            updateQuery.execute()
        }
    }

    /**
     * 直接设置变量引用计数（非增量更新）
     * 用于基于实际统计结果直接更新引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @param varName 变量名
     * @param referCount 引用计数值
     */
    fun updateReferCountDirectly(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varName: String,
        referCount: Int
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            dslContext.update(this)
                .set(REFER_COUNT, referCount)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.eq(varName))
                .execute()
        }
    }
}
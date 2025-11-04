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

    fun batchSave(
        dslContext: DSLContext,
        publicVarGroupPOs: List<PublicVarPO>
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            if (publicVarGroupPOs.isEmpty()) return
            val batchInsert = dslContext.insertInto(
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
            )
            publicVarGroupPOs.forEach {
                batchInsert.values(
                    it.id,
                    it.projectId,
                    it.varName,
                    it.alias,
                    it.type.name,
                    it.valueType.name,
                    it.defaultValue?.toString(),
                    it.desc,
                    it.referCount,
                    it.groupName,
                    it.version,
                    it.buildFormProperty,
                    it.creator,
                    it.modifier,
                    it.createTime,
                    it.updateTime
                )
            }
            batchInsert.execute()
        }
    }

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

    fun listGroupNamesByVarType(
        dslContext: DSLContext,
        projectId: String,
        type: String
    ): List<String> {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
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

    fun deleteByGroupName(dslContext: DSLContext, projectId: String, groupName: String) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
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
     * 更新变量引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @param varName 变量名
     * @param referCount 新的引用计数
     */
    fun updateReferCount(
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

    /**
     * 按变量组批量更新引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @param countChange 计数变化量（正数表示增加，负数表示减少）
     */
    fun updateReferCountByGroup(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        countChange: Int
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            dslContext.update(this)
                .set(REFER_COUNT, REFER_COUNT.plus(countChange))
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    /**
     * 减少变量引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @param varName 变量名
     */
    fun decrementReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varName: String
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            dslContext.update(this)
                .set(REFER_COUNT, REFER_COUNT.minus(1))
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.eq(varName))
                .and(REFER_COUNT.gt(0)) // 确保引用计数不会变成负数
                .execute()
        }
    }

    /**
     * 增加变量引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @param varName 变量名
     */
    fun incrementReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varName: String
    ) {
        with(TResourcePublicVar.T_RESOURCE_PUBLIC_VAR) {
            dslContext.update(this)
                .set(REFER_COUNT, REFER_COUNT.plus(1))
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.eq(varName))
                .execute()
        }
    }
}
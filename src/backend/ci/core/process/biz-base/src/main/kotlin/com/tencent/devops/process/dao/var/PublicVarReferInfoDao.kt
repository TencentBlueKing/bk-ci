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

import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.model.process.tables.TResourcePublicVarReferInfo
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarReferInfoDao {

    fun batchSave(
        dslContext: DSLContext,
        pipelinePublicVarReferPOs: List<ResourcePublicVarReferPO>
    ) {
        if (pipelinePublicVarReferPOs.isEmpty()) {
            return
        }
        
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            var insertQuery = dslContext.insertInto(
                this,
                ID, PROJECT_ID,
                GROUP_NAME,
                VAR_NAME,
                VERSION,
                REFER_ID,
                REFER_TYPE,
                REFER_VERSION,
                REFER_VERSION_NAME,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
            
            pipelinePublicVarReferPOs.forEach { po ->
                insertQuery = insertQuery.values(
                    po.id,
                    po.projectId,
                    po.groupName,
                    po.varName,
                    po.version,
                    po.referId,
                    po.referType.name,
                    po.referVersion,
                    po.referVersionName,
                    po.creator,
                    po.modifier,
                    po.createTime,
                    po.updateTime
                )
            }
            insertQuery.execute()
        }
    }

    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersionName: String? = null
    ) {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
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
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
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
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
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
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
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

    fun deleteByReferIdAndGroup(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int
    ) {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))
                .and(REFER_VERSION.eq(referVersion))
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
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
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

    /**
     * 根据引用ID和变量组查询变量引用信息列表
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param groupName 变量组名
     * @param referVersion 引用版本
     * @return 变量引用信息列表
     */
    fun listVarReferInfoByReferIdAndGroup(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int
    ): List<ResourcePublicVarReferPO> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))
                .and(REFER_VERSION.eq(referVersion))
                .fetch()
                .map {
                    ResourcePublicVarReferPO(
                        id = it.id,
                        projectId = it.projectId,
                        groupName = it.groupName,
                        varName = it.varName,
                        version = it.version,
                        referId = it.referId,
                        referType = PublicVerGroupReferenceTypeEnum.valueOf(it.referType),
                        referVersion = it.referVersion,
                        referVersionName = it.referVersionName,
                        creator = it.creator,
                        modifier = it.modifier,
                        createTime = it.createTime,
                        updateTime = it.updateTime
                    )
                }
        }
    }

    /**
     * 根据引用ID、变量组和变量名列表删除引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param groupName 变量组名
     * @param referVersion 引用版本
     * @param varNames 变量名列表
     */
    fun deleteByReferIdAndGroupAndVarNames(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int,
        varNames: List<String>
    ) {
        if (varNames.isEmpty()) {
            return
        }
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))
                .and(REFER_VERSION.eq(referVersion))
                .and(VAR_NAME.`in`(varNames))
                .execute()
        }
    }

    /**
     * 根据引用ID、引用类型和引用版本查询所有变量引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本
     * @return 变量引用信息列表
     */
    fun listVarReferInfoByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ): List<ResourcePublicVarReferPO> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))
                .fetch()
                .map {
                    ResourcePublicVarReferPO(
                        id = it.id,
                        projectId = it.projectId,
                        groupName = it.groupName,
                        varName = it.varName,
                        version = it.version,
                        referId = it.referId,
                        referType = PublicVerGroupReferenceTypeEnum.valueOf(it.referType),
                        referVersion = it.referVersion,
                        referVersionName = it.referVersionName,
                        creator = it.creator,
                        modifier = it.modifier,
                        createTime = it.createTime,
                        updateTime = it.updateTime
                    )
                }
        }
    }

    /**
     * 根据引用ID、引用类型和引用版本查询变量组信息（只查询groupName和version字段）
     * 获取字段用于对比
     * @return 变量组信息Map，key为"groupName:version"
     */
    fun listVarGroupsByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int
    ): Map<String, Pair<String, Int>> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.selectDistinct(GROUP_NAME, VERSION)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))
                .fetch()
                .associate { record ->
                    val groupName = record.value1()
                    val version = record.value2() ?: -1
                    val key = "$groupName:$version"
                    key to Pair(groupName, version)
                }
        }
    }

    /**
     * 根据引用ID、引用类型、引用版本、变量组名和版本查询变量名列表
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本
     * @param groupName 变量组名
     * @param version 变量组版本
     * @return 变量名列表
     */
    fun listVarNamesByReferIdAndGroup(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int,
        groupName: String,
        version: Int
    ): List<String> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.select(VAR_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .fetch()
                .map { it.value1() }
        }
    }

    /**
     * 根据引用ID、引用类型、引用版本、变量组名和变量名删除单个变量引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本
     * @param groupName 变量组名
     * @param varName 变量名
     */
    fun deleteByReferIdAndVar(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVerGroupReferenceTypeEnum,
        referVersion: Int,
        groupName: String,
        varName: String
    ) {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
                .execute()
        }
    }

}
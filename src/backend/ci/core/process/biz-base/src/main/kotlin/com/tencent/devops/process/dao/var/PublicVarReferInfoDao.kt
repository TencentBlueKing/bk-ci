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

import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.model.process.tables.TResourcePublicVarReferInfo
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarReferPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarReferInfoDao {

    /**
     * 批量保存变量引用信息
     * @param dslContext 数据库上下文
     * @param pipelinePublicVarReferPOs 变量引用PO列表
     */
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

    /**
     * 根据引用ID删除变量引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersionName 引用版本名称（可选）
     */
    fun deleteByReferId(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
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

    /**
     * 根据引用ID和变量组名删除变量引用记录（支持可选版本过滤）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param groupName 变量组名
     * @param referVersion 引用版本（可选，为null时删除该组所有版本的引用）
     */
    fun deleteByReferIdAndGroup(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        groupName: String,
        referVersion: Int? = null
    ) {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            val deleteQuery = dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.eq(groupName))

            if (referVersion != null) {
                deleteQuery.and(REFER_VERSION.eq(referVersion))
            }

            deleteQuery.execute()
        }
    }

    /**
     * 批量删除多个变量组的引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param groupNames 变量组名列表
     * @param referVersion 引用版本
     */
    fun batchDeleteByReferIdAndGroups(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        groupNames: List<String>,
        referVersion: Int
    ) {
        if (groupNames.isEmpty()) {
            return
        }
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(GROUP_NAME.`in`(groupNames))
                .and(REFER_VERSION.eq(referVersion))
                .execute()
        }
    }

    /**
     * 删除指定资源版本的所有变量引用记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本号
     */
    fun deleteByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int
    ) {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))
                .execute()
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
        referType: PublicVarGroupReferenceTypeEnum,
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
     * 根据引用ID、引用类型和引用版本查询变量引用信息
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param referId 引用ID
     * @param referType 引用类型
     * @param referVersion 引用版本
     * @param groupName 变量组名（可选）
     * @param version 版本号（可选）
     * @return 变量引用信息列表
     */
    fun listVarReferInfoByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int,
        groupName: String? = null,
        version: Int? = null
    ): List<ResourcePublicVarReferPO> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            var conditions = dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))

            // 添加可选的 groupName 条件
            if (groupName != null) {
                conditions = conditions.and(GROUP_NAME.eq(groupName))
            }

            // 添加可选的 version 条件
            if (version != null) {
                conditions = conditions.and(VERSION.eq(version))
            }

            return conditions.fetch()
                .map {
                    ResourcePublicVarReferPO(
                        id = it.id,
                        projectId = it.projectId,
                        groupName = it.groupName,
                        varName = it.varName,
                        version = it.version,
                        referId = it.referId,
                        referType = PublicVarGroupReferenceTypeEnum.valueOf(it.referType),
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
     * @return 变量组PublicGroupKey集合
     */
    fun listVarGroupsByReferIdAndVersion(
        dslContext: DSLContext,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int
    ): Set<com.tencent.devops.process.pojo.`var`.PublicGroupKey> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.selectDistinct(GROUP_NAME, VERSION)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REFER_ID.eq(referId))
                .and(REFER_TYPE.eq(referType.name))
                .and(REFER_VERSION.eq(referVersion))
                .fetch()
                .map { record ->
                    val groupName = record.value1()
                    val version = record.value2()
                    com.tencent.devops.process.pojo.`var`.PublicGroupKey(
                        groupName = groupName,
                        version = if (version == -1) null else version
                    )
                }
                .toSet()
        }
    }

    /**
     * 统计指定变量的不同 referId 数量（跨版本去重）
     * 计数原则：referId + varName 的唯一组合计数为1，跨版本去重
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 变量组版本
     * @param varName 变量名
     * @return 不同 referId 的数量
     */
    fun countDistinctReferIdsByVar(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varName: String
    ): Int {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.select(DSL.countDistinct(REFER_ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.eq(varName))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 根据变量名和版本列表查询引用该变量的资源ID列表（去重）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param varName 变量名
     * @param versions 变量组版本列表
     * @param referType 引用类型（可选）
     * @return 引用ID列表（去重）
     */
    fun listReferIdsByVarNameAndVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        versions: List<Int>,
        referType: PublicVarGroupReferenceTypeEnum?
    ): List<String> {
        if (versions.isEmpty()) {
            return emptyList()
        }

        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                GROUP_NAME.eq(groupName),
                VAR_NAME.eq(varName),
                VERSION.`in`(versions)
            )
            referType?.let { conditions.add(REFER_TYPE.eq(it.name)) }

            return dslContext.selectDistinct(REFER_ID)
                .from(this)
                .where(conditions)
                .fetch()
                .map { it.value1() }
        }
    }

    /**
     * 查询指定变量和版本下已存在引用的 referId 集合
     * 用于判断是否需要增加引用计数
     * @param dslContext 数据库上下文
     * @param projectId 项目ID（引用记录所在的项目）
     * @param groupName 变量组名
     * @param varName 变量名
     * @param version 变量组版本
     * @param referIds 待检查的引用ID列表
     * @return 已存在引用的 referId 集合
     */
    fun listExistingReferIdsByVarAndVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varName: String,
        version: Int,
        referIds: List<String>
    ): Set<String> {
        if (referIds.isEmpty()) {
            return emptySet()
        }

        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.selectDistinct(REFER_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.eq(varName))
                .and(VERSION.eq(version))
                .and(REFER_ID.`in`(referIds))
                .fetch()
                .mapNotNull { it.value1() }
                .toSet()
        }
    }

    /**
     * 批量统计多个变量的引用数量（跨所有版本，按 referId 去重）
     * 单个流水线即使通过多个版本引用同一变量，也只计为 1 次引用。
     * 注意：该 SQL 直接聚合 T_RESOURCE_PUBLIC_VAR_REFER_INFO，不区分流水线最新/历史版本，
     * 如果 DB 中存在未清理的历史版本引用记录，可能导致计数偏高。
     * 变量组详情页等需要精确"当前最新有效引用"计数的场景，应改用
     * `PublicVarVersionSummaryDao.batchGetActiveReferCount`（Summary 表由保存/卸载链路维护）。
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param varNames 变量名列表
     * @return Map<String, Int> 变量名到引用数量的映射
     */
    fun batchCountDistinctReferIdsAcrossVersionsByVars(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) {
            return emptyMap()
        }

        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            val countField = DSL.countDistinct(REFER_ID)
            val dbResult = dslContext.select(VAR_NAME, countField)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VAR_NAME.`in`(varNames))
                .groupBy(VAR_NAME)
                .fetch()
                .associate { record ->
                    record.getValue(VAR_NAME) to (record.get(countField) ?: 0)
                }
            // 为未查询到的变量名补充默认值0，确保返回map覆盖所有输入变量名
            return varNames.associateWith { dbResult[it] ?: 0 }
        }
    }

    /**
     * 批量统计多个变量的引用数量（按 referId 去重）
     * 用于查询变量列表时显示实际引用该变量的资源数量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 变量组版本
     * @param varNames 变量名列表
     * @return Map<String, Int> 变量名到引用数量的映射
     */
    fun batchCountDistinctReferIdsByVars(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        varNames: List<String>
    ): Map<String, Int> {
        if (varNames.isEmpty()) {
            return emptyMap()
        }

        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            return dslContext.select(VAR_NAME, DSL.countDistinct(REFER_ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .and(VAR_NAME.`in`(varNames))
                .groupBy(VAR_NAME)
                .fetch()
                .associate { record ->
                    record.getValue(VAR_NAME) to (record.getValue(1, Int::class.java) ?: 0)
                }
        }
    }

    /**
     * 统计每个 referId 在指定活跃版本中引用某变量组的变量数量
     * 同一 referId 的多个活跃版本（最新版本、分支版本、草稿）引用同一变量计为 1
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param referIdVersions referId 到活跃版本列表的映射
     * @return Map<referId, count> 每个资源引用的去重变量数量
     */
    fun countVarReferByGroupAndActiveVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referIdVersions: Map<String, List<Int>>
    ): Map<String, Int> {
        if (referIdVersions.isEmpty()) {
            return emptyMap()
        }

        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            // 构造条件：(REFER_ID = ? AND REFER_VERSION IN (?)) OR ...
            val versionConditions = referIdVersions.map { (referId, versions) ->
                REFER_ID.eq(referId).and(REFER_VERSION.`in`(versions))
            }.reduce { acc, condition -> acc.or(condition) }

            return dslContext.select(REFER_ID, DSL.countDistinct(VAR_NAME))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(versionConditions)
                .groupBy(REFER_ID)
                .fetch()
                .associate { record ->
                    record.getValue(REFER_ID) to (record.getValue(1, Int::class.java) ?: 0)
                }
        }
    }

    /**
     * 查询指定变量组下有实际变量引用的不同 referId 列表
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param referType 引用类型（可选）
     * @param varName 变量名（可选）
     * @return 有实际变量引用的 referId 列表
     */
    fun listReferIdsWithActualVarRefer(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referType: PublicVarGroupReferenceTypeEnum? = null,
        varName: String? = null
    ): List<String> {
        with(TResourcePublicVarReferInfo.T_RESOURCE_PUBLIC_VAR_REFER_INFO) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                GROUP_NAME.eq(groupName)
            )
            referType?.let { conditions.add(REFER_TYPE.eq(it.name)) }
            varName?.let { conditions.add(VAR_NAME.eq(it)) }

            return dslContext.selectDistinct(REFER_ID)
                .from(this)
                .where(conditions)
                .fetch()
                .map { it.value1() }
        }
    }
}

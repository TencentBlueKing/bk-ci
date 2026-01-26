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

import com.tencent.devops.model.process.tables.TResourcePublicVarGroup
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPO
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupDao {

    /**
     * 将数据库记录转换为PublicVarGroupPO对象的公共方法
     * @param record 数据库记录
     * @return PublicVarGroupPO对象
     */
    private fun mapRecordToPublicVarGroupPO(record: org.jooq.Record): PublicVarGroupPO {
        return PublicVarGroupPO(
            id = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.ID),
            projectId = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.PROJECT_ID),
            groupName = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.GROUP_NAME),
            version = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.VERSION),
            versionName = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.VERSION_NAME),
            latestFlag = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.LATEST_FLAG),
            desc = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.DESC),
            varCount = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.VAR_COUNT),
            creator = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.CREATOR),
            modifier = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.MODIFIER),
            createTime = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.CREATE_TIME),
            updateTime = record.getValue(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP.UPDATE_TIME)
        )
    }

    /**
     * 保存变量组信息
     * @param dslContext 数据库上下文
     * @param publicVarGroupPO 变量组PO对象
     */
    fun save(
        dslContext: DSLContext,
        publicVarGroupPO: PublicVarGroupPO
    ) {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            dslContext.insertInto(this)
                .set(ID, publicVarGroupPO.id)
                .set(PROJECT_ID, publicVarGroupPO.projectId)
                .set(GROUP_NAME, publicVarGroupPO.groupName)
                .set(VERSION, publicVarGroupPO.version)
                .set(VERSION_NAME, publicVarGroupPO.versionName)
                .set(LATEST_FLAG, publicVarGroupPO.latestFlag)
                .set(DESC, publicVarGroupPO.desc)
                .set(VAR_COUNT, publicVarGroupPO.varCount)
                .set(CREATOR, publicVarGroupPO.creator)
                .set(MODIFIER, publicVarGroupPO.modifier)
                .set(UPDATE_TIME, publicVarGroupPO.updateTime)
                .set(CREATE_TIME, publicVarGroupPO.createTime)
                .execute()
        }
    }

    /**
     * 获取变量组的最新版本号
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @return 最新版本号，如果不存在则返回null
     */
    fun getLatestVersionByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ): Int? {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            return dslContext.select(VERSION).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(LATEST_FLAG.eq(true))
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 批量获取多个组的最新版本
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名列表
     * @return Map<变量组名, 版本号>
     */
    fun getLatestVersionsByGroupNames(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            return dslContext.select(GROUP_NAME, VERSION).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .and(LATEST_FLAG.eq(true))
                .fetch()
                .associate { record ->
                    record.getValue(GROUP_NAME) to record.getValue(VERSION)
                }
        }
    }

    /**
     * 获取指定版本之前的最大版本号（前一个版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param currentVersion 当前版本号
     * @return 前一个版本号，如果不存在则返回null
     */
    fun getPreviousVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        currentVersion: Int
    ): Int? {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            return dslContext.select(VERSION)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.lt(currentVersion))
                .orderBy(VERSION.desc())
                .limit(1)
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 查询项目下所有最新版本的变量组
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @return 变量组PO列表
     */
    fun listGroupsByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): List<PublicVarGroupPO> {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LATEST_FLAG.eq(true))
                .fetch { record -> mapRecordToPublicVarGroupPO(record) }
        }
    }

    /**
     * 查询项目下所有最新版本的变量组名称
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @return 变量组名称列表
     */
    fun listGroupsNameByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            return dslContext.select(GROUP_NAME).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LATEST_FLAG.eq(true))
                .fetchInto(String::class.java)
        }
    }

    /**
     * 构建变量组查询的公共条件
     * @param projectId 项目ID
     * @param filterByGroupName 按变量组名过滤（模糊匹配）
     * @param filterByGroupDesc 按描述过滤（模糊匹配）
     * @param filterByUpdater 按更新人过滤（模糊匹配）
     * @param groupNames 按变量组名列表过滤（精确匹配）
     * @return 查询条件列表
     */
    private fun buildGroupQueryConditions(
        projectId: String,
        filterByGroupName: String? = null,
        filterByGroupDesc: String? = null,
        filterByUpdater: String? = null,
        groupNames: List<String>? = null
    ): List<Condition> {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(LATEST_FLAG.eq(true))

            // 添加筛选条件
            filterByGroupName?.let {
                conditions.add(GROUP_NAME.like("%$it%"))
            }
            filterByGroupDesc?.let {
                conditions.add(DESC.like("%$it%"))
            }
            filterByUpdater?.let {
                conditions.add(MODIFIER.like("%$it%"))
            }
            if (!groupNames.isNullOrEmpty()) {
                conditions.add(GROUP_NAME.`in`(groupNames))
            }

            return conditions
        }
    }

    /**
     * 分页查询项目下的变量组（最新版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param filterByGroupName 按变量组名过滤
     * @param filterByGroupDesc 按描述过滤
     * @param filterByUpdater 按更新人过滤
     * @param groupNames 按变量组名列表过滤
     * @return 变量组PO列表
     */
    fun listGroupsByProjectIdPage(
        dslContext: DSLContext,
        projectId: String,
        page: Int,
        pageSize: Int,
        filterByGroupName: String? = null,
        filterByGroupDesc: String? = null,
        filterByUpdater: String? = null,
        groupNames: List<String>? = null
    ): List<PublicVarGroupPO> {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            val conditions = buildGroupQueryConditions(
                projectId = projectId,
                filterByGroupName = filterByGroupName,
                filterByGroupDesc = filterByGroupDesc,
                filterByUpdater = filterByUpdater,
                groupNames = groupNames
            )

            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(UPDATE_TIME.desc())
                .limit(pageSize)
                .offset((page - 1) * pageSize)
                .fetch { record -> mapRecordToPublicVarGroupPO(record) }
        }
    }

    /**
     * 统计项目下符合条件的变量组数量（最新版本）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param filterByGroupName 按变量组名过滤
     * @param filterByGroupDesc 按描述过滤
     * @param filterByUpdater 按更新人过滤
     * @param groupNames 按变量组名列表过滤
     * @return 变量组数量
     */
    fun countGroupsByProjectId(
        dslContext: DSLContext,
        projectId: String,
        filterByGroupName: String? = null,
        filterByGroupDesc: String? = null,
        filterByUpdater: String? = null,
        groupNames: List<String>? = null
    ): Long {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            val conditions = buildGroupQueryConditions(
                projectId = projectId,
                filterByGroupName = filterByGroupName,
                filterByGroupDesc = filterByGroupDesc,
                filterByUpdater = filterByUpdater,
                groupNames = groupNames
            )

            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java) ?: 0
        }
    }

    /**
     * 根据变量组名查询变量组记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号（可选）
     * @param versionName 版本名称（可选）
     * @return 变量组PO对象，如果不存在则返回null
     */
    fun getRecordByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int? = null,
        versionName: String? = null
    ): PublicVarGroupPO? {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            if (version == null && versionName == null) {
                conditions.add(LATEST_FLAG.eq(true))
            } else {
                if (version != null) {
                    conditions.add(VERSION.eq(version))
                } else {
                    conditions.add(VERSION_NAME.eq(versionName))
                }
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetchOne()?.let { record -> mapRecordToPublicVarGroupPO(record) }
        }
    }

    /**
     * 根据变量组名删除所有版本的变量组
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     */
    fun deleteByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ) {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }

    /**
     * 更新变量组的最新版本标识
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param latestFlag 最新版本标识
     */
    fun updateLatestFlag(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        latestFlag: Boolean
    ) {
        with(TResourcePublicVarGroup.T_RESOURCE_PUBLIC_VAR_GROUP) {
            dslContext.update(this)
                .set(LATEST_FLAG, latestFlag)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(LATEST_FLAG.eq(true))
                .execute()
        }
    }

}
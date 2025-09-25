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

import com.tencent.devops.model.process.tables.TPipelinePublicVarGroup
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPO
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupDao {

    /**
     * 将数据库记录转换为PublicVarGroupPO对象的公共方法
     */
    private fun mapRecordToPublicVarGroupPO(record: org.jooq.Record): PublicVarGroupPO {
        return PublicVarGroupPO(
            id = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.ID),
            projectId = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.PROJECT_ID),
            groupName = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.GROUP_NAME),
            version = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.VERSION),
            versionName = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.VERSION_NAME),
            latestFlag = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.LATEST_FLAG),
            desc = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.DESC),
            referCount = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.REFER_COUNT),
            varCount = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.VAR_COUNT),
            creator = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.CREATOR),
            modifier = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.MODIFIER),
            createTime = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.CREATE_TIME),
            updateTime = record.getValue(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP.UPDATE_TIME)
        )
    }

    fun save(
        dslContext: DSLContext,
        publicVarGroupPO: PublicVarGroupPO
    ) {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            dslContext.insertInto(this)
                .set(ID, publicVarGroupPO.id)
                .set(PROJECT_ID, publicVarGroupPO.projectId)
                .set(GROUP_NAME, publicVarGroupPO.groupName)
                .set(VERSION, publicVarGroupPO.version)
                .set(LATEST_FLAG, publicVarGroupPO.latestFlag)
                .set(DESC, publicVarGroupPO.desc)
                .set(REFER_COUNT, publicVarGroupPO.referCount)
                .set(VAR_COUNT, publicVarGroupPO.varCount)
                .set(CREATOR, publicVarGroupPO.creator)
                .set(MODIFIER, publicVarGroupPO.modifier)
                .set(UPDATE_TIME, publicVarGroupPO.updateTime)
                .set(CREATE_TIME, publicVarGroupPO.createTime)
                .execute()
        }
    }

    fun getLatestVersionByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ): Int? {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            return dslContext.select(VERSION).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(LATEST_FLAG.eq(true))
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 批量获取多个组的最新版本
     */
    fun getLatestVersionsByGroupNames(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
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

    fun listGroupsByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): List<PublicVarGroupPO> {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LATEST_FLAG.eq(true))
                .fetch { record -> mapRecordToPublicVarGroupPO(record) }
        }
    }

    fun listGroupsNameByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): List<String> {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            return dslContext.select(GROUP_NAME).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LATEST_FLAG.eq(true))
                .fetchInto(String::class.java)
        }
    }

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
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(LATEST_FLAG.eq(true))

            // 添加新的筛选条件
            filterByGroupName?.let {
                conditions.add(GROUP_NAME.like("%$it%"))
            }
            filterByGroupDesc?.let {
                conditions.add(DESC.like("%$it%"))
            }
            filterByUpdater?.let {
                conditions.add(MODIFIER.like("%$it%"))
            }
            if(!groupNames.isNullOrEmpty()) {
                conditions.add(GROUP_NAME.`in`(groupNames))
            }

            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(UPDATE_TIME.desc())
                .limit(pageSize)
                .offset((page - 1) * pageSize)
                .fetch { record -> mapRecordToPublicVarGroupPO(record) }
        }
    }

    fun countGroupsByProjectId(
        dslContext: DSLContext,
        projectId: String,
        filterByGroupName: String? = null,
        filterByGroupDesc: String? = null,
        filterByUpdater: String? = null,
        groupNames: List<String>? = null
    ): Long {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            val condition = DSL.noCondition()
                .and(PROJECT_ID.eq(projectId))
                .and(LATEST_FLAG.eq(true))

            // 添加新的筛选条件
            filterByGroupName?.let {
                condition.and(GROUP_NAME.like("%$it%"))
            }
            filterByGroupDesc?.let {
                condition.and(DESC.like("%$it%"))
            }
            filterByUpdater?.let {
                condition.and(MODIFIER.like("%$it%"))
            }
            if (!groupNames.isNullOrEmpty()) {
                condition.and(GROUP_NAME.`in`(groupNames))
            }

            return dslContext.selectCount()
                .from(this)
                .where(condition)
                .fetchOne(0, Long::class.java) ?: 0
        }
    }

    fun getRecordByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int? = null,
        versionName: String? = null
    ): PublicVarGroupPO? {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
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

    fun countRecordByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int? = null
    ): Int {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(projectId))
            conditions.add(GROUP_NAME.eq(groupName))
            if (version == null) {
                conditions.add(LATEST_FLAG.eq(true))
            } else {
                conditions.add(VERSION.eq(version))
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    fun deleteByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupName: String
    ) {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }

    fun updateReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int,
        referCount: Int
    ) {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            dslContext.update(this)
                .set(REFER_COUNT, referCount)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .execute()
        }
    }

    fun updateLatestFlag(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        latestFlag: Boolean
    ) {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            dslContext.update(this)
                .set(LATEST_FLAG, latestFlag)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(LATEST_FLAG.eq(true))
                .execute()
        }
    }

    fun updateVarGroupNameReferCount(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        referCount: Int
    ) {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            dslContext.update(this)
                .set(REFER_COUNT, referCount)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }

    /**
     * 批量获取变量组的VAR_COUNT
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名称列表
     * @return Map<String, Int> 变量组名称到VAR_COUNT的映射
     */
    fun getVarCountsByGroupNames(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            return dslContext.select(GROUP_NAME, VAR_COUNT)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .and(LATEST_FLAG.eq(true))
                .fetch()
                .associate { record ->
                    record.getValue(GROUP_NAME) to record.getValue(VAR_COUNT)
                }
        }
    }

    /**
     * 批量查询最新版本变量组的varCount
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupNames 变量组名称列表
     * @return Map<String, Int> 变量组名称到VAR_COUNT的映射
     */
    fun batchGetLatestVarCountsByGroupNames(
        dslContext: DSLContext,
        projectId: String,
        groupNames: List<String>
    ): Map<String, Int> {
        if (groupNames.isEmpty()) return emptyMap()
        
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            return dslContext.select(GROUP_NAME, VAR_COUNT)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.`in`(groupNames))
                .and(LATEST_FLAG.eq(true))
                .fetch()
                .associate { record ->
                    record.getValue(GROUP_NAME) to record.getValue(VAR_COUNT)
                }
        }
    }

    /**
     * 批量查询指定版本变量组的varCount
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupVersions 变量组名称和版本的映射列表
     * @return Map<Pair<String, Int>, Int> (变量组名称,版本)到VAR_COUNT的映射
     */
    fun batchGetSpecificVarCountsByGroupVersions(
        dslContext: DSLContext,
        projectId: String,
        groupVersions: List<Pair<String, Int>>
    ): Map<Pair<String, Int>, Int> {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            if (groupVersions.isEmpty()) return emptyMap()
            
            val orCondition = groupVersions.map { (groupName, version) ->
                GROUP_NAME.eq(groupName).and(VERSION.eq(version))
            }.reduce { acc, condition -> acc.or(condition) }
            
            return dslContext.select(GROUP_NAME, VERSION, VAR_COUNT)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(orCondition)
                .fetch()
                .associate { record ->
                    val groupName = record.getValue(GROUP_NAME)
                    val version = record.getValue(VERSION)
                    val varCount = record.getValue(VAR_COUNT)
                    Pair(groupName, version) to varCount
                }
        }
    }
}
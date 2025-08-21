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

    fun listGroupsByProjectId(
        dslContext: DSLContext,
        projectId: String
    ): List<PublicVarGroupPO> {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(LATEST_FLAG.eq(true))
                .fetch { record ->
                    PublicVarGroupPO(
                        id = record.id,
                        projectId = record.projectId,
                        groupName = record.groupName,
                        version = record.version,
                        latestFlag = record.latestFlag,
                        desc = record.desc,
                        referCount = record.referCount,
                        varCount = record.varCount,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
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
            groupNames?.let {
                conditions.add(GROUP_NAME.`in`(it))
            }

            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(UPDATE_TIME.desc())
                .limit(pageSize)
                .offset((page - 1) * pageSize)
                .fetch { record ->
                    PublicVarGroupPO(
                        id = record.id,
                        projectId = record.projectId,
                        groupName = record.groupName,
                        version = record.version,
                        latestFlag = record.latestFlag,
                        desc = record.desc,
                        referCount = record.referCount,
                        varCount = record.varCount,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
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
            groupNames?.let {
                condition.and(GROUP_NAME.`in`(it))
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
        version: Int? = null
    ): PublicVarGroupPO? {
        with(TPipelinePublicVarGroup.T_PIPELINE_PUBLIC_VAR_GROUP) {
            val condition = if (version == null) {
                LATEST_FLAG.eq(true)
            } else {
                VERSION.eq(version)
            }
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(condition)
                .fetchOne()?.let { record ->
                    PublicVarGroupPO(
                        id = record.id,
                        projectId = record.projectId,
                        groupName = record.groupName,
                        version = record.version,
                        latestFlag = record.latestFlag,
                        desc = record.desc,
                        referCount = record.referCount,
                        varCount = record.varCount,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
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
}

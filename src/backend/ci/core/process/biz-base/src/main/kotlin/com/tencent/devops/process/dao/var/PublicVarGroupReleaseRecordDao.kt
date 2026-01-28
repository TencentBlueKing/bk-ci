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

import com.tencent.devops.model.process.tables.TResourcePublicVarGroupReleaseRecord
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.po.ResourcePublicVarGroupReleaseRecordPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PublicVarGroupReleaseRecordDao {

    /**
     * 批量插入发布记录
     * @param dslContext 数据库上下文
     * @param records 发布记录PO列表
     */
    fun batchInsert(dslContext: DSLContext, records: List<ResourcePublicVarGroupReleaseRecordPO>) {
        if (records.isEmpty()) return

        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            val insertStep = dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                GROUP_NAME,
                VERSION,
                PUBLISHER,
                PUB_TIME,
                DESC,
                CONTENT,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )

            records.forEach { record ->
                insertStep.values(
                    record.id,
                    record.projectId,
                    record.groupName,
                    record.version,
                    record.publisher,
                    record.pubTime,
                    record.desc,
                    record.content,
                    record.creator,
                    record.modifier,
                    record.createTime,
                    record.updateTime
                )
            }

            insertStep.execute()
        }
    }

    /**
     * 根据变量组名删除所有发布记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     */
    fun deleteByGroupName(dslContext: DSLContext, projectId: String, groupName: String) {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }

    /**
     * 统计变量组的发布记录数量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @return 记录数量
     */
    fun countByGroupName(dslContext: DSLContext, projectId: String, groupName: String): Long {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .fetchOne(0, Long::class.java) ?: 0
        }
    }

    /**
     * 统计变量组的不同版本数量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @return 版本数量
     */
    fun countVersionsByGroupName(dslContext: DSLContext, projectId: String, groupName: String): Long {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            return dslContext.selectDistinct(VERSION)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .fetch()
                .size
                .toLong()
        }
    }

    /**
     * 分页查询变量组的发布记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param page 页码
     * @param pageSize 每页大小
     * @return 发布记录PO列表
     */
    fun listByGroupNamePage(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        page: Int,
        pageSize: Int
    ): List<ResourcePublicVarGroupReleaseRecordPO> {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            val offset = (page - 1) * pageSize
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .orderBy(PUB_TIME.desc())
                .offset(offset)
                .limit(pageSize)
                .fetch { record ->
                    ResourcePublicVarGroupReleaseRecordPO(
                        id = record.id,
                        projectId = record.projectId,
                        groupName = record.groupName,
                        version = record.version,
                        publisher = record.publisher,
                        pubTime = record.pubTime,
                        desc = record.desc,
                        content = record.content,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
        }
    }

    /**
     * 分页查询变量组的发布历史（简化版）
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param page 页码
     * @param pageSize 每页大小
     * @return 发布历史DO列表
     */
    fun listGroupReleaseHistory(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        page: Int,
        pageSize: Int
    ): List<PublicVarReleaseDO> {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            val offset = (page - 1) * pageSize
            return dslContext.select(
                GROUP_NAME,
                VERSION,
                PUBLISHER,
                PUB_TIME,
                DESC,
                CONTENT
            ).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .orderBy(PUB_TIME.desc())
                .offset(offset)
                .limit(pageSize)
                .fetch { record ->
                    PublicVarReleaseDO(
                        groupName = record.get(GROUP_NAME),
                        version = record.get(VERSION),
                        publisher = record.get(PUBLISHER),
                        pubTime = record.get(PUB_TIME),
                        desc = record.get(DESC),
                        content = record.get(CONTENT)
                    )
                }
        }
    }

    /**
     * 查询指定版本的所有发布记录
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param version 版本号
     * @return 发布记录DO列表
     */
    fun listAllRecordsByVersion(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        version: Int
    ): List<PublicVarReleaseDO> {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            return dslContext.select(
                GROUP_NAME,
                VERSION,
                PUBLISHER,
                PUB_TIME,
                DESC,
                CONTENT
            ).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .and(VERSION.eq(version))
                .orderBy(CREATE_TIME.asc())
                .fetch { record ->
                    PublicVarReleaseDO(
                        groupName = record.get(GROUP_NAME),
                        version = record.get(VERSION),
                        publisher = record.get(PUBLISHER),
                        pubTime = record.get(PUB_TIME),
                        desc = record.get(DESC),
                        content = record.get(CONTENT)
                    )
                }
        }
    }

    /**
     * 分页查询变量组的所有不同版本号
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param groupName 变量组名
     * @param page 页码
     * @param pageSize 每页大小
     * @return 版本号列表（按版本号降序）
     */
    fun listDistinctVersions(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        page: Int,
        pageSize: Int
    ): List<Int> {
        with(TResourcePublicVarGroupReleaseRecord.T_RESOURCE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            val offset = (page - 1) * pageSize
            return dslContext.selectDistinct(VERSION)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .orderBy(VERSION.desc())
                .offset(offset)
                .limit(pageSize)
                .fetch(VERSION)
        }
    }
}
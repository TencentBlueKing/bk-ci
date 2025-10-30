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

package com.tencent.devops.process.dao

import com.tencent.devops.common.pipeline.pojo.transfer.VarRefDetail
import com.tencent.devops.model.process.tables.TVarRefDetail
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class VarRefDetailDao {

    /**
     * 批量保存变量引用详情（支持更新）
     * 使用 onDuplicateKeyUpdate 处理已存在的记录
     * @param dslContext 数据库上下文
     * @param varRefDetails 变量引用详情列表
     */
    fun batchSave(
        dslContext: DSLContext,
        varRefDetails: List<VarRefDetail>
    ) {
        if (varRefDetails.isEmpty()) {
            return
        }

        with(TVarRefDetail.T_VAR_REF_DETAIL) {
            varRefDetails.forEach { detail ->
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    VAR_NAME,
                    RESOURCE_ID,
                    RESOURCE_TYPE,
                    RESOURCE_VERSION_NAME,
                    REFER_VERSION,
                    STAGE_ID,
                    CONTAINER_ID,
                    TASK_ID,
                    POSITION_PATH,
                    CREATOR,
                    MODIFIER,
                    CREATE_TIME,
                    UPDATE_TIME
                )
                .values(
                    detail.projectId,
                    detail.varName,
                    detail.resourceId,
                    detail.resourceType,
                    detail.resourceVersionName,
                    detail.referVersion,
                    detail.stageId,
                    detail.containerId,
                    detail.taskId,
                    detail.positionPath,
                    detail.creator,
                    detail.modifier,
                    detail.createTime,
                    detail.updateTime
                )
                .onDuplicateKeyUpdate()
                .set(POSITION_PATH, detail.positionPath)
                .set(MODIFIER, detail.modifier)
                .set(UPDATE_TIME, detail.updateTime)
                .execute()
            }
        }
    }

    /**
     * 根据资源ID删除变量引用详情
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param resourceId 资源ID
     * @param resourceType 资源类型
     * @param referVersion 引用版本（可选）
     */
    fun deleteByResourceId(
        dslContext: DSLContext,
        projectId: String,
        resourceId: String,
        resourceType: String,
        referVersion: Int? = null
    ) {
        with(TVarRefDetail.T_VAR_REF_DETAIL) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                RESOURCE_ID.eq(resourceId),
                RESOURCE_TYPE.eq(resourceType)
            )
            if (referVersion != null) {
                conditions.add(REFER_VERSION.eq(referVersion))
            }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    /**
     * 批量删除指定资源的变量引用详情
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param resourceIds 资源ID列表
     * @param resourceType 资源类型
     */
    fun batchDeleteByResourceIds(
        dslContext: DSLContext,
        projectId: String,
        resourceIds: List<String>,
        resourceType: String
    ) {
        if (resourceIds.isEmpty()) {
            return
        }
        with(TVarRefDetail.T_VAR_REF_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(RESOURCE_ID.`in`(resourceIds))
                .and(RESOURCE_TYPE.eq(resourceType))
                .execute()
        }
    }

    /**
     * 统计指定资源的变量引用数量
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param resourceId 资源ID
     * @param resourceType 资源类型
     * @param referVersion 引用版本（可选）
     * @return 变量引用数量
     */
    fun countByResourceId(
        dslContext: DSLContext,
        projectId: String,
        resourceId: String,
        resourceType: String,
        referVersion: Int? = null
    ): Int {
        with(TVarRefDetail.T_VAR_REF_DETAIL) {
            val conditions = mutableListOf(
                PROJECT_ID.eq(projectId),
                RESOURCE_ID.eq(resourceId),
                RESOURCE_TYPE.eq(resourceType)
            )
            if (referVersion != null) {
                conditions.add(REFER_VERSION.eq(referVersion))
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Int::class.java) ?: 0
        }
    }

    /**
     * 根据变量名列表删除不在列表中的变量引用
     * @param dslContext 数据库上下文
     * @param projectId 项目ID
     * @param resourceId 资源ID
     * @param resourceType 资源类型
     * @param referVersion 引用版本
     * @param varNames 要保留的变量名列表
     */
    fun deleteByVarNamesNotIn(
        dslContext: DSLContext,
        projectId: String,
        resourceId: String,
        resourceType: String,
        referVersion: Int,
        varNames: Set<String>
    ) {
        with(TVarRefDetail.T_VAR_REF_DETAIL) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(RESOURCE_ID.eq(resourceId))
                .and(RESOURCE_TYPE.eq(resourceType))
                .and(REFER_VERSION.eq(referVersion))
                .and(VAR_NAME.notIn(varNames))
                .execute()
        }
    }
}

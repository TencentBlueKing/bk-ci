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

package com.tencent.devops.process.engine.dao.template

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.process.tables.TTemplateInstanceItem
import com.tencent.devops.model.process.tables.records.TTemplateInstanceItemRecord
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Suppress("LongParameterList")
@Repository
class TemplateInstanceItemDao {

    fun createTemplateInstanceItem(
        dslContext: DSLContext,
        projectId: String,
        baseId: String,
        instances: List<TemplateInstanceUpdate>,
        status: String,
        userId: String
    ) {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            instances.map {
                val buildNo = it.buildNo
                val param = it.param
                dslContext.insertInto(
                    this,
                    ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    BUILD_NO_INFO,
                    STATUS,
                    PARAM,
                    BASE_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        projectId,
                        it.pipelineId,
                        it.pipelineName,
                        buildNo?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        status,
                        param?.let { self -> JsonUtil.toJson(self, formatted = false) },
                        baseId,
                        userId,
                        userId
                    )
                    .onDuplicateKeyUpdate()
                    .set(PIPELINE_NAME, it.pipelineName)
                    .set(BUILD_NO_INFO, buildNo?.let { self -> JsonUtil.toJson(self, formatted = false) })
                    .set(STATUS, status)
                    .set(PARAM, param?.let { self -> JsonUtil.toJson(self, formatted = false) })
                    .set(BASE_ID, baseId)
                    .set(CREATOR, userId)
                    .set(MODIFIER, userId)
                    .execute()
            }
        }
    }

    fun getTemplateInstanceItemList(
        dslContext: DSLContext,
        status: String,
        descFlag: Boolean,
        page: Int,
        pageSize: Int
    ): Result<TTemplateInstanceItemRecord>? {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            val baseStep = dslContext.selectFrom(this).where(STATUS.eq(status))
            if (descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun getTemplateInstanceItemListByBaseId(
        dslContext: DSLContext,
        projectId: String,
        baseId: String,
        descFlag: Boolean,
        page: Int,
        pageSize: Int
    ): Result<TTemplateInstanceItemRecord>? {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            val baseStep = dslContext.selectFrom(this).where(BASE_ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
            if (descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun getTemplateInstanceItemCountByBaseId(
        dslContext: DSLContext,
        projectId: String,
        baseId: String
    ): Long {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            return dslContext.selectCount().from(this)
                .where(BASE_ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getTemplateInstanceItemListByPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: Collection<String>
    ): Result<TTemplateInstanceItemRecord>? {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds).and(PROJECT_ID.eq(projectId))).fetch()
        }
    }

    fun deleteByBaseId(dslContext: DSLContext, projectId: String, baseId: String) {
        with(TTemplateInstanceItem.T_TEMPLATE_INSTANCE_ITEM) {
            dslContext.deleteFrom(this)
                .where(BASE_ID.eq(baseId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }
}

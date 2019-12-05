/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.dao.container

import com.tencent.devops.model.store.tables.TContainer
import com.tencent.devops.model.store.tables.records.TContainerRecord
import com.tencent.devops.store.pojo.container.ContainerRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import org.springframework.util.StringUtils
import java.time.LocalDateTime

@Repository
class ContainerDao {

    fun countByName(dslContext: DSLContext, name: String): Int {
        with(TContainer.T_CONTAINER) {
            return dslContext.selectCount().from(this).where(NAME.eq(name)).fetchOne(0, Int::class.java)
        }
    }

    fun savePipelineContainer(dslContext: DSLContext, id: String, containerRequest: ContainerRequest): Int {
        with(TContainer.T_CONTAINER) {
            return dslContext.insertInto(
                this,
                ID,
                NAME,
                TYPE,
                OS,
                REQUIRED,
                MAX_QUEUE_MINUTES,
                MAX_RUNNING_MINUTES,
                PROPS
            ).values(
                id,
                containerRequest.name,
                containerRequest.type,
                containerRequest.os,
                containerRequest.required,
                containerRequest.maxQueueMinutes,
                containerRequest.maxRunningMinutes,
                containerRequest.props
            )
                .execute()
        }
    }

    fun updatePipelineContainer(dslContext: DSLContext, id: String, containerRequest: ContainerRequest): Int {
        with(TContainer.T_CONTAINER) {
            return dslContext.update(this)
                .set(NAME, containerRequest.name)
                .set(TYPE, containerRequest.type)
                .set(OS, containerRequest.os)
                .set(REQUIRED, containerRequest.required)
                .set(MAX_QUEUE_MINUTES, containerRequest.maxQueueMinutes)
                .set(MAX_RUNNING_MINUTES, containerRequest.maxRunningMinutes)
                .set(PROPS, containerRequest.props)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getAllPipelineContainer(dslContext: DSLContext, type: String?, os: String?): Result<TContainerRecord>? {
        with(TContainer.T_CONTAINER) {
            val conditions = queryContainerCondition(this, type, os)
            return dslContext
                .selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    private fun queryContainerCondition(a: TContainer, type: String?, os: String?): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!StringUtils.isEmpty(type)) conditions.add(a.TYPE.eq(type))
        if (!StringUtils.isEmpty(os)) conditions.add(a.OS.eq(os))
        return conditions
    }

    fun getPipelineContainer(dslContext: DSLContext, id: String): TContainerRecord? {
        with(TContainer.T_CONTAINER) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun deletePipelineContainer(dslContext: DSLContext, id: String): Int {
        with(TContainer.T_CONTAINER) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }
}
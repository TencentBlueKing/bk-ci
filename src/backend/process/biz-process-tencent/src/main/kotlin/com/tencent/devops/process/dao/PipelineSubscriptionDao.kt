/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TPipelineSubscription
import com.tencent.devops.model.process.tables.records.TPipelineSubscriptionRecord
import com.tencent.devops.process.pojo.SubscriptionType
import com.tencent.devops.process.pojo.pipeline.PipelineSubscription
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineSubscriptionDao {

    fun insert(dslContext: DSLContext, pipelineId: String, username: String, subscriptionTypes: List<PipelineSubscriptionType>, type: SubscriptionType) {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            dslContext.insertInto(this,
                    PIPELINE_ID,
                    USERNAME,
                    SUBSCRIPTION_TYPE,
                    TYPE)
                    .values(pipelineId,
                            username,
                            subscriptionTypes.joinToString(","),
                            type.type)
                    .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Int,
        subscriptionTypes: List<PipelineSubscriptionType>,
        type: SubscriptionType
    ) =
            with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
                dslContext.update(this)
                        .set(SUBSCRIPTION_TYPE, subscriptionTypes.joinToString(","))
                        .set(TYPE, type.type)
                        .where(ID.eq(id))
                        .execute()
    }

    fun delete(dslContext: DSLContext, pipelineId: String, username: String): Boolean {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            return dslContext.deleteFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(USERNAME.eq(username))
                    .execute() == 1
        }
    }

    fun get(dslContext: DSLContext, pipelineId: String, username: String): TPipelineSubscriptionRecord? {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .and(USERNAME.eq(username))
                    .fetchOne()
        }
    }

    fun list(dslContext: DSLContext, pipelineId: String): List<TPipelineSubscriptionRecord> {
        with(TPipelineSubscription.T_PIPELINE_SUBSCRIPTION) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.eq(pipelineId))
                    .fetch()
        }
    }

    fun convert(record: TPipelineSubscriptionRecord): PipelineSubscription {
        with(record) {
            return PipelineSubscription(pipelineId, username, convertSubscriptionTypes(subscriptionType),
                    if (type == null) {
                        SubscriptionType.ALL
                    } else {
                        SubscriptionType.toType(type)
                    }
            )
        }
    }

    private fun convertSubscriptionTypes(types: String?): List<PipelineSubscriptionType> {
        return if (types.isNullOrEmpty()) {
            listOf()
        } else {
            val tmp = types!!.split(",")
            tmp.map {
                PipelineSubscriptionType.valueOf(it)
            }.toList()
        }
    }
}
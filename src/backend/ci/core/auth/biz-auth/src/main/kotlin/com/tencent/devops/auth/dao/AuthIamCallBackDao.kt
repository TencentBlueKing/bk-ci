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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.IamCallBackInfo
import com.tencent.devops.model.auth.tables.TAuthIamCallback
import com.tencent.devops.model.auth.tables.records.TAuthIamCallbackRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class AuthIamCallBackDao {

    fun create(dslContext: DSLContext, info: IamCallBackInfo): Int {
        with(TAuthIamCallback.T_AUTH_IAM_CALLBACK) {
            return dslContext.insertInto(
                this,
                GATEWAY,
                RESOURCE,
                PATH,
                SYSTEM
            ).values(info.gateway, info.resource, info.path, info.system).execute()
        }
    }

    fun update(dslContext: DSLContext, info: IamCallBackInfo, id: Int): Int {
        with(TAuthIamCallback.T_AUTH_IAM_CALLBACK) {
            return dslContext.update(this)
                .set(GATEWAY, info.gateway)
                .set(RESOURCE, info.resource)
                .set(PATH, info.path)
                .set(SYSTEM, info.system)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, resource: String): TAuthIamCallbackRecord? {
        with(TAuthIamCallback.T_AUTH_IAM_CALLBACK) {
            return dslContext.selectFrom(this).where(RESOURCE.eq(resource).and(DELETE_FLAG.eq(false))).fetchAny()
        }
    }

    fun list(dslContext: DSLContext): Result<TAuthIamCallbackRecord>? {
        with(TAuthIamCallback.T_AUTH_IAM_CALLBACK) {
            return dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).fetch()
        }
    }

    fun refreshGateway(dslContext: DSLContext, oldToNewMap: Map<String, String>) {
        with(TAuthIamCallback.T_AUTH_IAM_CALLBACK) {
            dslContext.transaction { configuration ->
                val updates = mutableListOf<UpdateSetMoreStep<TAuthIamCallbackRecord>>()
                val transactionContext = DSL.using(configuration)
                transactionContext.selectFrom(this).fetch().forEach { record ->
                    oldToNewMap.forEach nextOne@{ (old, new) ->
                        if (!record.gateway.contains(old)) return@nextOne
                        val update = transactionContext.update(this)
                            .set(GATEWAY, record.gateway.replace(old, new))
                        update.where(ID.eq(record.id))
                        updates.add(update)
                    }
                }
                transactionContext.batch(updates).execute()
            }
        }
    }
}

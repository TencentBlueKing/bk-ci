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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProjectCallback
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectCallbackDao {
    fun get(dslContext: DSLContext, event: String) = with(TProjectCallback.T_PROJECT_CALLBACK) {
        dslContext.selectFrom(this)
            .where(EVENT.eq(event))
            .fetch()
    }

    fun create(
        dslContext: DSLContext,
        event: String,
        url: String,
        secretType: String,
        secretParam: String
    ) {
        with(TProjectCallback.T_PROJECT_CALLBACK) {
            dslContext.insertInto(
                this,
                EVENT,
                CALLBACK_URL,
                SECRET_TYPE,
                SECRET_PARAM
            ).values(
                event,
                url,
                secretType,
                secretParam
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        event: String,
        url: String
    ): Int {
        return with(TProjectCallback.T_PROJECT_CALLBACK) {
            dslContext.deleteFrom(this).where(
                EVENT.eq(event)
                    .and(CALLBACK_URL.eq(url))
            ).execute()
        }
    }
}
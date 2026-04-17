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

package com.tencent.devops.ai.dao

import com.tencent.devops.model.ai.tables.TAiWelcomeGuide
import com.tencent.devops.model.ai.tables.records.TAiWelcomeGuideRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

/** 欢迎引导 DAO，对应 T_AI_WELCOME_GUIDE 表。 */
@Repository
class WelcomeGuideDao {

    fun listEnabledByType(
        dslContext: DSLContext,
        type: String
    ): Result<TAiWelcomeGuideRecord> {
        with(TAiWelcomeGuide.T_AI_WELCOME_GUIDE) {
            return dslContext.selectFrom(this)
                .where(ENABLED.eq(true))
                .and(TYPE.eq(type))
                .orderBy(SORT_ORDER.asc())
                .fetch()
        }
    }

    fun listEnabledByParentId(
        dslContext: DSLContext,
        parentId: String
    ): Result<TAiWelcomeGuideRecord> {
        with(TAiWelcomeGuide.T_AI_WELCOME_GUIDE) {
            return dslContext.selectFrom(this)
                .where(ENABLED.eq(true))
                .and(PARENT_ID.eq(parentId))
                .orderBy(SORT_ORDER.asc())
                .fetch()
        }
    }

    fun listAllEnabled(dslContext: DSLContext): Result<TAiWelcomeGuideRecord> {
        with(TAiWelcomeGuide.T_AI_WELCOME_GUIDE) {
            return dslContext.selectFrom(this)
                .where(ENABLED.eq(true))
                .orderBy(SORT_ORDER.asc())
                .fetch()
        }
    }
}

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

import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.project.tables.TI18nMessage
import com.tencent.devops.model.project.tables.records.TI18nMessageRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class I18nMessageDao {

    fun batchAdd(dslContext: DSLContext, userId: String, i18nMessages: List<I18nMessage>) {
        with(TI18nMessage.T_I18N_MESSAGE) {
            dslContext.batch(i18nMessages.map { i18nMessage ->
                dslContext.insertInto(
                    this,
                    ID,
                    MODULE_CODE,
                    LANGUAGE,
                    KEY,
                    VALUE,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        i18nMessage.moduleCode,
                        i18nMessage.language,
                        i18nMessage.key,
                        i18nMessage.value,
                        userId,
                        userId
                    ).onDuplicateKeyUpdate()
                    .set(VALUE, i18nMessage.value)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(MODIFIER, userId)
            }).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        moduleCode: String,
        key: String,
        language: String? = null
    ) {
        with(TI18nMessage.T_I18N_MESSAGE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(MODULE_CODE.eq(moduleCode))
            conditions.add(KEY.like("$key%"))
            language?.let { conditions.add(LANGUAGE.eq(language)) }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        moduleCode: String,
        key: String,
        language: String
    ): TI18nMessageRecord? {
        with(TI18nMessage.T_I18N_MESSAGE) {
            return dslContext.selectFrom(this)
                .where(MODULE_CODE.eq(moduleCode))
                .and(KEY.eq(key))
                .and(LANGUAGE.eq(language))
                .fetchOne()
        }
    }

    fun list(
        dslContext: DSLContext,
        moduleCodes: Set<String>,
        language: String,
        keys: List<String>? = null,
        keyPrefix: String? = null
    ): Result<TI18nMessageRecord>? {
        with(TI18nMessage.T_I18N_MESSAGE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(MODULE_CODE.`in`(moduleCodes))
            conditions.add(LANGUAGE.eq(language))
            keys?.let { conditions.add(KEY.`in`(keys)) }
            keyPrefix?.let { conditions.add(KEY.like("$keyPrefix%")) }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }
}

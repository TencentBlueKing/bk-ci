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

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.model.project.tables.TI18nMessage
import com.tencent.devops.model.project.tables.records.TI18nMessageRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class I18nMessageDao {

    fun batchAdd(dslContext: DSLContext, userId: String, i18nMessages: List<I18nMessage>) {
        with(TI18nMessage.T_I18N_MESSAGE) {
            dslContext.batch(i18nMessages.map { i18nMessage ->
                dslContext.insertInto(
                    this,
                    MODULE_CODE,
                    LOCALE,
                    KEY,
                    VALUE,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        i18nMessage.moduleCode.name,
                        i18nMessage.locale,
                        i18nMessage.key,
                        i18nMessage.value,
                        userId,
                        userId
                    ).onDuplicateKeyUpdate()
                    .set(VALUE, i18nMessage.locale)
            }).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        moduleCode: SystemModuleEnum,
        key: String,
        locale: String? = null
    ) {
        with(TI18nMessage.T_I18N_MESSAGE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(MODULE_CODE.eq(moduleCode.name))
            conditions.add(KEY.like("$key%"))
            locale?.let { conditions.add(LOCALE.eq(locale)) }
            dslContext.deleteFrom(this)
                .where(conditions)
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        moduleCode: SystemModuleEnum,
        key: String,
        locale: String
    ): TI18nMessageRecord? {
        with(TI18nMessage.T_I18N_MESSAGE) {
            return dslContext.selectFrom(this)
                .where(MODULE_CODE.eq(moduleCode.name))
                .and(KEY.eq(key))
                .and(LOCALE.eq(locale))
                .fetchOne()
        }
    }

    fun list(
        dslContext: DSLContext,
        moduleCode: SystemModuleEnum,
        keys: List<String>,
        locale: String
    ): Result<TI18nMessageRecord>? {
        with(TI18nMessage.T_I18N_MESSAGE) {
            return dslContext.selectFrom(this)
                .where(MODULE_CODE.eq(moduleCode.name))
                .and(KEY.`in`(keys))
                .and(LOCALE.eq(locale))
                .fetch()
        }
    }

    fun getI18nMessageKeys(
        dslContext: DSLContext,
        moduleCode: SystemModuleEnum,
        key: String,
        locale: String? = null
    ): List<String> {
        with(TI18nMessage.T_I18N_MESSAGE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(MODULE_CODE.eq(moduleCode.name))
            conditions.add(KEY.like("$key%"))
            locale?.let { conditions.add(LOCALE.eq(locale)) }
            return dslContext.select(KEY).from(this)
                .where(conditions)
                .fetchInto(String::class.java)
        }
    }
}

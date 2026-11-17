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

package com.tencent.devops.project.dao

import com.tencent.devops.project.pojo.convention.ConventionAgreementItem
import com.tencent.devops.project.service.ConventionSupport
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ConventionDao {

    fun getBody(dslContext: DSLContext, lang: String): String? {
        return dslContext.select(bodyContentField)
            .from(bodyTable)
            .where(langField.eq(lang))
            .limit(1)
            .fetchOne(bodyContentField)
    }

    fun saveBody(dslContext: DSLContext, lang: String, content: String, updater: String) {
        val now = LocalDateTime.now()
        dslContext.insertInto(bodyTable)
            .set(langField, lang)
            .set(bodyContentField, content)
            .set(updaterField, updater)
            .set(bodyUpdateTimeField, now)
            .onDuplicateKeyUpdate()
            .set(bodyContentField, content)
            .set(updaterField, updater)
            .set(bodyUpdateTimeField, now)
            .execute()
    }

    fun existsAgreement(dslContext: DSLContext, userId: String): Boolean {
        return dslContext.select(agreementIdField)
            .from(agreementTable)
            .where(userIdField.eq(userId))
            .limit(1)
            .fetchOne() != null
    }

    fun insertAgreement(dslContext: DSLContext, userId: String, agreeTime: LocalDateTime) {
        dslContext.insertInto(agreementTable)
            .set(userIdField, userId)
            .set(agreeTimeField, agreeTime)
            .execute()
    }

    fun countAgreements(dslContext: DSLContext, userKeyword: String?): Long {
        return dslContext.selectCount()
            .from(agreementTable)
            .where(agreementCondition(userKeyword))
            .fetchOne(0, Int::class.java)
            ?.toLong() ?: 0L
    }

    fun listAgreements(
        dslContext: DSLContext,
        userKeyword: String?,
        offset: Int,
        limit: Int
    ): List<ConventionAgreementItem> {
        return dslContext.select(userIdField, agreeTimeField)
            .from(agreementTable)
            .where(agreementCondition(userKeyword))
            .orderBy(agreeTimeField.desc(), agreementIdField.desc())
            .limit(limit)
            .offset(offset)
            .fetch {
                ConventionAgreementItem(
                    userId = it.get(userIdField),
                    agreeTime = ConventionSupport.formatMinute(it.get(agreeTimeField)).orEmpty()
                )
            }
    }

    private fun agreementCondition(userKeyword: String?): Condition {
        var condition: Condition = agreementIdField.gt(0L)
        if (!userKeyword.isNullOrBlank()) {
            condition = condition.and(userIdField.contains(userKeyword))
        }
        return condition
    }

    companion object {
        private val bodyTable = DSL.table(DSL.name("T_CONVENTION_BODY"))
        private val agreementTable = DSL.table(DSL.name("T_CONVENTION_AGREEMENT"))

        private val langField: Field<String> =
            DSL.field(DSL.name("T_CONVENTION_BODY", "LANG"), SQLDataType.VARCHAR)
        private val bodyContentField: Field<String> =
            DSL.field(DSL.name("T_CONVENTION_BODY", "CONTENT"), SQLDataType.CLOB)
        private val updaterField: Field<String> =
            DSL.field(DSL.name("T_CONVENTION_BODY", "UPDATER"), SQLDataType.VARCHAR)
        private val bodyUpdateTimeField: Field<LocalDateTime> =
            DSL.field(DSL.name("T_CONVENTION_BODY", "UPDATE_TIME"), SQLDataType.LOCALDATETIME)

        private val agreementIdField: Field<Long> =
            DSL.field(DSL.name("T_CONVENTION_AGREEMENT", "ID"), SQLDataType.BIGINT)
        private val userIdField: Field<String> =
            DSL.field(DSL.name("T_CONVENTION_AGREEMENT", "USER_ID"), SQLDataType.VARCHAR)
        private val agreeTimeField: Field<LocalDateTime> =
            DSL.field(DSL.name("T_CONVENTION_AGREEMENT", "AGREE_TIME"), SQLDataType.LOCALDATETIME)
    }
}

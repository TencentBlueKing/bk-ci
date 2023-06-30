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

package com.tencent.devops.common.db.utils

import com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException
import java.math.BigDecimal
import java.sql.Timestamp
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.exception.DataAccessException
import org.jooq.impl.DSL

object JooqUtils {

    const val JooqDeadLockMessage = "Deadlock found when trying to get lock; try restarting transaction"

    fun <T> retryWhenDeadLock(action: () -> T): T {
        return try {
            action()
        } catch (dae: DataAccessException) {
            if (dae.isDeadLock()) action() else throw dae
        }
    }

    private fun DataAccessException.isDeadLock(): Boolean =
        message?.contains(JooqDeadLockMessage) == true || this.cause is MySQLTransactionRollbackException

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>, t2: Field<Timestamp>): Field<Long> {
        return DSL.field(
            "timestampdiff({0}, {1}, {2})",
            Long::class.java, DSL.keyword(part.toSQL()), t1, t2
        )
    }

    fun strPosition(data: Field<String>, param: String): Field<Int> {
        return DSL.field(
            "POSITION({0} IN {1})",
            Int::class.java,
            data,
            param
        )
    }

    fun subStr(str: Field<String>, delim: String, count: Int): Field<String> {
        return DSL.field(
            "SUBSTRING_INDEX({0}, {1}, {2})",
            String::class.java, str, delim, count
        )
    }

    fun jsonExtract(t1: Field<String>, t2: String, lower: Boolean = false): Field<String> {
        return if (lower) {
            DSL.field(
                "LOWER(JSON_EXTRACT({0}, {1}))",
                String::class.java, t1, t2
            )
        } else {
            DSL.field(
                "JSON_EXTRACT({0}, {1})",
                String::class.java, t1, t2
            )
        }
    }

    fun <T> sum(data: Field<T>): Field<BigDecimal> {
        return DSL.field(
            "sum(${data.name})",
            BigDecimal::class.java
        )
    }

    fun <T> sum(data1: Field<T>, data2: Field<T>, operation: String): Field<BigDecimal> {
        return DSL.field(
            "sum(${data1.name}$operation${data2.name})",
            BigDecimal::class.java
        )
    }

    fun <T> productSum(t1: Field<T>, t2: Field<T>): Field<BigDecimal> {
        return DSL.field(
            "sum(${t1.name} * ${t2.name})",
            BigDecimal::class.java
        )
    }

    fun <T> count(data: Field<T>): Field<Int> {
        return DSL.field(
            "count(${data.name})",
            Int::class.java
        )
    }
}

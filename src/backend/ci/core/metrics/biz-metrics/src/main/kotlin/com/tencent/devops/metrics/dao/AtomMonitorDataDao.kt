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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.db.utils.JooqUtils.sum
import com.tencent.devops.metrics.pojo.`do`.AtomMonitorDataDO
import com.tencent.devops.model.metrics.tables.TAtomMonitorDataDaily
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AtomMonitorDataDao {

    fun getAtomMonitorDatas(
        dslContext: DSLContext,
        atomCode: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        errorTypes: List<Int?>? = null
    ): List<AtomMonitorDataDO>? {
        with(TAtomMonitorDataDaily.T_ATOM_MONITOR_DATA_DAILY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ATOM_CODE.eq(atomCode))
            conditions.add(STATISTICS_TIME.between(startTime, endTime))
            if (!errorTypes.isNullOrEmpty()) {
                conditions.add(ERROR_TYPE.`in`(errorTypes))
            }
            return dslContext.select(
                ATOM_CODE.`as`(AtomMonitorDataDO::atomCode.name),
                ERROR_TYPE.`as`(AtomMonitorDataDO::errorType.name),
                sum<Long>(EXECUTE_COUNT).`as`(AtomMonitorDataDO::totalExecuteCount.name)
            )
                .from(this)
                .where(conditions)
                .groupBy(ATOM_CODE, ERROR_TYPE)
                .fetchInto(AtomMonitorDataDO::class.java)
        }
    }
}

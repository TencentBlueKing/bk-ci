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

import com.tencent.devops.model.metrics.tables.TErrorCodeInfo
import com.tencent.devops.metrics.pojo.`do`.ErrorCodeInfoDO
import com.tencent.devops.metrics.pojo.qo.QueryErrorCodeInfoQO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ErrorCodeInfoDao {

    fun getErrorCodeInfo(
        dslContext: DSLContext,
        queryCondition: QueryErrorCodeInfoQO
    ): List<ErrorCodeInfoDO> {
        with(TErrorCodeInfo.T_ERROR_CODE_INFO) {
            val step = dslContext.select(ERROR_TYPE, ERROR_CODE, ERROR_MSG).from(this)
            val conditionStep = if (!queryCondition.errorTypes.isNullOrEmpty()) {
                step.where(ERROR_TYPE.`in`(queryCondition.errorTypes))
                    .groupBy(ERROR_CODE)
            } else {
                step.groupBy(ERROR_CODE)
            }
            return conditionStep.limit((queryCondition.page - 1) * queryCondition.pageSize, queryCondition.pageSize)
                .fetchInto(ErrorCodeInfoDO::class.java)
        }
    }

    fun getErrorCodeInfoCount(
        dslContext: DSLContext,
        queryCondition: QueryErrorCodeInfoQO
    ): Long {
        with(TErrorCodeInfo.T_ERROR_CODE_INFO) {
            val step = dslContext.select(ERROR_CODE).from(this)
            val conditionStep =
                if (!queryCondition.errorTypes.isNullOrEmpty()) {
                step.where(ERROR_TYPE.`in`(queryCondition.errorTypes))
                    .groupBy(ERROR_CODE)
            } else {
                step.groupBy(ERROR_CODE)
            }
            return conditionStep.execute().toLong()
        }
    }
}
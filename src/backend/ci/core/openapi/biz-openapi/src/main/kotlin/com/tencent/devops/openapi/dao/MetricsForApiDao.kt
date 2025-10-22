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

package com.tencent.devops.openapi.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.openapi.tables.TOpenapiMetricsForApi
import com.tencent.devops.openapi.pojo.MetricsApiData
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class MetricsForApiDao {

    companion object {
        const val batchGetLimit = Short.MAX_VALUE
    }

    @Suppress("ComplexMethod")
    fun createOrUpdate(
        dslContext: DSLContext,
        metricsApis: List<MetricsApiData>,
        perHour: Boolean,
        perDay: Boolean
    ): Int {
        return with(TOpenapiMetricsForApi.T_OPENAPI_METRICS_FOR_API) {
            dslContext.batch(
                metricsApis.mapNotNull { metricsApi ->
                    dslContext.insertInto(
                        this,
                        API,
                        KEY,
                        SECOND_LEVEL_CONCURRENCY,
                        PEAK_CONCURRENCY,
                        CALL_5M,
                        CALL_1H,
                        CALL_24H,
                        CALL_7D
                    ).values(
                        metricsApi.api,
                        metricsApi.key,
                        metricsApi.secondLevelConcurrency ?: 0,
                        metricsApi.peakConcurrency ?: 0,
                        metricsApi.call5m ?: 0,
                        metricsApi.call1h ?: 0,
                        metricsApi.call24h ?: 0,
                        metricsApi.call7d ?: 0
                    ).onDuplicateKeyUpdate()
                        .let { u ->
                            metricsApi.secondLevelConcurrency?.let { i ->
                                u.set(SECOND_LEVEL_CONCURRENCY, i)
                            } ?: u.set(SECOND_LEVEL_CONCURRENCY, 0)
                        }
                        .let { u ->
                            metricsApi.peakConcurrency?.let { i ->
                                u.set(PEAK_CONCURRENCY, i)
                            } ?: u.set(PEAK_CONCURRENCY, 0)
                        }
                        .let { u ->
                            metricsApi.call5m?.let { i -> u.set(CALL_5M, i) } ?: u.set(CALL_5M, 0)
                        }
                        .let { u ->
                            metricsApi.call1h?.let { i -> u.set(CALL_1H, i) } ?: if (perHour) u.set(CALL_1H, 0) else u
                        }
                        .let { u ->
                            metricsApi.call24h?.let { i -> u.set(CALL_24H, i) } ?: if (perDay) u.set(CALL_24H, 0) else u
                        }
                        .let { u ->
                            metricsApi.call7d?.let { i -> u.set(CALL_7D, i) } ?: if (perDay) u.set(CALL_7D, 0) else u
                        }
                }
            ).execute().sum()
        }
    }

    fun batchGet(
        dslContext: DSLContext
    ): List<MetricsApiData> {
        return with(TOpenapiMetricsForApi.T_OPENAPI_METRICS_FOR_API) {
            dslContext.select(API, KEY, PEAK_CONCURRENCY).from(this).limit(batchGetLimit).skipCheck().fetch {
                MetricsApiData(
                    api = it.value1(),
                    key = it.value2(),
                    peakConcurrency = it.value3()
                )
            }
        }
    }
}

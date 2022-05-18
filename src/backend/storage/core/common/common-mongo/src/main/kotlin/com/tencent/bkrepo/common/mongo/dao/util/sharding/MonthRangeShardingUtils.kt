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

package com.tencent.bkrepo.common.mongo.dao.util.sharding

import org.bson.Document
import java.time.LocalDateTime

object MonthRangeShardingUtils : ShardingUtils {

    override fun shardingCountFor(i: Int): Int {
        return -1
    }

    override fun shardingSequenceFor(value: Any, shardingCount: Int): Int {
        require(value is LocalDateTime)
        return calculateSequence(value)
    }

    override fun shardingSequencesFor(value: Any, shardingCount: Int): Set<Int> {
        require(value is Document && value.size == 2)
        val startValue = (value["\$gte"] ?: value["\$gt"]) as LocalDateTime
        val endValue = (value["\$lte"] ?: value["\$lt"]) as LocalDateTime
        var yearMonth = startValue
        val sequences = mutableSetOf<Int>()
        do {
            sequences.add(calculateSequence(yearMonth))
            yearMonth = yearMonth.plusMonths(1L)
        } while (calculateSequence(yearMonth) <= calculateSequence(endValue))
        return sequences
    }

    private fun calculateSequence(value: LocalDateTime) = value.year * 100 + value.monthValue
}

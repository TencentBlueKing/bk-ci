/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.mongo.dao.util.sharding

import org.slf4j.LoggerFactory

/**
 * Sharding 工具类
 */
object HashShardingUtils : ShardingUtils {

    private const val MAXIMUM_CAPACITY = 1 shl 10
    private val logger = LoggerFactory.getLogger(HashShardingUtils::class.java)

    /**
     * 计算[i]对应的合适的sharding数量，规则是得到大于等于[i]的2的次幂，最小值为1，最大值为[MAXIMUM_CAPACITY]
     */
    override fun shardingCountFor(i: Int): Int {
        require(i >= 0) { "Illegal initial sharding count : $i" }
        var result = if (i > MAXIMUM_CAPACITY) MAXIMUM_CAPACITY else i
        result = tableSizeFor(result)
        if (i != result) {
            logger.warn("Bad initial sharding count: [$i], converted to: [$result]")
        }
        return result
    }

    /**
     * 计算[value]对应的sharding sequence
     *
     * [shardingCount]表示分表数量，计算出的结果范围为[0, shardingCount)
     */
    override fun shardingSequenceFor(value: Any, shardingCount: Int): Int {
        val hashCode = value.hashCode()
        return hashCode and shardingCount - 1
    }

    override fun shardingSequencesFor(value: Any, shardingCount: Int): Set<Int> {
        throw UnsupportedOperationException()
    }

    private fun tableSizeFor(cap: Int): Int {
        // 减一的目的在于如果cap本身就是2的次幂，保证结果是原值，不减一的话，结果就成了cap * 2
        var n = cap - 1
        // 从最高位的1往低位复制
        n = n or n.ushr(1)
        n = n or n.ushr(2)
        n = n or n.ushr(4)
        n = n or n.ushr(8)
        n = n or n.ushr(16)
        // 到这里，从最高位的1到第0位都是1了，再加上1就是2的次幂
        return if (n < 0) 1 else if (n >= MAXIMUM_CAPACITY) MAXIMUM_CAPACITY else n + 1
    }
}

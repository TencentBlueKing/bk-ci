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

package com.tencent.bkrepo.common.mongo.dao.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ShardingUtilsTest {

    @Test
    fun testShardingCount() {
        assertThrows<IllegalArgumentException> { ShardingUtils.shardingCountFor(-1) }
        Assertions.assertEquals(1, ShardingUtils.shardingCountFor(0))
        Assertions.assertEquals(1, ShardingUtils.shardingCountFor(1))
        Assertions.assertEquals(2, ShardingUtils.shardingCountFor(2))
        Assertions.assertEquals(4, ShardingUtils.shardingCountFor(3))
        Assertions.assertEquals(256, ShardingUtils.shardingCountFor(255))
        Assertions.assertEquals(256, ShardingUtils.shardingCountFor(256))
        Assertions.assertEquals(512, ShardingUtils.shardingCountFor(257))
        Assertions.assertEquals(1024, ShardingUtils.shardingCountFor(2000))
    }

    @Test
    fun testShardingSequence() {
        Assertions.assertEquals(0, ShardingUtils.shardingSequenceFor(0, 256))
        Assertions.assertEquals(255, ShardingUtils.shardingSequenceFor(255, 256))
        Assertions.assertEquals(0, ShardingUtils.shardingSequenceFor(256, 256))

        Assertions.assertEquals(0, ShardingUtils.shardingSequenceFor(0, 1))
        Assertions.assertEquals(0, ShardingUtils.shardingSequenceFor(1, 1))
        Assertions.assertEquals(0, ShardingUtils.shardingSequenceFor(2, 1))
    }
}

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

package com.tencent.devops.common.pipeline.dialect

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PipelineDialectUtilTest {

    @Test
    fun testGetDialect() {
        val actual = PipelineDialectUtil.getPipelineDialect(null, null, null)
        Assertions.assertEquals(PipelineDialectType.CLASSIC.dialect, actual)

        val actual2 = PipelineDialectUtil.getPipelineDialect(
            inheritedDialect = null,
            projectDialect = PipelineDialectType.CLASSIC.name,
            pipelineDialect = null
        )
        Assertions.assertEquals(PipelineDialectType.CLASSIC.dialect, actual2)

        val actual3 = PipelineDialectUtil.getPipelineDialect(
            inheritedDialect = true,
            projectDialect = null,
            pipelineDialect = null
        )
        Assertions.assertEquals(PipelineDialectType.CLASSIC.dialect, actual3)

        val actual4 = PipelineDialectUtil.getPipelineDialect(
            inheritedDialect = true,
            projectDialect = null,
            pipelineDialect = PipelineDialectType.CONSTRAINED.name
        )
        Assertions.assertEquals(PipelineDialectType.CLASSIC.dialect, actual4)

        val actual5 = PipelineDialectUtil.getPipelineDialect(
            inheritedDialect = false,
            projectDialect = null,
            pipelineDialect = PipelineDialectType.CONSTRAINED.name
        )
        Assertions.assertEquals(PipelineDialectType.CONSTRAINED.dialect, actual5)

        val actual6 = PipelineDialectUtil.getPipelineDialect(
            inheritedDialect = true,
            projectDialect = PipelineDialectType.CLASSIC.name,
            pipelineDialect = PipelineDialectType.CONSTRAINED.name
        )
        Assertions.assertEquals(PipelineDialectType.CLASSIC.dialect, actual6)

        val actual7 = PipelineDialectUtil.getPipelineDialect(
            inheritedDialect = false,
            projectDialect = PipelineDialectType.CLASSIC.name,
            pipelineDialect = PipelineDialectType.CONSTRAINED.name
        )
        Assertions.assertEquals(PipelineDialectType.CONSTRAINED.dialect, actual7)
    }
}

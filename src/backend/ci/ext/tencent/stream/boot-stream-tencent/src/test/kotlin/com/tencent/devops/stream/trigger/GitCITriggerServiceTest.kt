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

package com.tencent.devops.stream.trigger

import com.nhaarman.mockito_kotlin.mock
import com.tencent.devops.stream.trigger.pojo.CheckType
import org.junit.Test

class GitCITriggerServiceTest {

    private val gitCITriggerService = GitCITriggerService(
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        mock()
    )

    /**
     * 校验
     * 源有，目标无，变更有
     * 源有，目标无，变更无
     * 源有，目标有，变更有
     * 源有，目标有，变更无
     */
    @Test
    fun checkMrYamlPathList() {
        val sources = setOf("1", "2", "3", "4")
        val target = setOf("1", "4")
        val changeSet = setOf("1")
        val result = gitCITriggerService.checkMrYamlPathList(sources, target, changeSet)
        val compare = mapOf(
            "1" to CheckType.NEED_CHECK,
            "2" to CheckType.NO_TRIGGER,
            "3" to CheckType.NO_TRIGGER,
            "4" to CheckType.NO_NEED_CHECK
        )
        assert(result == compare)
    }

    /**
     * 校验
     * 源无，目标有，变更有
     * 源无，目标有，变更无
     * 源无，目标无，变更有
     * 源无，目标无，变更无
     */
    @Test
    fun checkMrYamlPathList2() {
        val sources = setOf("3")
        val target = setOf("1", "2")
        val changeSet = setOf("1", "5")
        val result = gitCITriggerService.checkMrYamlPathList(sources, target, changeSet)
        val compare = mapOf("2" to CheckType.NO_NEED_CHECK, "3" to CheckType.NO_TRIGGER)
        assert(result == compare)
    }
}

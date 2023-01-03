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

package com.tencent.devops.process.engine.service.rule

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.engine.dao.PipelineRuleDao
import io.mockk.mockk
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PipelineRuleServiceTest {

    private val dslContext: DSLContext = mockk()
    private val pipelineRuleDao: PipelineRuleDao = mockk()
    private val redisOperation: RedisOperation = RedisOperation(mockk())

    private val pipelineRuleService = PipelineRuleService(
        dslContext = dslContext,
        pipelineRuleDao = pipelineRuleDao,
        redisOperation = redisOperation
    )

    @Test
    fun generateReplaceRuleStr() {
        val ruleStr = "\${{YEAR}}_\${{DAY_OF_MONTH}}-\${{FORMAT_DATE:\"yyyy-MM-dd HH:mm:ss\"}}"
        val validRuleValueMap = mutableMapOf<String, String>()
        validRuleValueMap["YEAR"] = "2020"
        validRuleValueMap["DAY_OF_MONTH"] = "28"
        validRuleValueMap["FORMAT_DATE:\"yyyy-MM-dd HH:mm:ss\""] = "2021-04-28 15:28:00"
        val replaceRuleStr = pipelineRuleService.generateReplaceRuleStr(ruleStr, validRuleValueMap)
        Assertions.assertEquals(replaceRuleStr, "2020_28-2021-04-28 15:28:00")
    }

    @Test
    fun getRuleNameList() {
        val ruleStr = "\${{YEAR}}_\${{DAY_OF_MONTH}}-\${{FORMAT_DATE:\"yyyy-MM-dd HH:mm:ss\"}}}"
        val ruleValue = JsonUtil.toJson(pipelineRuleService.getRuleNameList(ruleStr))
        Assertions.assertEquals(
            ruleValue,
            "[ \"YEAR\", \"DAY_OF_MONTH\", \"FORMAT_DATE:\\\"yyyy-MM-dd HH:mm:ss\\\"\" ]"
        )
    }
}

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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.pipeline.enums.BuildTaskStatus
import com.tencent.devops.process.pojo.BuildTask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BuildVarOverflowExprSupportTest {

    private val variables = mapOf(
        "name" to "world",
        "path" to "/data/\$x"
    )

    // 引擎侧替换前的原始参数：Map 类型参数（插件 data / customEnv 等）与字符串参数
    private val rawParams: Map<String, Any> = mapOf(
        "script" to "echo \${name} \${path} \${{ name }}",
        "json" to "{\"a\": \"\${name}\"}",
        "data" to mapOf(
            "input" to mapOf("msg" to "hi \${name}", "list" to listOf("\${path}", 1, true)),
            "output" to emptyMap<String, Any>()
        ),
        "customEnv" to listOf(mapOf("key" to "K", "value" to "\${name}"))
    )

    @Test
    fun replaceOnWorkerEqualsEngine() {
        // 引擎侧的参数由 DB JSON 反序列化而来（可变集合），ObjectReplaceEnvVarUtil 会原地修改
        val engineParams = JsonUtil.toMutableMap(JsonUtil.toJson(rawParams, false))
        val engineReplaced = engineParams.mapValues { (_, v) ->
            JsonUtil.toJson(ObjectReplaceEnvVarUtil.replaceEnvVar(v, variables), formatted = false)
        }
        val workerReplaced = BuildVarOverflowExprSupport.replaceClassicTaskParams(
            buildTask(replaceVarOnWorker = true, params = rawParams.mapValues { JsonUtil.toJson(it.value, false) })
        ).params
        Assertions.assertEquals(engineReplaced, workerReplaced)
    }

    @Test
    fun skipWhenEngineAlreadyReplaced() {
        val params = mapOf("script" to "echo \${name}")
        val task = buildTask(replaceVarOnWorker = null, params = params)
        Assertions.assertSame(task, BuildVarOverflowExprSupport.replaceClassicTaskParams(task))
    }

    private fun buildTask(replaceVarOnWorker: Boolean?, params: Map<String, String>) = BuildTask(
        buildId = "b-1",
        vmSeqId = "1",
        status = BuildTaskStatus.DO,
        taskId = "t-1",
        params = params,
        buildVariable = variables,
        replaceVarOnWorker = replaceVarOnWorker
    )
}

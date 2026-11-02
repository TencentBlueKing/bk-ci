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
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
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

    /** 带缩进的 JSON 文本参数走字符串替换路径，变量值仍只进入字符串叶子，结果必须仍是合法 JSON。 */
    @Test
    fun replaceOnWorkerKeepsFormattedJsonTextValid() {
        val payload = "{\"first\":1}{\"second\":2}"
        val formattedData = """
            {
              "input": {
                "simple": "${'$'}{payload}",
                "struct": { "desc": "${'$'}{payload}" }
              }
            }
        """.trimIndent()

        val replacedData = BuildVarOverflowExprSupport.replaceClassicTaskParams(
            buildTask(
                replaceVarOnWorker = true,
                params = mapOf("data" to formattedData),
                variables = mapOf("payload" to payload)
            )
        ).params!!["data"]!!

        val input = (JsonUtil.toMap(replacedData)["input"] as Map<*, *>)
        Assertions.assertEquals(payload, input["simple"])
        Assertions.assertEquals(payload, (input["struct"] as Map<*, *>)["desc"])
    }

    /**
     * 复现线上场景：经典方言 + 新构建机，插件 data 参数引用一个值本身是「带换行与转义引号的 JSON 文本」
     * 的大变量。替换后 data 必须仍能被 [JsonUtil.toMutableMap] 解析（MarketAtomTask 读取入参的方式）。
     */
    @Test
    fun replaceOnWorkerKeepsMarketDataJsonValidWhenValueIsFormattedJsonText() {
        val modelText = "{\n  \"@type\": \"Model\",\n  \"name\": \"构建\\\"测试\\\"\",\n" +
            "  \"desc\": \"必填 \${name} 与 \$x\",\n  \"stages\": [{\"id\": \"stage-1\"}]\n}"
        val realValue = JsonUtil.toJson(mapOf("type" to "string", "value" to modelText), formatted = false)
        val exprKey = "jobs.job_tl1.steps.mfKTO5.outputs.testResult1"
        val data = "{\"input\":{\"desc\":\"\${{$exprKey}}\"}," +
            "\"output\":{\"testResult\":\"string\"}}"

        val replacedData = BuildVarOverflowExprSupport.replaceClassicTaskParams(
            buildTask(
                replaceVarOnWorker = true,
                params = mapOf("data" to data),
                variables = mapOf(exprKey to realValue)
            )
        ).params!!["data"]!!

        // MarketAtomTask.execute 读取 data 的方式
        val input = JsonUtil.toMutableMap(replacedData)["input"] as Map<*, *>
        Assertions.assertEquals(realValue, input["desc"])
    }

    /**
     * 根因回归：`data` 的字符串值里带**裸控制字符**（换行/制表符）时，fge 的严格解析器会判定非法、
     * 而插件侧的 Jackson 能正常解析。若按前者判定就会退化成文本拼接，把大变量原样拼进 JSON。
     * 这里断言这类参数仍按结构替换，结果对 [JsonUtil.toMutableMap] 保持可解析。
     */
    @Test
    fun replaceOnWorkerKeepsJsonValidWhenParamContainsRawControlChars() {
        val modelText = "{\n  \"@type\": \"Model\",\n  \"name\": \"构建-并行打包\"\n}"
        // 引擎对字符串型参数是原样下发，裸换行/制表符会保留在参数文本里
        val data = "{\"input\":{\"desc\":\"\${payload}\",\"note\":\"line1\nline2\tend\"}," +
            "\"output\":{\"testResult\":\"string\"}}"

        val replacedData = BuildVarOverflowExprSupport.replaceClassicTaskParams(
            buildTask(
                replaceVarOnWorker = true,
                params = mapOf("data" to data),
                variables = mapOf("payload" to modelText)
            )
        ).params!!["data"]!!

        val input = JsonUtil.toMutableMap(replacedData)["input"] as Map<*, *>
        Assertions.assertEquals(modelText, input["desc"])
        Assertions.assertEquals("line1\nline2\tend", input["note"])
    }

    @Test
    fun replaceOnWorkerKeepsMarketDataJsonValidWhenValueLooksLikeJson() {
        val payload = "{\"first\":1}{\"second\":2}"
        val buildVariables = mapOf("payload" to payload)
        val rawData = mapOf(
            "input" to mapOf(
                "simple" to "\${payload}",
                "struct" to mapOf("desc" to "\${payload}")
            ),
            "output" to emptyMap<String, Any>()
        )

        val replacedData = BuildVarOverflowExprSupport.replaceClassicTaskParams(
            buildTask(
                replaceVarOnWorker = true,
                params = mapOf("data" to JsonUtil.toJson(rawData, false)),
                variables = buildVariables
            )
        ).params!!["data"]!!

        val dataMap = JsonUtil.toMutableMap(replacedData)
        val input = dataMap["input"] as Map<*, *>
        Assertions.assertEquals(payload, input["simple"])
        Assertions.assertEquals(payload, (input["struct"] as Map<*, *>)["desc"])
    }

    /**
     * 表达式方言下的任务参数替换（[BuildVarOverflowExprSupport.replaceTaskParam]）：
     * 大变量真实值是非法 JSON 片段时，也只能落到字符串叶子上，外层参数 JSON 必须仍可解析。
     */
    @Test
    fun replaceTaskParamKeepsContainerValidForExpressionDialect() {
        val payload = "{\"first\":1}{\"second\":2}"
        val reference = "__BK_OVF__:${payload.length}:1:${"a".repeat(64)}"
        val data = JsonUtil.toJson(
            mapOf("input" to mapOf("desc" to "\${{ testResult1 }}")),
            formatted = false
        )

        val replaced = BuildVarOverflowExprSupport.replaceTaskParam(
            value = data,
            variables = mapOf("testResult1" to reference),
            dialect = PipelineDialectType.CONSTRAINED.dialect,
            overflowKeys = setOf("testResult1"),
            loader = { payload }
        )

        val input = JsonUtil.toMap(replaced)["input"] as Map<*, *>
        Assertions.assertEquals(payload, input["desc"])
    }

    @Test
    fun skipWhenEngineAlreadyReplaced() {
        val params = mapOf("script" to "echo \${name}")
        val task = buildTask(replaceVarOnWorker = null, params = params)
        Assertions.assertSame(task, BuildVarOverflowExprSupport.replaceClassicTaskParams(task))
    }

    private fun buildTask(
        replaceVarOnWorker: Boolean?,
        params: Map<String, String>,
        variables: Map<String, String> = this.variables
    ) = BuildTask(
        buildId = "b-1",
        vmSeqId = "1",
        status = BuildTaskStatus.DO,
        taskId = "t-1",
        params = params,
        buildVariable = variables,
        replaceVarOnWorker = replaceVarOnWorker
    )
}

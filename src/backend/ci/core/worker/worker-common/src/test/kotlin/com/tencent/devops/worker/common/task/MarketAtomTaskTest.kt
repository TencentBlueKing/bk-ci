package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.KeyReplacement
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParseException
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.pipeline.EnvReplacementParser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@Suppress("ALL")
internal class MarketAtomTaskTest {

    @Test
    fun inputTest() {
        val variables = mapOf(
            "host1" to "127.0.0.1",
            "service" to "process",
            "port" to "8080"
        )
        val inputParam = mapOf(
            "bizId" to "100205",
            "globalVarStr" to
                "[{\"server\":{\"ip_list\":[{\"ip\":\"\${host1}\"}]}}," +
                "{\"name\":\"service\",\"value\":\"\${service}\"},\"value\":\"\${port}\"}]",
            "planId" to "17667"
        )
        Assertions.assertEquals(
            originReplacement(inputParam, variables),
            newReplacement(inputParam, variables)
        )
    }

    private fun originReplacement(
        inputMap: Map<String, Any>,
        variables: Map<String, String>
    ): Map<String, String> {
        val atomParams = mutableMapOf<String, String>()
        inputMap.forEach { (name, value) ->
            // 修复插件input环境变量替换问题 #5682
            atomParams[name] = EnvUtils.parseEnv(
                command = JsonUtil.toJson(value),
                data = variables
            )
        }
        return atomParams
    }
    private fun newReplacement(
        inputMap: Map<String, Any>,
        variables: Map<String, String>
    ): Map<String, String> {
        val atomParams = mutableMapOf<String, String>()
        val context = ExecutionContext(DictionaryContextData())
        val nameValue = mutableListOf<NamedValueInfo>()
        ExpressionParser.fillContextByMap(variables, context, nameValue)
        val replacement = object : KeyReplacement {
            override fun getReplacement(key: String): String? {
                return try {
                    ExpressionParser.evaluateByContext(key, context, nameValue, true)?.let {
                        JsonUtil.toJson(it, false)
                    }
                } catch (ignore: ExpressionParseException) {
                    println("Expression evaluation failed: ")
                    ignore.printStackTrace()
                    null
                }
            }
        }
        inputMap.forEach { (name, value) ->
            atomParams[name] = EnvReplacementParser.parse(
                obj = JsonUtil.toJson(value),
                contextMap = variables,
                replacement = replacement
            )
        }
        return atomParams
    }
}

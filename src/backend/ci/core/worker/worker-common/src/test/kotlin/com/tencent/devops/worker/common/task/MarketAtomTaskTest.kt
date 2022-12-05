package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.expression.ExecutionContext
import com.tencent.devops.common.expression.ExpressionParser
import com.tencent.devops.common.expression.context.DictionaryContextData
import com.tencent.devops.common.expression.context.PipelineContextData
import com.tencent.devops.common.expression.context.RuntimeNamedValue
import com.tencent.devops.common.expression.context.StringContextData
import com.tencent.devops.common.expression.expression.sdk.NamedValueInfo
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.ticket.pojo.CredentialInfo
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.worker.common.expression.SpecialFunctions
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

    @Test
    fun credentialTest() {
        val variables = mapOf(
            "host1" to "127.0.0.1",
            "service" to "process",
            "port" to "8080"
        )
        val r = EnvReplacementParser.getCustomExecutionContextByMap(
            variables, listOf(CredentialRuntimeNamedValue())
        )
        Assertions.assertEquals(
            "1234",
            EnvReplacementParser.parse(
                value = "\${{ settings.a.password }}",
                contextMap = variables,
                onlyExpression = true,
                contextPair = r,
                functions = SpecialFunctions.functions
            )
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
        inputMap.forEach { (name, value) ->
            atomParams[name] = EnvReplacementParser.parse(
                value = JsonUtil.toJson(value),
                contextMap = variables,
                contextPair = Pair(context, nameValue),
                functions = SpecialFunctions.functions
            )
        }
        return atomParams
    }

    class CredentialRuntimeNamedValue(
        override val key: String = "settings"
    ) : RuntimeNamedValue {
        override fun getValue(key: String): PipelineContextData? {
            return DictionaryContextData().apply {
                try {
//                    val pair = DHUtil.initKey()
//                    val credentialInfo = requestCredential(key, pair, targetProjectId).data!!
//                    val credentialList = getDecodedCredentialList(credentialInfo, pair)
                    val credentialInfo = CredentialInfo("", CredentialType.PASSWORD, "123")
                    val credentialList = listOf("1234")
                    val keyMap = CredentialType.Companion.getKeyMap(credentialInfo.credentialType.name)
                    println("[$key]|credentialInfo=$credentialInfo|credentialList=$credentialList|$keyMap")
                    credentialList.forEachIndexed { index, credential ->
                        val token = keyMap["v${index + 1}"] ?: return@forEachIndexed
                        add(token, StringContextData(credential))
                    }
                } catch (ignore: Throwable) {
                    ignore.printStackTrace()
                    return null
                }
            }
        }
    }
}

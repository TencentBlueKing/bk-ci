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

package com.tencent.devops.stream.trigger.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.Container
import com.tencent.devops.common.ci.v2.Credentials
import com.tencent.devops.common.ci.v2.GitNotices
import com.tencent.devops.common.ci.v2.Service
import com.tencent.devops.common.ci.v2.ServiceWith
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.Strategy
import com.tencent.devops.common.ci.v2.Variable
import com.tencent.devops.common.ci.v2.exception.YamlFormatException
import com.tencent.devops.stream.trigger.template.pojo.enums.TemplateType

object YamlObjects {

    fun getVariable(variable: Map<String, Any>): Variable {
        return Variable(
            value = variable["value"]?.toString(),
            readonly = getNullValue("readonly", variable)?.toBoolean()
        )
    }

    fun getStep(fromPath: String, step: Map<String, Any>): Step {
        return Step(
            name = step["name"]?.toString(),
            id = step["id"]?.toString(),
            ifFiled = step["if"]?.toString(),
            uses = step["uses"]?.toString(),
            with = if (step["with"] == null) {
                null
            } else {
                transValue<Map<String, Any>>(fromPath, "with", step["with"])
            },
            timeoutMinutes = getNullValue("timeout-minutes", step)?.toInt(),
            continueOnError = getNullValue("continue-on-error", step)?.toBoolean(),
            retryTimes = getNullValue("retry-times", step)?.toInt(),
            env = if (step["env"] == null) {
                null
            } else {
                transValue<Map<String, Any?>>(fromPath, "env", step["env"])
            },
            run = step["run"]?.toString(),
            checkout = step["checkout"]?.toString()
        )
    }

    fun getService(fromPath: String, service: Any): Map<String, Service> {
        val serviceMap = transValue<Map<String, Any?>>(fromPath, "services", service)
        val newServiceMap = mutableMapOf<String, Service>()
        serviceMap.forEach { (key, value) ->
            val newValue = transValue<Map<String, Any>>(fromPath, "services", value)
            val with = transValue<Map<String, Any>>(fromPath, "with", newValue["with"])
            newServiceMap.putAll(
                mapOf(
                    key to Service(
                        image = getNotNullValue(key = "image", mapName = "Container", map = newValue),
                        with = ServiceWith(
                            password = getNotNullValue(key = "password", mapName = "with", map = with)
                        )
                    )
                )
            )
        }
        return newServiceMap
    }

    fun getContainer(fromPath: String, container: Any): Container {
        val containerMap = transValue<Map<String, Any?>>(fromPath, "container", container)
        return Container(
            image = getNotNullValue(key = "image", mapName = "Container", map = containerMap),
            credentials = if (containerMap["credentials"] == null) {
                null
            } else {
                val credentialsMap =
                    transValue<Map<String, String>>(fromPath, "credentials", containerMap["credentials"])
                Credentials(
                    username = credentialsMap["username"]!!,
                    password = credentialsMap["password"]!!
                )
            }
        )
    }

    fun getStrategy(fromPath: String, strategy: Any?): Strategy? {
        val strategyMap = transValue<Map<String, Any?>>(fromPath, "strategy", strategy)
        val matrix = strategyMap["matrix"] ?: return null
        return Strategy(
            matrix = matrix,
            fastKill = getNullValue("fast-kill", strategyMap)?.toBoolean(),
            maxParallel = getNullValue("max-parallel", strategyMap)?.toInt()
        )
    }

    fun getNotice(fromPath: String, notice: Map<String, Any?>): GitNotices {
        return GitNotices(
            type = notice["type"].toString(),
            title = notice["title"]?.toString(),
            ifField = notice["if"]?.toString(),
            content = notice["content"]?.toString(),
            receivers = if (notice["receivers"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "receivers", notice["receivers"]).toSet()
            },
            ccs = if (notice["ccs"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "ccs", notice["ccs"]).toSet()
            },
            chatId = if (notice["chat-id"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "receivers", notice["chat-id"]).toSet()
            }
        )
    }

    inline fun <reified T> getObjectFromYaml(path: String, template: String): T {
        return try {
            YamlUtil.getObjectMapper().readValue(template, object : TypeReference<T>() {})
        } catch (e: Exception) {
            throw YamlFormatException(YamlTemplate.YAML_FORMAT_ERROR.format(path, e.message))
        }
    }

    fun checkTemplate(path: String, yaml: String, type: TemplateType) {
        val yamlMap = getObjectFromYaml<Map<String, Any?>>(path, yaml)
        if (type == TemplateType.EXTEND) {
            // extend后不能包含on
            if (yamlMap.contains("on")) {
                error(YamlTemplate.EXTENDS_TEMPLATE_ON_ERROR.format(path))
            }
            // extend后不能接extend模板
            if (yamlMap.contains("extends")) {
                error(YamlTemplate.EXTENDS_TEMPLATE_EXTENDS_ERROR.format(path))
            }
        } else {
            yamlMap.forEach { (key, _) ->
                if (key != "parameters" && key != "resources" && key != type.content) {
                    error(YamlTemplate.TEMPLATE_KEYWORDS_ERROR.format(path, type.text, type.content))
                }
            }
        }
    }

    private inline fun <reified T> transValue(file: String, type: String, value: Any?): T {
        if (value == null) {
            throw YamlFormatException(YamlTemplate.TRANS_AS_ERROR.format(file, type))
        }
        return try {
            value as T
        } catch (e: Exception) {
            throw YamlFormatException(YamlTemplate.TRANS_AS_ERROR.format(file, type))
        }
    }

    inline fun <reified T> transNullValue(file: String, type: String, key: String, map: Map<String, Any?>): T? {
        return if (map[key] == null) {
            null
        } else {
            return try {
                map[key] as T
            } catch (e: Exception) {
                throw YamlFormatException(YamlTemplate.TRANS_AS_ERROR.format(file, type))
            }
        }
    }

    fun getNullValue(key: String, map: Map<String, Any?>): String? {
        return if (map[key] == null) {
            null
        } else {
            map[key].toString()
        }
    }

    private fun getNotNullValue(key: String, mapName: String, map: Map<String, Any?>): String {
        return if (map[key] == null) {
            throw YamlFormatException(YamlTemplate.ATTR_MISSING_ERROR.format(key, mapName))
        } else {
            map[key].toString()
        }
    }

    private fun error(content: String) {
        throw YamlFormatException(content)
    }
}

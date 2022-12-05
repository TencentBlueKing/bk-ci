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

package com.tencent.devops.stream.trigger.parsers.yamlCheck

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.stream.config.TXStreamGitConfig
import com.tencent.devops.stream.service.StreamGitTokenService
import com.tencent.devops.stream.service.StreamScmService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.util.RetryUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml

@Primary
@Component
class TXYamlSchemaCheck @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val scmService: StreamScmService,
    private val streamGitTokenService: StreamGitTokenService,
    private val txStreamGitConfig: TXStreamGitConfig
) : YamlSchemaCheck() {

    companion object {
        private const val REDIS_STREAM_YAML_SCHEMA = "stream:yaml.schema.v2:json"
        private const val CI_SCHEMA = "ci"
        private const val TEMPLATE_EXTEND_SCHEMA = "template-extends"
        private const val TEMPLATE_STAGE_SCHEMA = "template-stages"
        private const val TEMPLATE_JOB_SCHEMA = "template-jobs"
        private const val TEMPLATE_STEP_SCHEMA = "template-steps"
        private const val TEMPLATE_VARIABLE_SCHEMA = "template-variables"
        private const val TEMPLATE_GATE_SCHEMA = "template-gates"
    }

    private val logger = LoggerFactory.getLogger(YamlSchemaCheck::class.java)

    private val yaml = Yaml()

    private val schemaFactory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
        .objectMapper(YamlUtil.getObjectMapper())
        .build()

    private val schemaMap = mutableMapOf<String, JsonSchema>()

    // 给来自前端的接口用，直接扔出去就好
    override fun check(originYaml: String, templateType: TemplateType?, isCiFile: Boolean) {
        checkYamlSchema(originYaml, templateType, isCiFile)
    }

    override fun check(action: BaseAction, templateType: TemplateType?, isCiFile: Boolean) {
        schemaExceptionHandle(action) {
            checkYamlSchema(action.data.context.originYaml!!, templateType, isCiFile)
        }
    }

    private fun checkYamlSchema(originYaml: String, templateType: TemplateType? = null, isCiFile: Boolean) {
        val loadYaml = try {
            RetryUtils.retryAnyException {
                YamlUtil.toYaml(Yaml().load(originYaml))
            }
        } catch (ignored: Throwable) {
            logger.warn("TX_YAML_SCHEMA_CHECK|originYaml=$originYaml", ignored)
            throw YamlFormatException("There may be a problem with your yaml syntax ${ignored.message}")
        }
        // 解析锚点
        val yamlJson = YamlUtil.getObjectMapper().readTree(loadYaml).replaceOn()
        // v1 不走这里的校验逻辑
        if (yamlJson.checkV1()) {
            return
        }
        if (isCiFile) {
            getSchema(CI_SCHEMA).check(yamlJson)
            // 校验schema后有一些特殊的校验
            yamlJson.checkCiRequired()
            yamlJson.checkVariablesFormat()
        }
        if (templateType == null) {
            return
        }
        when (templateType) {
            TemplateType.EXTEND -> {
                getSchema(TEMPLATE_EXTEND_SCHEMA).check(yamlJson)
                yamlJson.checkExtendsRequired()
                yamlJson.checkVariablesFormat()
            }
            TemplateType.VARIABLE -> {
                getSchema(TEMPLATE_VARIABLE_SCHEMA).check(yamlJson)
                yamlJson.checkVariablesFormat()
            }
            TemplateType.STAGE -> getSchema(TEMPLATE_STAGE_SCHEMA).check(yamlJson)
            TemplateType.GATE -> getSchema(TEMPLATE_GATE_SCHEMA).check(yamlJson)
            TemplateType.JOB -> getSchema(TEMPLATE_JOB_SCHEMA).check(yamlJson)
            TemplateType.STEP -> getSchema(TEMPLATE_STEP_SCHEMA).check(yamlJson)
            else -> {
                return
            }
        }
    }

    private fun getSchema(file: String): JsonSchema {
        // TODO: 上线schema时先实验一段时间，使用redis做兜底，没有问题后下掉redis逻辑
        // 先去取下redis看看有没有变量，没有拿代码里初始化好的变量
        val str = redisOperation.get("$REDIS_STREAM_YAML_SCHEMA:$file") ?: return getSchemaFromGit(file)
        return schemaFactory.getSchema(str)
    }
}

private fun JsonSchema.check(yaml: JsonNode) {
    validate(yaml).let {
        if (!it.isNullOrEmpty()) {
            throw YamlFormatException(it.toString())
        }
    }
}

// Yaml规则下会将on当成true在消除锚点时会将On替换为true
private fun JsonNode.replaceOn(): JsonNode {
    val realOn = get("true") ?: return this
    val node = this as ObjectNode
    node.set<JsonNode>("on", realOn)
    node.remove("true")
    return this
}

private fun JsonNode.checkV1(): Boolean {
    return get("version")?.textValue() != "v2.0"
}

private fun JsonNode.checkVariablesFormat() {
    val vars = get("variables") ?: return
    val keyRegex = Regex("^[0-9a-zA-Z_]+$")
    vars.fields().forEach {
        if (!keyRegex.matches(it.key)) {
            throw YamlFormatException("变量名称必须是英文字母、数字或下划线(_)")
        }
    }
}

private fun JsonNode.checkCiRequired() {
    val requiredList = listOf("stages", "jobs", "steps", "extends")
    requiredList.forEach {
        if (get(it) != null) {
            return
        }
    }
    throw YamlFormatException("stages, jobs, steps, extends 必须存在一个")
}

private fun JsonNode.checkExtendsRequired() {
    if (get("stages") == null && get("jobs") == null && get("steps") == null) {
        throw YamlFormatException("stages, jobs, steps, extends 必须存在一个")
    }
}

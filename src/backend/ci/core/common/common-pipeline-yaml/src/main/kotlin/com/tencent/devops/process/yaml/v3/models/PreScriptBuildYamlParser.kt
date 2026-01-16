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

package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.tencent.devops.common.pipeline.pojo.transfer.IPreStep
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.pojo.YamlVersionParser
import com.tencent.devops.process.yaml.v3.models.job.IPreJob
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.IPreStage
import org.slf4j.LoggerFactory

/**
 * PreScriptBuildYamlI 是PreScriptBuildYaml的拓展，方便再既不修改data class的特性情况下，其他类可以在继承新增字段
 * 注：PreScriptBuildYaml 新增的字段需要在这里新增
 */
interface PreScriptBuildYamlIParser : YamlVersionParser {
    var version: String?
    var name: String?
    var label: List<String>?
    var variables: Map<String, Variable>?
    var variableTemplates: List<VariableTemplate>?
    var stages: List<IPreStage>?
    var jobs: LinkedHashMap<String, IPreJob>?
    var steps: List<IPreStep>?
    var extends: PreExtends?
    var resources: Resources?
    var finally: LinkedHashMap<String, IPreJob>?
    val concurrency: Concurrency?
    val disablePipeline: Boolean?
    val recommendedVersion: RecommendedVersion?
    val customBuildNum: String?
    val syntaxDialect: String?
    val failIfVariableInvalid: Boolean?
    val cancelPolicy: String?
}

/**
 * model
 *
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(stream,prebuild等)异常
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreScriptBuildYamlParser(
    override var version: String?,
    override var name: String?,
    override var label: List<String>? = null,
    @JsonProperty("on")
    var triggerOn: PreTriggerOn?,
    override var variables: Map<String, Variable>? = null,
    override var variableTemplates: List<VariableTemplate>? = null,
    override var stages: List<IPreStage>? = null,
    override var jobs: LinkedHashMap<String, IPreJob>? = null,
    override var steps: List<IPreStep>? = null,
    override var extends: PreExtends? = null,
    override var resources: Resources?,
    var notices: List<GitNotices>?,
    override var finally: LinkedHashMap<String, IPreJob>? = null,
    override val concurrency: Concurrency? = null,
    override val disablePipeline: Boolean? = null,
    override val recommendedVersion: RecommendedVersion? = null,
    override val customBuildNum: String? = null,
    override val syntaxDialect: String?,
    override val failIfVariableInvalid: Boolean? = null,
    override val cancelPolicy: String? = null
) : PreScriptBuildYamlIParser {
    override fun yamlVersion() = YamlVersion.V2_0

    companion object {
        private val logger = LoggerFactory.getLogger(PreScriptBuildYamlParser::class.java)
    }

    @JsonSetter("variables")
    private fun setVariables(raw: Any?) {
        when (raw) {
            null -> {
                // 显式处理 null 情况，清空变量
                this.variables = null
                this.variableTemplates = null
            }
            is Map<*, *> -> {
                // 提取template数据
                val templateList = raw["template"] as? List<Map<String, String>>
                if (raw["template"] != null && templateList == null) {
                    logger.warn(
                        "variables.template 字段类型转换失败，" +
                            "期望 List<Map<String, String>>，实际类型: ${raw["template"]?.javaClass?.name}"
                    )
                }
                this.variableTemplates = templateList
                    ?.filter { !it["name"].isNullOrBlank() }?.map {
                        VariableTemplate(name = it["name"]!!, version = it["version"])
                    }
                // 过滤掉 template 字段和 null key，并转换为 String key
                val regularVariables = raw.filterKeys { key ->
                    if (key == null) {
                        logger.warn("variables 中存在 null key，已忽略")
                        false
                    } else {
                        key != "template"
                    }
                }.mapKeys { entry ->
                    entry.key.toString()
                }
                
                if (regularVariables.isNotEmpty()) {
                    this.variables = regularVariables.mapValues { parseVariableValue(value = it.value) }
                } else {
                    this.variables = null
                }
            }
            else -> {
                // 处理非预期类型
                logger.warn(
                    "variables 字段类型不支持，期望 Map<String, Any>，" +
                        "实际类型: ${raw.javaClass.name}，值: $raw"
                )
                this.variables = null
                this.variableTemplates = null
            }
        }
    }

    private fun parseVariableValue(value: Any?): Variable {
        return when (value) {
            null -> {
                // 显式处理 null 值
                logger.warn("变量值为 null，将创建空值变量")
                Variable(value = null)
            }
            // 处理对象格式
            is Map<*, *> -> parseVariable(map = value)
            // 处理简单值格式（字符串、数字、布尔等）
            else -> Variable(value = value)
        }
    }

    private fun parseVariable(map: Map<*, *>): Variable {
        return Variable(
            value = map["value"],
            readonly = map["readonly"] as? Boolean,
            allowModifyAtStartup = map["allow-modify-at-startup"] as? Boolean,
            const = map["const"] as? Boolean,
            props = map["props"] as? VariableProps
        )
    }
}

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

package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.pojo.YamlVersionParser
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.Stage
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "version",
    defaultImpl = PreTemplateScriptBuildYamlParser::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlV3Parser::class, name = YamlVersion.V3),
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlParser::class, name = YamlVersion.V2)
)
interface IPreTemplateScriptBuildYamlParser : YamlVersionParser {
    val version: String?
    val name: String?
    val desc: String?
    val label: List<String>?
    val notices: List<Notices>?
    var concurrency: Concurrency?
    var disablePipeline: Boolean?
    var recommendedVersion: RecommendedVersion?
    var customBuildNum: String?
    var syntaxDialect: String?
    var failIfVariableInvalid: Boolean?

    fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlIParser)

    fun formatVariables(): Map<String, Variable>

    fun formatTriggerOn(default: ScmType): List<Pair<TriggerType, TriggerOn>>

    fun formatStages(): List<Stage>

    fun formatFinallyStage(): List<Job>

    fun formatResources(): Resources?

    fun templateFilter(): ITemplateFilter
}

/*
* ITemplateFilter 为模板替换所需材料
*/
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "version",
    defaultImpl = PreTemplateScriptBuildYamlParser::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlV3Parser::class, name = YamlVersion.V3),
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlParser::class, name = YamlVersion.V2)
)
interface ITemplateFilter : YamlVersionParser {
    var variables: Map<String, Any>?
    var stages: ArrayList<Map<String, Any>>?
    val jobs: LinkedHashMap<String, Any>?
    val steps: ArrayList<Map<String, Any>>?
    var extends: Extends?
    var resources: Resources?
    var finally: LinkedHashMap<String, Any>?

    fun initPreScriptBuildYamlI(): PreScriptBuildYamlIParser
}

/**
 * model
 *
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateScriptBuildYamlParser(
    override var version: String?,
    override val name: String?,
    override val desc: String?,
    override val label: List<String>? = null,
    @JsonProperty("on")
    var triggerOn: PreTriggerOn? = null,
    override var variables: Map<String, Any>? = null,
    override var stages: ArrayList<Map<String, Any>>? = null,
    override val jobs: LinkedHashMap<String, Any>? = null,
    override val steps: ArrayList<Map<String, Any>>? = null,
    override var extends: Extends? = null,
    override var resources: Resources? = null,
    override var finally: LinkedHashMap<String, Any>? = null,
    override val notices: List<GitNotices>?,
    override var concurrency: Concurrency? = null,
    @JsonProperty("disable-pipeline")
    override var disablePipeline: Boolean? = null,
    @JsonProperty("recommended-version")
    override var recommendedVersion: RecommendedVersion? = null,
    @JsonProperty("custom-build-num")
    override var customBuildNum: String? = null,
    @JsonProperty("syntax-dialect")
    override var syntaxDialect: String? = null,
    @JsonProperty("fail-if-variable-invalid")
    override var failIfVariableInvalid: Boolean? = null
) : IPreTemplateScriptBuildYamlParser, ITemplateFilter {

    init {
        version = YamlVersion.V2_0.tag
    }

    override fun yamlVersion() = YamlVersion.V2_0

    override fun initPreScriptBuildYamlI(): PreScriptBuildYamlIParser {
        return PreScriptBuildYamlParser(
            version = version,
            name = name,
            label = label,
            triggerOn = triggerOn,
            resources = resources,
            notices = notices,
            concurrency = concurrency,
            syntaxDialect = syntaxDialect,
            failIfVariableInvalid = failIfVariableInvalid
        )
    }

    @JsonIgnore
    lateinit var preYaml: PreScriptBuildYamlParser

    override fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlIParser) {
        preYaml = f(this) as PreScriptBuildYamlParser
    }

    override fun formatVariables(): Map<String, Variable> {
        checkInitialized()
        return preYaml.variables ?: emptyMap()
    }

    override fun formatTriggerOn(default: ScmType): List<Pair<TriggerType, TriggerOn>> {
        val format = ScriptYmlUtils.formatTriggerOn(triggerOn)
        return listOf(TriggerType.BASE to format, TriggerType.parse(default) to format)
    }

    override fun formatStages(): List<Stage> {
        checkInitialized()
        return ScriptYmlUtils.formatStage(preYaml)
    }

    override fun formatFinallyStage(): List<Job> {
        checkInitialized()
        return ScriptYmlUtils.preJobs2Jobs(preYaml.finally)
    }

    override fun formatResources(): Resources? {
        return resources
    }

    override fun templateFilter(): ITemplateFilter = this

    private fun checkInitialized() {
        if (!this::preYaml.isInitialized) throw RuntimeException("need replaceTemplate before")
    }
}

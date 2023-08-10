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

package com.tencent.devops.process.yaml.v2.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.Stage
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "version",
    defaultImpl = PreTemplateScriptBuildYaml::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PreTemplateScriptBuildYamlV3::class, name = YamlVersion.Version.V3),
    JsonSubTypes.Type(value = PreTemplateScriptBuildYaml::class, name = YamlVersion.Version.V2)
)
interface IPreTemplateScriptBuildYaml : YamlVersion {
    val version: String?
    val name: String?
    val label: List<String>?
    val notices: List<GitNotices>?
    val concurrency: Concurrency?

    fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlI)

    fun formatVariables(): Map<String, Variable>

    fun formatTriggerOn(default: ScmType): Map<ScmType, TriggerOn>

    fun formatStages(): List<Stage>

    fun formatFinallyStage(): List<Job>

    fun formatResources(): Resources?
}

/*
* ITemplateFilter 为模板替换所需材料
*/
interface ITemplateFilter : YamlVersion {
    val variables: Map<String, Any>?
    val stages: List<Map<String, Any>>?
    val jobs: Map<String, Any>?
    val steps: List<Map<String, Any>>?
    val extends: Extends?
    val resources: Resources?
    var finally: Map<String, Any>?

    fun initPreScriptBuildYamlI(): PreScriptBuildYamlI
}

/**
 * model
 *
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateScriptBuildYaml(
    override val version: String?,
    override val name: String?,
    override val label: List<String>? = null,
    @JsonProperty("on")
    val triggerOn: PreTriggerOn?,
    override val variables: Map<String, Any>?,
    override val stages: List<Map<String, Any>>?,
    override val jobs: Map<String, Any>? = null,
    override val steps: List<Map<String, Any>>? = null,
    override val extends: Extends?,
    override val resources: Resources?,
    override val notices: List<GitNotices>?,
    override var finally: Map<String, Any>?,
    override val concurrency: Concurrency? = null
) : IPreTemplateScriptBuildYaml, ITemplateFilter {
    override fun yamlVersion() = YamlVersion.Version.V2_0

    override fun initPreScriptBuildYamlI(): PreScriptBuildYamlI {
        return PreScriptBuildYaml(
            version = version,
            name = name,
            label = label,
            triggerOn = triggerOn,
            resources = resources,
            notices = notices,
            concurrency = concurrency
        )
    }

    @JsonIgnore
    lateinit var preYaml: PreScriptBuildYaml

    override fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlI) {
        preYaml = f(this) as PreScriptBuildYaml
    }

    override fun formatVariables(): Map<String, Variable> {
        checkInitialized()
        return preYaml.variables ?: emptyMap()
    }

    override fun formatTriggerOn(default: ScmType): Map<ScmType, TriggerOn> {
        return mapOf(default to ScriptYmlUtils.formatTriggerOn(triggerOn))
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
        checkInitialized()
        return preYaml.resources
    }

    private fun checkInitialized() {
        if (!this::preYaml.isInitialized) throw RuntimeException("need replaceTemplate before")
    }
}

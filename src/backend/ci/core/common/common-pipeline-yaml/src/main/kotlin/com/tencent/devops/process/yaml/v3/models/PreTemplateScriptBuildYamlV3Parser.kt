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
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.Stage
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateScriptBuildYamlV3Parser(
    override var version: String?,
    override val name: String?,
    override val desc: String?,
    override val label: List<String>? = null,
    @JsonProperty("on")
    var triggerOn: Any? = null,
    override var variables: Map<String, Any>? = null,
    override var stages: ArrayList<Map<String, Any>>? = null,
    override val jobs: LinkedHashMap<String, Any>? = null,
    override val steps: ArrayList<Map<String, Any>>? = null,
    override var extends: Extends? = null,
    override var resources: Resources? = null,
    override var finally: LinkedHashMap<String, Any>? = null,
    override val notices: List<PacNotices>?,
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
    companion object {
        private val logger = LoggerFactory.getLogger(PreTemplateScriptBuildYamlV3Parser::class.java)
    }

    init {
        version = YamlVersion.V3_0.tag
    }

    override fun yamlVersion() = YamlVersion.V3_0

    override fun initPreScriptBuildYamlI(): PreScriptBuildYamlIParser {
        return PreScriptBuildYamlV3Parser(
            version = version,
            name = name,
            label = label,
            triggerOn = makeRunsOn(),
            resources = resources,
            notices = notices,
            concurrency = concurrency,
            disablePipeline = disablePipeline,
            syntaxDialect = syntaxDialect,
            failIfVariableInvalid = failIfVariableInvalid
        )
    }

    @JsonIgnore
    lateinit var preYaml: PreScriptBuildYamlV3Parser

    private val formatStages = lazy { ScriptYmlUtils.formatStage(preYaml, transferData) }
    private val formatFinallyStage = lazy { ScriptYmlUtils.preJobs2Jobs(preYaml.finally, transferData) }

    @JsonIgnore
    val transferData: YamlTransferData = YamlTransferData()

    override fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlIParser) {
        kotlin.runCatching {
            preYaml = f(this) as PreScriptBuildYamlV3Parser
        }.onFailure { error ->
            logger.warn("replaceTemplate error", error)
            throw PipelineTransferException(
                YAML_NOT_VALID,
                arrayOf(error.message ?: "unknown error")
            )
        }
    }

    override fun formatVariables(): Map<String, Variable> {
        checkInitialized()
        return preYaml.variables ?: emptyMap()
    }

    override fun formatTriggerOn(default: ScmType): List<Pair<TriggerType, TriggerOn>> {
        checkInitialized()
        val runsOn = preYaml.triggerOn ?: return listOf(
            TriggerType.parse(default) to ScriptYmlUtils.formatTriggerOn(null)
        )

        val res = mutableListOf<Pair<TriggerType, TriggerOn>>()
        var baseOk = false
        runsOn.forEach {
            if (!baseOk && it.repoName == null && it.type == null) {
                res.add(TriggerType.BASE to ScriptYmlUtils.formatTriggerOn(it))
                baseOk = true
                return@forEach
            }
            res.add((TriggerType.parse(it.type) ?: TriggerType.parse(default)) to ScriptYmlUtils.formatTriggerOn(it))
        }
        return res
    }

    override fun formatStages(): List<Stage> {
        checkInitialized()
        return formatStages.value
    }

    override fun formatFinallyStage(): List<Job> {
        checkInitialized()
        return formatFinallyStage.value
    }

    override fun formatResources(): Resources? {
        return resources
    }

    override fun templateFilter(): ITemplateFilter = this

    private fun checkInitialized() {
        if (!this::preYaml.isInitialized) throw RuntimeException("need replaceTemplate before")
    }

    private fun makeRunsOn(): List<PreTriggerOnV3>? {
        if (triggerOn == null) return null
        // 简写方式
        if (triggerOn is Map<*, *>) {
            val new = JsonUtil.anyTo(triggerOn, object : TypeReference<PreTriggerOnV3>() {})
            return listOf(PreTriggerOnV3(manual = new.manual, schedules = new.schedules, remote = new.remote), new)
        }
        if (triggerOn is List<*>) {
            return JsonUtil.anyTo(triggerOn, object : TypeReference<List<PreTriggerOnV3>>() {})
        }
        return null
    }
}

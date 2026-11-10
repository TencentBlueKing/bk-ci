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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode.YAML_NOT_VALID
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSettingGroupType
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.v3.models.job.IJob
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.IStage
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateScriptBuildYamlV3Parser(
    override var version: String?,
    override var name: String? = null,
    override var desc: String? = null,
    override var label: List<String>? = null,
    @JsonProperty("on")
    var triggerOn: Any? = null,
    override var variables: Map<String, Any>? = null,
    override var stages: ArrayList<Map<String, Any>>? = null,
    override val jobs: LinkedHashMap<String, Any>? = null,
    override val steps: ArrayList<Map<String, Any>>? = null,
    override var extends: PreExtends? = null,
    override var resources: Resources? = null,
    override var finally: LinkedHashMap<String, Any>? = null,
    override var notices: List<PacNotices>? = null,
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
    override var failIfVariableInvalid: Boolean? = null,
    @JsonProperty("cancel-policy")
    override var cancelPolicy: String? = null,
    @JsonProperty("runs-on")
    override var runsOn: Any? = null
) : IPreTemplateScriptBuildYamlParser, ITemplateFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(PreTemplateScriptBuildYamlV3Parser::class.java)

        // PreTriggerOnV3 已声明的 YAML 字段名（含 @JsonProperty 重命名），对象形态中不在此集合的 key 视为通用触发器
        private val preTriggerOnV3Keys: Set<String> by lazy {
            val mapper = JsonUtil.getObjectMapper()
            mapper.serializationConfig
                .introspect(mapper.constructType(PreTriggerOnV3::class.java))
                .findProperties()
                .map { it.name }
                .toSet()
        }
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
            failIfVariableInvalid = failIfVariableInvalid,
            extends = extends,
            cancelPolicy = cancelPolicy
        )
    }

    @JsonIgnore
    lateinit var preYaml: PreScriptBuildYamlV3Parser

    private val formatExtends = lazy { ScriptYmlUtils.preExtend2Extend(preYaml.extends) }
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

    override fun formatVariableTemplates(): List<VariableTemplate> {
        checkInitialized()
        return preYaml.variableTemplates ?: emptyList()
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
            val type = if (it.type == null) {
                TriggerType.parse(default)
            } else {
                TriggerType.parse(it.type) ?: TriggerType.GENERIC
            }
            res.add(type to ScriptYmlUtils.formatTriggerOn(it))
        }
        return res
    }

    override fun formatStages(): List<IStage> {
        checkInitialized()
        return formatStages.value
    }

    override fun formatFinallyStage(): List<IJob> {
        checkInitialized()
        return formatFinallyStage.value
    }

    override fun formatExtends(): Extends? {
        checkInitialized()
        return formatExtends.value
    }

    override fun formatResources(): Resources? {
        return resources
    }

    override fun templateFilter(): ITemplateFilter = this

    override fun settingGroups(): List<PipelineSettingGroupType>? {
        val res = mutableListOf<PipelineSettingGroupType>()
        if (concurrency != null) {
            res.add(PipelineSettingGroupType.CONCURRENCY)
        }
        if (customBuildNum != null) {
            res.add(PipelineSettingGroupType.CUSTOM_BUILD_NUM)
        }
        if (notices != null) {
            res.add(PipelineSettingGroupType.NOTICES)
        }
        return res
    }

    override fun checkForTemplateUse() = formatExtends()?.template != null

    private fun checkInitialized() {
        if (!this::preYaml.isInitialized) throw RuntimeException("need replaceTemplate before")
    }

    private fun makeRunsOn(): List<PreTriggerOnV3>? {
        if (triggerOn == null) return null
        // 对象形态
        if (triggerOn is Map<*, *>) {
            val map = triggerOn as Map<*, *>
            // 统一（通用）触发器框架「type + 事件类型」形态：事件 key 与 type 平级
            val eventKeys = eventKeys(map)
            // 简写方式（存量）：其余 key 拆成基础触发 + 默认代码库触发
            val rest = map.filterKeys { it !in eventKeys }
            val repoTrigger = JsonUtil.anyTo(rest, object : TypeReference<PreTriggerOnV3>() {})
            eventKeys.forEach { key -> repoTrigger.events[key as String] = map[key] }
            val baseTrigger = PreTriggerOnV3(
                manual = repoTrigger.manual,
                schedules = repoTrigger.schedules,
                remote = repoTrigger.remote
            )
            return listOf(baseTrigger, repoTrigger)
        }
        if (triggerOn is List<*>) {
            return (triggerOn as List<*>).map { item ->
                val pre = JsonUtil.anyTo(item, object : TypeReference<PreTriggerOnV3>() {})
                (item as? Map<*, *>)?.let { map ->
                    eventKeys(map).forEach { key -> pre.events[key as String] = map[key] }
                }
                pre
            }
        }
        return null
    }

    /**
     * 不是 PreTriggerOnV3 已有字段且值为对象的 key，视为通用框架触发器的事件类型（如 arrived）。
     *
     * 事件载荷原样放入 [PreTriggerOnV3.events]，由 type 对应的 TriggerConverter 负责解析，
     * 新增触发器/事件类型无需改动本类与 PreTriggerOnV3。
     */
    private fun eventKeys(map: Map<*, *>): List<Any?> = map.keys.filter { key ->
        key is String && key !in preTriggerOnV3Keys && map[key] is Map<*, *>
    }
}

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

package com.tencent.devops.common.ci.v2.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.tencent.devops.common.api.expression.ExpressionException
import com.tencent.devops.common.api.expression.Lex
import com.tencent.devops.common.api.expression.Word
import com.tencent.devops.common.ci.v2.MrRule
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.v2.PreTriggerOn
import com.tencent.devops.common.ci.v2.PushRule
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.Stage
import com.tencent.devops.common.ci.v2.TagRule
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.common.ci.v2.YmlVersion
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.Container
import com.tencent.devops.common.ci.v2.Container2
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.ParametersType
import com.tencent.devops.common.ci.v2.PreJob
import com.tencent.devops.common.ci.v2.PreStage
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.RunsOn
import com.tencent.devops.common.ci.v2.SchedulesRule
import com.tencent.devops.common.ci.v2.Service
import com.tencent.devops.common.ci.v2.StageLabel
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitMergeActionKind
import com.tencent.devops.common.ci.v2.enums.gitEventKind.TGitMergeExtensionActionKind
import com.tencent.devops.common.ci.v2.exception.YamlFormatException
import com.tencent.devops.common.ci.v2.stageCheck.Flow
import com.tencent.devops.common.ci.v2.stageCheck.PreStageCheck
import com.tencent.devops.common.ci.v2.stageCheck.StageCheck
import com.tencent.devops.common.ci.v2.stageCheck.StageReviews
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.StringReader
import java.util.Random
import java.util.regex.Pattern
import org.apache.commons.text.StringEscapeUtils

@Suppress("MaximumLineLength", "ComplexCondition")
object ScriptYmlUtils {

    private val logger = LoggerFactory.getLogger(ScriptYmlUtils::class.java)

    //    private const val dockerHubUrl = "https://index.docker.io/v1/"

    private const val secretSeed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    private const val stageNamespace = "stage-"
    private const val jobNamespace = "job-"
    private const val stepNamespace = "step-"

    // 用户编写的触发器语法和实际对象不一致
    private const val userTrigger = "on"
    private const val formatTrigger = "triggerOn"

    private const val MAX_SCHEDULES_BRANCHES = 3

    private const val PARAMETERS_PREFIX = "parameters."

    /**
     * 1、解决锚点
     * 2、yml string层面的格式化填充
     */
    // TODO: 删除GitCi微服务后需要修改这里，锚点替换在schema检查已经做了
    @Throws(JsonProcessingException::class)
    fun formatYaml(yamlStr: String): String {
        // replace custom tag
        val yamlNormal = formatYamlCustom(yamlStr)
        // replace anchor tag
        val yaml = Yaml()
        val obj = yaml.load(yamlNormal) as Any
        return YamlUtil.toYaml(obj)
    }

    fun parseVersion(yamlStr: String?): YmlVersion? {
        if (yamlStr == null) {
            return null
        }

        return try {
            val yaml = Yaml()
            val obj = YamlUtil.toYaml(yaml.load(yamlStr) as Any)
            YamlUtil.getObjectMapper().readValue(obj, YmlVersion::class.java)
        } catch (e: Exception) {
            logger.error("Check yaml version failed. return null")
            null
        }
    }

    fun isV2Version(yamlStr: String?): Boolean {
        if (yamlStr == null) {
            return false
        }
        return try {
            val yaml = Yaml()
            val obj = YamlUtil.toYaml(yaml.load(yamlStr) as Any)
            val version = YamlUtil.getObjectMapper().readValue(obj, YmlVersion::class.java)
            version != null && version.version == "v2.0"
        } catch (e: Exception) {
            logger.error("Check yaml version failed. Set default v2.0")
            true
        }
    }

    fun parseVariableValue(value: String?, settingMap: Map<String, String?>): String? {

        if (value == null || value.isEmpty()) {
            return ""
        }

        var newValue = value
        val pattern = Pattern.compile("\\$\\{\\{([^{}]+?)}}")
        val matcher = pattern.matcher(value)
        while (matcher.find()) {
            val realValue = settingMap[matcher.group(1).trim()] ?: continue
            newValue = newValue!!.replace(matcher.group(), realValue)
        }
        logger.info("STREAM|parseVariableValue value :$value; settingMap: $settingMap;newValue: $newValue")
        return newValue
    }

    fun parseParameterValue(value: String?, settingMap: Map<String, Any?>, paramType: ParametersType): String? {
        if (value.isNullOrBlank()) {
            return ""
        }
        var newValue = value
        // ScriptUtils.formatYaml会将所有的带上 "" 但替换时数组不需要"" 所以数组单独匹配
        val pattern = when (paramType) {
            ParametersType.ARRAY -> {
                Pattern.compile("\"\\$\\{\\{([^{}]+?)}}\"")
            }
            else -> {
                Pattern.compile("\\$\\{\\{([^{}]+?)}}")
            }
        }
        val matcher = pattern.matcher(value)
        while (matcher.find()) {
            if (settingMap.containsKey(matcher.group(1).trim())) {
                val realValue = settingMap[matcher.group(1).trim()]
                if (realValue is List<*>) {
                    newValue = newValue!!.replace(matcher.group(), JsonUtil.toJson(realValue))
                } else {
                    newValue = newValue!!.replace(matcher.group(), StringEscapeUtils.escapeJava(realValue.toString()))
                }
            }
        }
        // 替换if中没有加括号的
        val newValueLines = BufferedReader(StringReader(newValue!!))
        val resultValue = StringBuffer()
        var line = newValueLines.readLine()
        while (line != null) {
            if (line.trim().startsWith("if") || line.trim().startsWith("- if")) {
                val ifPrefix = line.substring(0 until line.indexOfFirst { it == ':' } + 1)
                val condition = line.substring(line.indexOfFirst { it == '"' } + 1 until line.length).trimEnd()
                    .removeSuffix("\"")
                // 去掉花括号
                val baldExpress = condition.replace("\${{", "").replace("}}", "").trim()
                val originItems: List<Word>
                // 先语法分析
                try {
                    originItems = Lex(baldExpress.toList().toMutableList()).getToken()
                } catch (e: Exception) {
                    logger.info("expression=$baldExpress|reason=Grammar Invalid: ${e.message}")
                    throw ExpressionException("expression=$baldExpress|reason=Grammar Invalid: ${e.message}")
                }
                // 替换变量
                val items = mutableListOf<Word>()
                originItems.forEach { it ->
                    if (it.symbol == "ident") {
                        items.add(Word(
                            if (it.str.startsWith(PARAMETERS_PREFIX)) {
                                val realValue = settingMap[it.str] ?: it.str
                                if (realValue is List<*>) {
                                    // ["test"]->[test]
                                    JsonUtil.toJson(realValue).replace("\"", "")
                                        .replace("[ ", "[")
                                        .replace(" ]", "]")
                                } else {
                                    StringEscapeUtils.escapeJava(realValue.toString())
                                }
                            } else {
                                it.str
                            },
                            it.symbol)
                        )
                    } else {
                        items.add(Word(it.str, it.symbol))
                    }
                }
                val itemsStr = items.joinToString(" ") { it.str }
                resultValue.append("$ifPrefix \"${itemsStr}\"").append("\n")
            } else {
                resultValue.append(line).append("\n")
            }
            line = newValueLines.readLine()
        }
        val a = resultValue.toString()
        return resultValue.toString()
    }

    private fun formatYamlCustom(yamlStr: String): String {
        val sb = StringBuilder()
        val br = BufferedReader(StringReader(yamlStr))
        var line: String? = br.readLine()
        while (line != null) {
            line = line.trimEnd()
            if (line.startsWith("$userTrigger:")) {
                sb.append("$formatTrigger:").append("\n")
            } else {
                sb.append(line).append("\n")
            }

            line = br.readLine()
        }
        return sb.toString()
    }

    fun checkYaml(preScriptBuildYaml: PreTemplateScriptBuildYaml, yaml: String) {
        checkTriggerOn(preScriptBuildYaml)
        checkVariable(preScriptBuildYaml)
        checkStage(preScriptBuildYaml)
        checkExtend(yaml)
    }

    private fun checkTriggerOn(preScriptBuildYaml: PreTemplateScriptBuildYaml) {
        if (preScriptBuildYaml.triggerOn?.schedules?.branches?.size != null &&
            preScriptBuildYaml.triggerOn.schedules.branches.size > MAX_SCHEDULES_BRANCHES) {
            throw YamlFormatException("定时任务的最大执行分支数不能超过: $MAX_SCHEDULES_BRANCHES")
        }
    }

    private fun checkVariable(preScriptBuildYaml: PreTemplateScriptBuildYaml) {
        if (preScriptBuildYaml.variables == null) {
            return
        }

        preScriptBuildYaml.variables.forEach {
            val keyRegex = Regex("^[0-9a-zA-Z_]+$")
            if (!keyRegex.matches(it.key)) {
                throw YamlFormatException("变量名称必须是英文字母、数字或下划线(_)")
            }
        }
    }

    private fun checkStage(preScriptBuildYaml: PreTemplateScriptBuildYaml) {
        if ((preScriptBuildYaml.stages != null && preScriptBuildYaml.jobs != null) ||
            (preScriptBuildYaml.stages != null && preScriptBuildYaml.steps != null) ||
            (preScriptBuildYaml.jobs != null && preScriptBuildYaml.steps != null)
        ) {
            throw YamlFormatException("stages, jobs, steps不能并列存在，只能存在其一")
        }
    }

    private fun checkExtend(yaml: String) {
        val yamlMap = YamlUtil.getObjectMapper().readValue(yaml, object : TypeReference<Map<String, Any?>>() {})
        if (yamlMap["extends"] == null) {
            return
        }
        yamlMap.forEach { (t, _) ->
            if (t != formatTrigger && t != "extends" && t != "version" &&
                t != "resources" && t != "name" && t != "on") {
                throw YamlFormatException("使用 extends 时顶级关键字只能有触发器 name, on , version 与 resources")
            }
        }
    }

    private fun formatStage(preScriptBuildYaml: PreScriptBuildYaml): List<Stage> {
        return when {
            preScriptBuildYaml.steps != null -> {
                listOf(
                    Stage(
                        name = "stage_1",
                        id = randomString(stageNamespace),
                        jobs = listOf(
                            Job(
                                id = randomString(jobNamespace),
                                name = "job1",
                                runsOn = RunsOn(),
                                steps = formatSteps(preScriptBuildYaml.steps)
                            )
                        ),
                        checkIn = null,
                        checkOut = null
                    )
                )
            }
            preScriptBuildYaml.jobs != null -> {
                listOf(
                    Stage(
                        name = "stage_1",
                        id = randomString(stageNamespace),
                        jobs = preJobs2Jobs(preScriptBuildYaml.jobs),
                        checkIn = null,
                        checkOut = null
                    )
                )
            }
            else -> {
                preStages2Stages(preScriptBuildYaml.stages as List<PreStage>)
            }
        }
    }

    private fun preJobs2Jobs(preJobs: Map<String, PreJob>?): List<Job> {
        if (preJobs == null) {
            return emptyList()
        }

        val jobs = mutableListOf<Job>()
        preJobs.forEach { (t, u) ->
            // 检测job env合法性
            GitCIEnvUtils.checkEnv(u.env)

            val services = mutableListOf<Service>()
            u.services?.forEach { (key, value) ->
                services.add(
                    Service(
                        serviceId = key,
                        image = value.image,
                        with = value.with
                    )
                )
            }

            jobs.add(
                Job(
                    id = t,
                    name = u.name,
                    runsOn = formatRunsOn(u.runsOn),
                    services = services,
                    ifField = u.ifField,
                    steps = formatSteps(u.steps),
                    timeoutMinutes = u.timeoutMinutes,
                    env = u.env,
                    continueOnError = u.continueOnError,
                    strategy = u.strategy,
                    dependOn = u.dependOn
                )
            )
        }

        return jobs
    }

    private fun formatRunsOn(preRunsOn: Any?): RunsOn {
        if (preRunsOn == null) {
            return RunsOn()
        }

        try {
            val runsOn = YamlUtil.getObjectMapper().readValue(JsonUtil.toJson(preRunsOn), RunsOn::class.java)
            return if (runsOn.container != null) {
                try {
                    val container =
                        YamlUtil.getObjectMapper().readValue(JsonUtil.toJson(runsOn.container), Container::class.java)
                    runsOn.copy(container = container)
                } catch (e: Exception) {
                    val container =
                        YamlUtil.getObjectMapper().readValue(JsonUtil.toJson(runsOn.container), Container2::class.java)
                    runsOn.copy(container = container)
                }
            } else {
                runsOn
            }
        } catch (e: Exception) {
            return RunsOn(
                poolName = preRunsOn.toString()
            )
        }
    }

    private fun formatSteps(oldSteps: List<Step>?): List<Step> {
        if (oldSteps == null) {
            return emptyList()
        }

        val stepList = mutableListOf<Step>()
        val stepIdSet = mutableSetOf<String>()
        oldSteps.forEach {
            if (it.uses == null && it.run == null && it.checkout == null) {
                throw YamlFormatException("step必须包含uses或run或checkout!")
            }

            // 校验stepId唯一性
            if (it.id != null && stepIdSet.contains(it.id)) {
                throw YamlFormatException("请确保step.id唯一性!(${it.id})")
            } else if (it.id != null && !stepIdSet.contains(it.id)) {
                stepIdSet.add(it.id)
            }

            // 检测step env合法性
            GitCIEnvUtils.checkEnv(it.env)

            stepList.add(
                Step(
                    name = it.name,
                    id = it.id ?: randomString(stepNamespace),
                    ifFiled = it.ifFiled,
                    uses = it.uses,
                    with = it.with,
                    timeoutMinutes = it.timeoutMinutes,
                    continueOnError = it.continueOnError,
                    retryTimes = it.retryTimes,
                    env = it.env,
                    run = it.run,
                    checkout = it.checkout
                )
            )
        }

        return stepList
    }

    private fun preStages2Stages(preStageList: List<PreStage>?): List<Stage> {
        if (preStageList == null) {
            return emptyList()
        }

        val stageList = mutableListOf<Stage>()
        val stageIdSet = mutableSetOf<String>()
        preStageList.forEach {
            // 校验stageId唯一性
            if (it.id != null && stageIdSet.contains(it.id)) {
                throw YamlFormatException("请确保stage.id唯一性!(${it.id})")
            } else if (it.id != null && !stageIdSet.contains(it.id)) {
                stageIdSet.add(it.id)
            }

            stageList.add(
                Stage(
                    id = it.id ?: randomString(stageNamespace),
                    name = it.name,
                    label = formatStageLabel(it.label),
                    ifField = it.ifField,
                    fastKill = it.fastKill ?: false,
                    jobs = preJobs2Jobs(it.jobs as Map<String, PreJob>),
                    checkIn = formatStageCheck(it.checkIn),
                    checkOut = formatStageCheck(it.checkOut)
                )
            )
        }

        return stageList
    }

    private fun formatStageCheck(preCheck: PreStageCheck?): StageCheck? {
        if (preCheck == null) {
            return null
        }
        return StageCheck(
            reviews = if (preCheck.reviews != null) {
                StageReviews(
                    flows = preCheck.reviews.flows?.map {
                        Flow(
                            name = it.name,
                            reviewers = anyToListString(it.reviewers)
                        )
                    },
                    variables = preCheck.reviews.variables,
                    description = preCheck.reviews.description
                )
            } else {
                null
            },
            gates = preCheck.gates,
            timeoutHours = preCheck.timeoutHours
        )
    }

    private fun formatStageLabel(labels: Any?): List<String> {
        if (labels == null) {
            return emptyList()
        }

        val transLabels = anyToListString(labels)

        val newLabels = mutableListOf<String>()
        transLabels.forEach {
            val stageLabel = getStageLabel(it)
            if (stageLabel != null) {
                newLabels.add(stageLabel.id)
            } else {
                throw YamlFormatException("请核对Stage标签是否正确")
            }
        }

        return newLabels
    }

    private fun getStageLabel(label: String): StageLabel? {
        StageLabel.values().forEach {
            if (it.value == label) {
                return it
            }
        }

        return null
    }

    /**
     * 预处理对象转化为合法对象
     */
    fun normalizeGitCiYaml(preScriptBuildYaml: PreScriptBuildYaml, filePath: String): ScriptBuildYaml {
        val stages = formatStage(
            preScriptBuildYaml
        )

        val thisTriggerOn = formatTriggerOn(preScriptBuildYaml.triggerOn)

        return ScriptBuildYaml(
            name = if (!preScriptBuildYaml.name.isNullOrBlank()) {
                preScriptBuildYaml.name!!
            } else {
                filePath
            },
            version = preScriptBuildYaml.version,
            triggerOn = thisTriggerOn,
            variables = preScriptBuildYaml.variables,
            extends = preScriptBuildYaml.extends,
            resource = preScriptBuildYaml.resources,
            notices = preScriptBuildYaml.notices,
            stages = stages,
            finally = preJobs2Jobs(preScriptBuildYaml.finally),
            label = preScriptBuildYaml.label ?: emptyList()
        )
    }

    fun formatTriggerOn(preTriggerOn: PreTriggerOn?): TriggerOn {

        if (preTriggerOn == null) {
            return TriggerOn(
                push = PushRule(
                    branches = listOf("*")
                ),
                tag = TagRule(
                    tags = listOf("*")
                ),
                // TODO: 暂时使用工蜂的事件，等后续修改为Stream事件
                mr = MrRule(
                    targetBranches = listOf("*"),
                    action = listOf(
                        TGitMergeActionKind.OPEN.value,
                        TGitMergeActionKind.REOPEN.value,
                        TGitMergeExtensionActionKind.PUSH_UPDATE.value
                    )
                )
            )
        }

        var pushRule: PushRule? = null
        var tagRule: TagRule? = null
        var mrRule: MrRule? = null
        var schedulesRule: SchedulesRule? = null

        if (preTriggerOn.push != null) {
            val push = preTriggerOn.push
            try {
                pushRule = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(push),
                    PushRule::class.java
                )
            } catch (e: MismatchedInputException) {
                try {
                    val pushObj = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(push),
                        List::class.java
                    ) as ArrayList<String>

                    pushRule = PushRule(
                        branches = pushObj,
                        branchesIgnore = null,
                        paths = null,
                        pathsIgnore = null,
                        users = null,
                        usersIgnore = null
                    )
                } catch (e: Exception) {
                    logger.error("Format triggerOn pushRule failed.", e)
                }
            }
        }

        if (preTriggerOn.tag != null) {
            val tag = preTriggerOn.tag
            try {
                tagRule = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(tag),
                    TagRule::class.java
                )
            } catch (e: MismatchedInputException) {
                try {
                    val tagList = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(tag),
                        List::class.java
                    ) as ArrayList<String>

                    tagRule = TagRule(
                        tags = tagList,
                        tagsIgnore = null,
                        fromBranches = null,
                        users = null,
                        usersIgnore = null
                    )
                } catch (e: Exception) {
                    logger.error("Format triggerOn tagRule failed.", e)
                }
            }
        }

        if (preTriggerOn.mr != null) {
            val mr = preTriggerOn.mr
            try {
                mrRule = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(mr),
                    MrRule::class.java
                )
            } catch (e: MismatchedInputException) {
                try {
                    val mrList = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(mr),
                        List::class.java
                    ) as ArrayList<String>

                    mrRule = MrRule(
                        targetBranches = mrList,
                        sourceBranchesIgnore = null,
                        paths = null,
                        pathsIgnore = null,
                        users = null,
                        usersIgnore = null
                    )
                } catch (e: Exception) {
                    logger.error("Format triggerOn mrRule failed.", e)
                }
            }
        }

        if (preTriggerOn.schedules != null) {
            val schedules = preTriggerOn.schedules
            try {
                schedulesRule = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(schedules),
                    SchedulesRule::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn schedulesRule failed.", e)
            }
        }

        return TriggerOn(
            push = pushRule,
            tag = tagRule,
            mr = mrRule,
            schedules = schedulesRule
        )
    }

    fun validate(schema: String, yamlJson: String): Pair<Boolean, String> {
        val schemaNode = jsonNodeFromString(schema)
        val jsonNode = jsonNodeFromString(yamlJson)
        val report = JsonSchemaFactory.byDefault().validator.validate(schemaNode, jsonNode)
        val itr = report.iterator()
        val sb = java.lang.StringBuilder()
        while (itr.hasNext()) {
            val message = itr.next() as ProcessingMessage
            if (message.logLevel == LogLevel.ERROR || message.logLevel == LogLevel.FATAL) {
                sb.append(message).append("\r\n")
            }
        }
        return Pair(report.isSuccess, sb.toString())
    }

    fun getPreScriptBuildYamlSchema(): String {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schema = mapper.generateJsonSchema(PreScriptBuildYaml::class.java)
        /*schema.schemaNode.with("properties").with("steps").put("item", CiYamlUtils.getAbstractTaskSchema())
        schema.schemaNode.with("properties").with("services").put("item", CiYamlUtils.getAbstractServiceSchema())
        schema.schemaNode.with("properties")
            .with("stages")
            .with("items")
            .with("properties")
            .with("stage")
            .with("items")
            .with("properties")
            .with("job")
            .with("properties")
            .with("steps")
            .put("item", CiYamlUtils.getAbstractTaskSchema())*/
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    fun jsonNodeFromString(json: String): JsonNode = JsonLoader.fromString(json)

    fun validateSchema(schema: String): Boolean =
        validateJson(schema)

    fun validateJson(json: String): Boolean {
        try {
            jsonNodeFromString(json)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    fun convertYamlToJson(yaml: String): String {
        val yamlReader = ObjectMapper(YAMLFactory())
        val obj = yamlReader.readValue(yaml, Any::class.java)

        val jsonWriter = ObjectMapper()
        return jsonWriter.writeValueAsString(obj)
    }

    fun parseServiceImage(image: String): Pair<String, String> {
        val list = image.split(":")
        if (list.size != 2) {
            throw YamlFormatException("GITCI Service镜像格式非法")
        }
        return Pair(list[0], list[1])
    }

    private fun randomString(flag: String): String {
        val random = Random()
        val buf = StringBuffer(flag)
        for (i in 0 until 7) {
            val num = random.nextInt(secretSeed.length)
            buf.append(secretSeed[num])
        }
        return buf.toString()
    }

    private fun anyToListString(value: Any): List<String> {
        return try {
            YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(value),
                List::class.java
            ) as ArrayList<String>
        } catch (e: MismatchedInputException) {
            listOf(value.toString())
        } catch (e: Exception) {
            logger.error("Format label  failed.", e)
            listOf<String>()
        }
    }

    fun normalizePreCiYaml(preScriptBuildYaml: PreScriptBuildYaml): ScriptBuildYaml {
        val stages = formatStage(
            preScriptBuildYaml
        )

        return ScriptBuildYaml(
            name = preScriptBuildYaml.name,
            version = preScriptBuildYaml.version,
            triggerOn = null,
            variables = preScriptBuildYaml.variables,
            extends = preScriptBuildYaml.extends,
            resource = preScriptBuildYaml.resources,
            notices = preScriptBuildYaml.notices,
            stages = stages,
            finally = preJobs2Jobs(preScriptBuildYaml.finally),
            label = preScriptBuildYaml.label ?: emptyList()
        )
    }
}

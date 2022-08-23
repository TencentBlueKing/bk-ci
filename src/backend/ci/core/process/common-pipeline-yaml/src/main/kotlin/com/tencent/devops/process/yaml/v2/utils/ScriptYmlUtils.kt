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

package com.tencent.devops.process.yaml.v2.utils

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.enums.StreamMrEventAction
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.models.PreRepositoryHook
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.process.yaml.v2.models.YmlName
import com.tencent.devops.process.yaml.v2.models.YmlVersion
import com.tencent.devops.process.yaml.v2.models.add
import com.tencent.devops.process.yaml.v2.models.job.Container
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.job.RunsOn
import com.tencent.devops.process.yaml.v2.models.job.Service
import com.tencent.devops.process.yaml.v2.models.on.DeleteRule
import com.tencent.devops.process.yaml.v2.models.on.EnableType
import com.tencent.devops.process.yaml.v2.models.on.IssueRule
import com.tencent.devops.process.yaml.v2.models.on.MrRule
import com.tencent.devops.process.yaml.v2.models.on.NoteRule
import com.tencent.devops.process.yaml.v2.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v2.models.on.PushRule
import com.tencent.devops.process.yaml.v2.models.on.ReviewRule
import com.tencent.devops.process.yaml.v2.models.on.SchedulesRule
import com.tencent.devops.process.yaml.v2.models.on.TagRule
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v2.models.stage.Stage
import com.tencent.devops.process.yaml.v2.models.stage.StageLabel
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.process.yaml.v2.models.step.Step
import com.tencent.devops.process.yaml.v2.stageCheck.Flow
import com.tencent.devops.process.yaml.v2.stageCheck.PreStageCheck
import com.tencent.devops.process.yaml.v2.stageCheck.StageCheck
import com.tencent.devops.process.yaml.v2.stageCheck.StageReviews
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.BufferedReader
import java.io.StringReader
import java.util.Random
import java.util.regex.Pattern

@Suppress("MaximumLineLength", "ComplexCondition")
object ScriptYmlUtils {

    private val logger = LoggerFactory.getLogger(ScriptYmlUtils::class.java)

    private const val secretSeed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    private const val jobNamespace = "job-"
    private const val stepNamespace = "step-"

    // 用户编写的触发器语法和实际对象不一致
    private const val userTrigger = "on"
    private const val formatTrigger = "triggerOn"

    /**
     * 1、解决锚点
     * 2、yml string层面的格式化填充
     */
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
            logger.warn("Check yaml version failed. return null")
            null
        }
    }

    fun parseName(yamlStr: String?): YmlName? {
        if (yamlStr.isNullOrBlank()) {
            return null
        }

        return try {
            val yaml = Yaml()
            val obj = YamlUtil.toYaml(yaml.load(yamlStr) as Any)
            YamlUtil.getObjectMapper().readValue(obj, YmlName::class.java)
        } catch (e: Exception) {
            logger.warn("get yaml name failed. return null")
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

    fun formatStage(preScriptBuildYaml: PreScriptBuildYaml, transferData: YamlTransferData?): List<Stage> {
        return when {
            preScriptBuildYaml.steps != null -> {
                val jobId = randomString(jobNamespace)
                listOf(
                    Stage(
                        name = "stage_1",
                        jobs = listOf(
                            Job(
                                id = jobId,
                                name = "job1",
                                runsOn = RunsOn(),
                                steps = preStepsToSteps(jobId, preScriptBuildYaml.steps, transferData)
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
                        jobs = preJobs2Jobs(preScriptBuildYaml.jobs, transferData),
                        checkIn = null,
                        checkOut = null
                    )
                )
            }
            else -> {
                preStages2Stages(preScriptBuildYaml.stages as List<PreStage>, transferData)
            }
        }
    }

    fun preJobs2Jobs(preJobs: Map<String, PreJob>?, transferData: YamlTransferData?): List<Job> {
        if (preJobs == null) {
            return emptyList()
        }

        val jobs = mutableListOf<Job>()
        preJobs.forEach { (index, preJob) ->

            // 校验id不能超过64，因为id可能为数字无法在schema支持，放到后台
            if (index.length > 64) {
                throw YamlFormatException("job.id 超过长度限制64 $index")
            }

            // 检测job env合法性
            StreamEnvUtils.checkEnv(preJob.env)

            val services = mutableListOf<Service>()
            preJob.services?.forEach { (key, value) ->
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
                    id = index,
                    name = preJob.name,
                    mutex = preJob.mutex,
                    runsOn = formatRunsOn(preJob.runsOn),
                    services = services,
                    ifField = preJob.ifField,
                    ifModify = preJob.ifModify,
                    steps = preStepsToSteps(index, preJob.steps, transferData),
                    timeoutMinutes = preJob.timeoutMinutes,
                    env = preJob.env,
                    continueOnError = preJob.continueOnError,
                    strategy = preJob.strategy,
                    dependOn = preJob.dependOn
                )
            )

            // 为每个job增加可能的模板信息
            transferData?.add(index, TemplateType.JOB, preJob.yamlMetaData?.templateInfo?.remoteTemplateProjectId)
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
        } catch (e: MismatchedInputException) {
            if (preRunsOn is String || preRunsOn is List<*>) {
                return RunsOn(
                    poolName = preRunsOn.toString()
                )
            }
            throw YamlFormatException(
                "runs-on 中 ${e?.path[0]?.fieldName} 格式有误," +
                    "应为 ${e?.targetType?.name}, error message:${e.message}"
            )
        }
    }

    private fun preStepsToSteps(
        jobId: String,
        oldSteps: List<PreStep>?,
        transferData: YamlTransferData?
    ): List<Step> {
        if (oldSteps == null) {
            return emptyList()
        }

        val stepList = mutableListOf<Step>()
        val stepIdSet = mutableSetOf<String>()
        oldSteps.forEach { preStep ->
            if (preStep.uses == null && preStep.run == null && preStep.checkout == null) {
                throw YamlFormatException("step必须包含uses或run或checkout!")
            }

            // 校验stepId唯一性
            if (!preStep.id.isNullOrBlank() && stepIdSet.contains(preStep.id)) {
                throw YamlFormatException("请确保step.id唯一性!(${preStep.id})")
            } else if (!preStep.id.isNullOrBlank() && !stepIdSet.contains(preStep.id)) {
                stepIdSet.add(preStep.id)
            }

            // 检测step env合法性
            StreamEnvUtils.checkEnv(preStep.env)

            val taskId = "e-${UUIDUtil.generate()}"
            stepList.add(
                Step(
                    name = preStep.name,
                    id = preStep.id,
                    ifFiled = preStep.ifFiled,
                    ifModify = preStep.ifModify,
                    uses = preStep.uses,
                    with = preStep.with,
                    timeoutMinutes = preStep.timeoutMinutes,
                    continueOnError = preStep.continueOnError,
                    retryTimes = preStep.retryTimes,
                    env = preStep.env,
                    run = preStep.run,
                    runAdditionalOptions = mapOf("shell" to preStep.shell),
                    checkout = preStep.checkout,
                    taskId = taskId
                )
            )

            transferData?.add(taskId, TemplateType.STEP, preStep.yamlMetaData?.templateInfo?.remoteTemplateProjectId)
        }

        return stepList
    }

    private fun preStages2Stages(preStageList: List<PreStage>?, transferData: YamlTransferData?): List<Stage> {
        if (preStageList == null) {
            return emptyList()
        }

        val stageList = mutableListOf<Stage>()
        preStageList.forEach {
            stageList.add(
                Stage(
                    name = it.name,
                    label = formatStageLabel(it.label),
                    ifField = it.ifField,
                    ifModify = it.ifModify,
                    fastKill = it.fastKill ?: false,
                    jobs = preJobs2Jobs(it.jobs as Map<String, PreJob>, transferData),
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
    fun normalizeGitCiYaml(
        preScriptBuildYaml: PreScriptBuildYaml,
        filePath: String,
        transferData: YamlTransferData = YamlTransferData()
    ): Pair<ScriptBuildYaml, YamlTransferData> {
        val stages = formatStage(
            preScriptBuildYaml,
            transferData
        )

        val thisTriggerOn = formatTriggerOn(preScriptBuildYaml.triggerOn)

        return Pair(
            ScriptBuildYaml(
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
                finally = preJobs2Jobs(preScriptBuildYaml.finally, transferData),
                label = preScriptBuildYaml.label ?: emptyList(),
                concurrency = preScriptBuildYaml.concurrency
            ),
            transferData
        )
    }

    fun formatRepoHookTriggerOn(preTriggerOn: PreTriggerOn?, name: String?): TriggerOn? {
        if (preTriggerOn?.repoHook == null) {
            logger.info("流水线已不存在远程触发配置，不做处理，返回null进行相关检查")
            return null
        }

        val repositoryHookList = try {
            YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(preTriggerOn.repoHook),
                object : TypeReference<List<PreRepositoryHook>>() {}
            )
        } catch (e: MismatchedInputException) {
            logger.error("Format triggerOn repoHook failed.", e)
            return null
        }
        repositoryHookList.find { it.name == name }?.let { repositoryHook ->
            if (repositoryHook.events == null) {
                return TriggerOn(
                    push = PushRule(
                        branches = listOf("*")
                    ),
                    tag = TagRule(
                        tags = listOf("*")
                    ),
                    mr = MrRule(
                        targetBranches = listOf("*"),
                        action = listOf(
                            StreamMrEventAction.OPEN.value,
                            StreamMrEventAction.REOPEN.value,
                            StreamMrEventAction.PUSH_UPDATE.value
                        )
                    ),
                    repoHook = repoHookRule(repositoryHook)
                )
            }
            val repoPreTriggerOn = try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(repositoryHook.events),
                    PreTriggerOn::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn repoHook events failed.", e)
                return null
            }

            return TriggerOn(
                push = pushRule(repoPreTriggerOn),
                tag = tagRule(repoPreTriggerOn),
                mr = mrRule(repoPreTriggerOn),
                schedules = schedulesRule(repoPreTriggerOn),
                delete = deleteRule(repoPreTriggerOn),
                issue = issueRule(repoPreTriggerOn),
                review = reviewRule(repoPreTriggerOn),
                note = noteRule(repoPreTriggerOn),
                repoHook = repoHookRule(repositoryHook),
                manual = manualRule(repoPreTriggerOn),
                openapi = openapiRule(repoPreTriggerOn)
            )
        }
        logger.warn("repo hook has none effective TriggerOn in ($repositoryHookList)")
        return null
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
                mr = MrRule(
                    targetBranches = listOf("*"),
                    action = listOf(
                        StreamMrEventAction.OPEN.value,
                        StreamMrEventAction.REOPEN.value,
                        StreamMrEventAction.PUSH_UPDATE.value
                    )
                )
            )
        }

        return TriggerOn(
            push = pushRule(preTriggerOn),
            tag = tagRule(preTriggerOn),
            mr = mrRule(preTriggerOn),
            schedules = schedulesRule(preTriggerOn),
            delete = deleteRule(preTriggerOn),
            issue = issueRule(preTriggerOn),
            review = reviewRule(preTriggerOn),
            note = noteRule(preTriggerOn),
            manual = manualRule(preTriggerOn),
            openapi = openapiRule(preTriggerOn)
        )
    }

    fun repoHookRule(
        preRepositoryHook: PreRepositoryHook
    ): RepositoryHook {
        with(preRepositoryHook) {
            when {
                credentials is String -> return RepositoryHook(
                    name = name,
                    credentialsForTicketId = credentials
                )
                credentials is Map<*, *> && credentials["username"] != null && credentials["password"] != null -> {
                    return RepositoryHook(
                        name = name,
                        credentialsForUserName = credentials["username"].toString(),
                        credentialsForPassword = credentials["password"].toString()
                    )
                }
                credentials is Map<*, *> && credentials["token"] != null -> {
                    return RepositoryHook(
                        name = name,
                        credentialsForToken = credentials["token"].toString()
                    )
                }
                else -> return RepositoryHook(
                    name = name
                )
            }
        }
    }

    private fun manualRule(
        preTriggerOn: PreTriggerOn
    ): String? {
        if (preTriggerOn.manual == null) {
            return null
        }

        if (preTriggerOn.manual != EnableType.TRUE.value && preTriggerOn.manual != EnableType.FALSE.value) {
            throw YamlFormatException("not allow manual type ${preTriggerOn.manual}")
        }

        return preTriggerOn.manual
    }

    private fun openapiRule(
        preTriggerOn: PreTriggerOn
    ): String? {
        if (preTriggerOn.openapi == null) {
            return null
        }

        if (preTriggerOn.openapi != EnableType.TRUE.value || preTriggerOn.openapi != EnableType.FALSE.value) {
            throw YamlFormatException("not allow openapi type ${preTriggerOn.openapi}")
        }

        return preTriggerOn.openapi
    }

    private fun noteRule(
        preTriggerOn: PreTriggerOn
    ): NoteRule? {
        if (preTriggerOn.note != null) {
            val note = preTriggerOn.note
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(note),
                    NoteRule::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn noteRule failed.", e)
                null
            }
        }
        return null
    }

    private fun reviewRule(
        preTriggerOn: PreTriggerOn
    ): ReviewRule? {
        if (preTriggerOn.review != null) {
            val issues = preTriggerOn.review
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(issues),
                    ReviewRule::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn reviewRule failed.", e)
                null
            }
        }
        return null
    }

    private fun issueRule(
        preTriggerOn: PreTriggerOn
    ): IssueRule? {
        if (preTriggerOn.issue != null) {
            val issues = preTriggerOn.issue
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(issues),
                    IssueRule::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn issueRule failed.", e)
                null
            }
        }
        return null
    }

    private fun deleteRule(
        preTriggerOn: PreTriggerOn
    ): DeleteRule? {
        if (preTriggerOn.delete != null) {
            val delete = preTriggerOn.delete
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(delete),
                    DeleteRule::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn schedulesRule failed.", e)
                null
            }
        }
        return null
    }

    private fun schedulesRule(
        preTriggerOn: PreTriggerOn
    ): SchedulesRule? {
        if (preTriggerOn.schedules != null) {
            val schedules = preTriggerOn.schedules
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(schedules),
                    SchedulesRule::class.java
                )
            } catch (e: MismatchedInputException) {
                logger.error("Format triggerOn schedulesRule failed.", e)
                null
            }
        }
        return null
    }

    private fun mrRule(
        preTriggerOn: PreTriggerOn
    ): MrRule? {
        if (preTriggerOn.mr != null) {
            val mr = preTriggerOn.mr
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(mr),
                    MrRule::class.java
                )
            } catch (e: MismatchedInputException) {
                try {
                    val mrList = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(mr),
                        List::class.java
                    ) as ArrayList<String>

                    MrRule(
                        targetBranches = mrList,
                        sourceBranchesIgnore = null,
                        paths = null,
                        pathsIgnore = null,
                        users = null,
                        usersIgnore = null
                    )
                } catch (e: Exception) {
                    logger.error("Format triggerOn mrRule failed.", e)
                    null
                }
            }
        }
        return null
    }

    private fun tagRule(
        preTriggerOn: PreTriggerOn
    ): TagRule? {
        if (preTriggerOn.tag != null) {
            val tag = preTriggerOn.tag
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(tag),
                    TagRule::class.java
                )
            } catch (e: MismatchedInputException) {
                try {
                    val tagList = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(tag),
                        List::class.java
                    ) as ArrayList<String>

                    TagRule(
                        tags = tagList,
                        tagsIgnore = null,
                        fromBranches = null,
                        users = null,
                        usersIgnore = null
                    )
                } catch (e: Exception) {
                    logger.error("Format triggerOn tagRule failed.", e)
                    null
                }
            }
        }
        return null
    }

    private fun pushRule(
        preTriggerOn: PreTriggerOn
    ): PushRule? {
        if (preTriggerOn.push != null) {
            val push = preTriggerOn.push
            return try {
                YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(push),
                    PushRule::class.java
                )
            } catch (e: MismatchedInputException) {
                try {
                    val pushObj = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(push),
                        List::class.java
                    ) as ArrayList<String>

                    PushRule(
                        branches = pushObj,
                        branchesIgnore = null,
                        paths = null,
                        pathsIgnore = null,
                        users = null,
                        usersIgnore = null
                    )
                } catch (e: Exception) {
                    logger.error("Format triggerOn pushRule failed.", e)
                    null
                }
            }
        }
        return null
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

    fun jsonNodeFromString(json: String): JsonNode = JsonLoader.fromString(json)

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
            throw YamlFormatException("STREAM Service镜像格式非法")
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

    /**
     * 标准化处理，并裁剪PreCI不支持的特性
     */
    fun normalizePreCiYaml(preScriptBuildYaml: PreScriptBuildYaml): ScriptBuildYaml {
        val stages = formatStage(
            preScriptBuildYaml,
            null
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
            finally = preJobs2Jobs(preScriptBuildYaml.finally, null),
            label = preScriptBuildYaml.label ?: emptyList(),
            concurrency = preScriptBuildYaml.concurrency
        )
    }
}

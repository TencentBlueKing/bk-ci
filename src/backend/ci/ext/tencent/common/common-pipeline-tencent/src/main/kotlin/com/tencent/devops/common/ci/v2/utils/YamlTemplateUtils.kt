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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.Container
import com.tencent.devops.common.ci.v2.Credentials
import com.tencent.devops.common.ci.v2.Extends
import com.tencent.devops.common.ci.v2.PreJob
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.v2.PreStage
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.Service
import com.tencent.devops.common.ci.v2.ServiceWith
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.Strategy
import com.tencent.devops.common.ci.v2.Variable
import com.tencent.devops.common.ci.v2.templates.ParametersTemplateNull
import com.tencent.devops.common.ci.v2.templates.PipelineTemplate
import com.tencent.devops.common.ci.v2.templates.TemplateGraph
import com.tencent.devops.common.ci.v2.templates.TemplateType

class YamlTemplateUtils(
    val yamlObject: PreTemplateScriptBuildYaml,
    val templates: Map<String, String?>,
    val rootPath: String
) {
    // 添加图防止模版的循环嵌套
    var varTemplateGraph = TemplateGraph<String>()
    var stageTemplateGraph = TemplateGraph<String>()
    var jobTemplateGraph = TemplateGraph<String>()
    var stepTemplateGraph = TemplateGraph<String>()

    fun replace(): PreScriptBuildYaml {
        val preYamlObject = with(yamlObject) {
            PreScriptBuildYaml(
                version = version,
                name = name,
                label = label,
                triggerOn = triggerOn,
                onFail = onFail,
                extends = extends,
                resource = resource,
                notices = notices
            )
        }

        if (yamlObject.extends != null) {
            replaceExtends(yamlObject.extends, preYamlObject)
        }
        if (yamlObject.variables != null) {
            replaceVariables(yamlObject.variables, preYamlObject)
        }
        if (yamlObject.stages != null) {
            replaceStages(yamlObject.stages, preYamlObject)
        }
        if (yamlObject.jobs != null) {
            replaceJobs(yamlObject.jobs, preYamlObject)
        }
        if (yamlObject.steps != null) {
            replaceSteps(yamlObject.steps, preYamlObject)
        }

        return preYamlObject
    }

    private fun replaceExtends(
        extend: Extends,
        preYamlObject: PreScriptBuildYaml
    ) {
        val path = extend.template
        val parameters = extend.parameters
        val template = parseTemplateParameters(path, templates[path], parameters)
        val templateObject = YamlUtil.getObjectMapper().readValue(template, PipelineTemplate::class.java)
        with(templateObject) {
            preYamlObject.label = label
            if (variables != null) {
                replaceVariables(JsonUtil.toMap(variables!!), preYamlObject)
            }
            preYamlObject.onFail = onFail
            preYamlObject.extends = extends
            preYamlObject.resource = resource
            preYamlObject.notices = notices
            if (stages != null) {
                replaceStages(stages!!.map { JsonUtil.toMap(it) }, preYamlObject)
            }
            if (jobs != null) {
                replaceJobs(JsonUtil.toMap(jobs!!), preYamlObject)
            }
            if (steps != null) {
                replaceSteps(steps!!.map { JsonUtil.toMap(it) }, preYamlObject)
            }
        }
    }

    private fun replaceVariables(
        variables: Map<String, Any>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val variableMap = mutableMapOf<String, Variable>()
        variables.forEach { (key, value) ->
            val newVariable = replaceVariableTemplate(mapOf(key to value), templates, rootPath)
            if (key == "template") {
                val interSet = newVariable.keys intersect variables.keys
                if (interSet.isNullOrEmpty() || (interSet.size == 1 && interSet.last() == "template")) {
                    variableMap.putAll(newVariable)
                } else {
                    throw RuntimeException(
                        "Variables template's id ${interSet.filter { it != "template" }} Duplicate"
                    )
                }
            } else {
                variableMap.putAll(newVariable)
            }
        }
        preYamlObject.variables = variableMap
    }

    private fun replaceStages(
        stages: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val stageList = mutableListOf<PreStage>()
        stages.forEach { stage ->
            stageList.addAll(replaceStageTemplate(listOf(stage), templates, rootPath))
        }
        preYamlObject.stages = stageList
    }

    private fun replaceJobs(
        jobs: Map<String, Any>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val jobMap = mutableMapOf<String, PreJob>()
        jobs.forEach { (key, value) ->
            // 检查根文件处job_id重复
            val newJob = replaceJobTemplate(mapOf(key to value), templates, rootPath)
            if (key == "template") {
                val interSet = newJob.keys intersect jobs.keys
                if (interSet.isNullOrEmpty() || (interSet.size == 1 && interSet.last() == "template")) {
                    jobMap.putAll(newJob)
                } else {
                    throw RuntimeException(
                        "Job template's id ${interSet.filter { it != "template" }} Duplicate"
                    )
                }
            } else {
                jobMap.putAll(newJob)
            }
        }
        preYamlObject.jobs = jobMap
    }

    private fun replaceSteps(
        steps: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val stepList = mutableListOf<Step>()
        steps.forEach { step ->
            stepList.addAll(replaceStepTemplate(listOf(step), templates, rootPath))
        }
        preYamlObject.steps = stepList
    }

    // 进行模板替换
    private fun replaceVariableTemplate(
        variables: Map<String, Any>,
        templates: Map<String, String?>,
        fromPath: String
    ): Map<String, Variable> {
        val variableMap = mutableMapOf<String, Variable>()
        variables.forEach { (key, value) ->
            if (key == "template") {
                val toPathList = value as List<Map<String, Any>>
                toPathList.forEach { item ->
                    val toPath = item["name"].toString()
                    saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.VARIABLE)

                    val parameters = transNullValue<Map<String, String?>>(
                        key = "parameters",
                        map = item
                    )
                    val template =
                        parseTemplateParameters(
                            path = toPath,
                            template = templates[toPath],
                            parameters = parameters
                        )
                    val templateObject =
                        YamlUtil.getObjectMapper().readValue(template, object : TypeReference<Map<String, Any>>() {})

                    val newVar = replaceVariableTemplate(
                        variables = templateObject["variables"] as Map<String, Any>,
                        templates = templates,
                        fromPath = toPath
                    )
                    // 检测variable是否存在重复的key
                    val interSet = newVar.keys intersect variableMap.keys
                    if (interSet.isNullOrEmpty() || (interSet.size == 1 && interSet.last() == "template")) {
                        variableMap.putAll(newVar)
                    } else {
                        throw RuntimeException(
                            "Variable template's id ${interSet.filter { it != "template" }} Duplicate "
                        )
                    }
                }
            } else {
                variableMap[key] = getVariable((value as Map<String, Any>))
            }
        }
        return variableMap
    }

    private fun replaceStageTemplate(
        stages: List<Map<String, Any>>,
        templates: Map<String, String?>,
        fromPath: String
    ): List<PreStage> {
        val stageList = mutableListOf<PreStage>()
        stages.forEach { stage ->
            if ("template" in stage.keys) {
                val toPath = stage["template"].toString()
                saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.STAGE)

                val parameters = transNullValue<Map<String, String?>>(
                    key = "parameters",
                    map = stage
                )
                val template = parseTemplateParameters(toPath, templates[toPath], parameters)
                val templateObject =
                    YamlUtil.getObjectMapper().readValue(template, object : TypeReference<Map<String, Any>>() {})
                stageList.addAll(
                    replaceStageTemplate(
                        stages = templateObject["stages"] as List<Map<String, Any>>,
                        templates = templates,
                        fromPath = toPath
                    )
                )
            } else {
                stageList.add(getStage(stage, templates))
            }
        }
        return stageList
    }

    private fun replaceJobTemplate(
        jobs: Map<String, Any>,
        templates: Map<String, String?>,
        fromPath: String
    ): Map<String, PreJob> {
        val jobMap = mutableMapOf<String, PreJob>()
        jobs.forEach { (key, value) ->
            if (key == "template") {
                val toPathList = value as List<Map<String, Any>>
                toPathList.forEach { item ->
                    val toPath = item["name"].toString()
                    saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.JOB)

                    val parameters = transNullValue<Map<String, String?>>(
                        key = "parameters",
                        map = item
                    )
                    val template =
                        parseTemplateParameters(
                            path = toPath,
                            template = templates[toPath],
                            parameters = parameters
                        )
                    val templateObject =
                        YamlUtil.getObjectMapper().readValue(template, object : TypeReference<Map<String, Any>>() {})

                    val newJob = replaceJobTemplate(
                        jobs = templateObject["jobs"] as Map<String, Any>,
                        templates = templates,
                        fromPath = toPath
                    )
                    // 检测job是否存在重复的key
                    val interSet = newJob.keys intersect jobMap.keys
                    if (interSet.isNullOrEmpty() || (interSet.size == 1 && interSet.last() == "template")) {
                        jobMap.putAll(newJob)
                    } else {
                        throw RuntimeException(
                            "Job template's id ${interSet.filter { it != "template" }} Duplicate"
                        )
                    }
                }
            } else {
                jobMap[key] = getJob((value as Map<String, Any>), templates)
            }
        }
        return jobMap
    }

    private fun replaceStepTemplate(
        steps: List<Map<String, Any>>,
        templates: Map<String, String?>,
        fromPath: String
    ): List<Step> {
        val stepList = mutableListOf<Step>()
        steps.forEach { step ->
            if ("template" in step.keys) {
                val toPath = step["template"].toString()
                saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.STEP)

                val parameters = transNullValue<Map<String, String?>>(
                    key = "parameters",
                    map = step
                )
                val template = parseTemplateParameters(toPath, templates[toPath], parameters)
                val templateObject =
                    YamlUtil.getObjectMapper().readValue(template, object : TypeReference<Map<String, Any>>() {})

                stepList.addAll(
                    replaceStepTemplate(
                        steps = templateObject["steps"] as List<Map<String, Any>>,
                        templates = templates,
                        fromPath = toPath
                    )
                )
            } else {
                stepList.add(getStep(step))
            }
        }
        return stepList
    }

    // 将路径加入图中并且做循环嵌套检测
    private fun saveAndCheckCyclicTemplate(
        fromPath: String,
        toPath: String,
        templateType: TemplateType
    ) {
        when (templateType) {
            TemplateType.VARIABLE -> {
                varTemplateGraph.addEdge(fromPath, toPath)
                if (varTemplateGraph.hasCyclic()) {
                    throw RuntimeException("yml file : $toPath in $fromPath has variable cricly")
                }
            }
            TemplateType.STAGE -> {
                stageTemplateGraph.addEdge(fromPath, toPath)
                if (stageTemplateGraph.hasCyclic()) {
                    throw RuntimeException("yml file : $toPath in $fromPath has stage cricly")
                }
            }
            TemplateType.JOB -> {
                jobTemplateGraph.addEdge(fromPath, toPath)
                if (jobTemplateGraph.hasCyclic()) {
                    throw RuntimeException("yml file : $toPath in $fromPath has job cricly")
                }
            }
            TemplateType.STEP -> {
                stepTemplateGraph.addEdge(fromPath, toPath)
                if (stepTemplateGraph.hasCyclic()) {
                    throw RuntimeException("yml file : $toPath in $fromPath has step cricly")
                }
            }
            else -> {
            }
        }
    }

    // 为模板中的变量赋值
    private fun parseTemplateParameters(
        path: String,
        template: String?,
        parameters: Map<String, String?>?
    ): String {
        if (template == null) {
            throw RuntimeException("template file: $path not find")
        }
        val newParameters = getTemplateParameters(path, template).parameters?.toMutableList()
        if (!newParameters.isNullOrEmpty()) {
            newParameters.forEachIndexed { index, param ->
                if (parameters != null) {
                    if (parameters.keys.contains(param.name)) {
                        newParameters[index] = param.copy(default = parameters[param.name].toString())
                    }
                }
            }
        } else {
            return template
        }
        val parametersMap = newParameters.associate {
            "parameters.${it.name}" to it.default
        }
        return ScriptYmlUtils.parseVariableValue(template, parametersMap)!!
    }

    // 获取模板中定义的参数
    private fun getTemplateParameters(path: String, template: String): ParametersTemplateNull {
        return try {
            YamlUtil.getObjectMapper().readValue(template, ParametersTemplateNull::class.java)
        } catch (e: Exception) {
            throw RuntimeException("$path wrong format！ ${e.message}")
        }
    }

    // 构造对象
    private fun getVariable(variable: Map<String, Any>): Variable {
        return Variable(
            value = variable["value"]?.toString(),
            readonly = getNullValue("readonly", variable)?.toBoolean()
        )
    }

    private fun getStep(step: Map<String, Any>): Step {
        return Step(
            name = step["name"]?.toString(),
            id = step["id"]?.toString(),
            ifFiled = step["if"]?.toString(),
            uses = step["uses"]?.toString(),
            with = if (step["with"] == null) {
                mapOf()
            } else {
                step["with"] as Map<String, Any>
            },
            timeoutMinutes = getNullValue("timeout-minutes", step)?.toInt(),
            continueOnError = getNullValue("continue-on-error", step)?.toBoolean(),
            retryTimes = step["retry-times"]?.toString(),
            env = step["env"]?.toString(),
            run = step["run"]?.toString()
        )
    }

    private fun getJob(job: Map<String, Any>, templates: Map<String, String?>): PreJob {
        return PreJob(
            name = job["name"]?.toString(),
            runsOn = if (job["runs-on"] == null) {
                null
            } else {
                job["runs-on"] as List<String>
            },
            container = if (job["container"] == null) {
                null
            } else {
                getContainer(job["container"]!!)
            },
            services = if (job["services"] == null) {
                null
            } else {
                getService(job["services"]!!)
            },
            ifField = job["if"]?.toString(),
            steps = if (job["steps"] == null) {
                null
            } else {
                val steps = job["steps"] as List<Map<String, Any>>
                val list = mutableListOf<Step>()
                steps.forEach {
                    list.addAll(replaceStepTemplate(listOf(it), templates, rootPath))
                }
                list
            },
            timeoutMinutes = getNullValue("timeout-minutes", job)?.toInt(),
            env = if (job["env"] == null) {
                emptyMap()
            } else {
                job["env"] as Map<String, String>
            },
            continueOnError = getNullValue("continue-on-error", job)?.toBoolean(),
            strategy = if (job["strategy"] == null) {
                null
            } else {
                getStrategy(job["strategy"]!!)
            },
            dependOn = if (job["depend-on"] == null) {
                null
            } else {
                job["depend-on"] as List<String>
            }
        )
    }

    private fun getStage(stage: Map<String, Any>, templates: Map<String, String?>): PreStage {
        return PreStage(
            name = stage["name"]?.toString(),
            id = stage["id"]?.toString(),
            label = stage["label"]?.toString(),
            ifField = stage["if"]?.toString(),
            fastKill = getNullValue("fast-kill", stage)?.toBoolean(),
            jobs = if (stage["jobs"] == null) {
                null
            } else {
                val jobs = stage["jobs"] as Map<String, Any>
                val map = mutableMapOf<String, PreJob>()
                jobs.forEach { (key, value) ->
                    // 检查根文件处jobId重复
                    val newJob = replaceJobTemplate(mapOf(key to value), templates, rootPath)
                    if (key == "template") {
                        val interSet = newJob.keys intersect jobs.keys
                        if (interSet.isNullOrEmpty() || (interSet.size == 1 && interSet.last() == "template")) {
                            map.putAll(newJob)
                        } else {
                            throw RuntimeException(
                                "Job template's id ${interSet.filter { it != "template" }} Duplicate"
                            )
                        }
                    } else {
                        map.putAll(newJob)
                    }
                }
                map
            }
        )
    }

    private fun getService(service: Any): Map<String, Service> {
        val serviceMap = service as Map<String, Any?>
        val newServiceMap = mutableMapOf<String, Service>()
        serviceMap.forEach { key, value ->
            val with = (value as Map<String, Any>)["with"] as Map<String, Any>
            newServiceMap.putAll(
                mapOf(
                    key to Service(
                        image = getNotNullValue(key = "image", mapName = "Container", map = value),
                        with = ServiceWith(
                            password = getNotNullValue(key = "password", mapName = "with", map = with)
                        )
                    )
                )
            )
        }
        return newServiceMap
    }

    private fun getContainer(container: Any): Container {
        val containerMap = container as Map<String, Any?>
        return Container(
            image = getNotNullValue(key = "image", mapName = "Container", map = containerMap),
            credentials = if (containerMap["credentials"] == null) {
                null
            } else {
                val credentialsMap = containerMap["credentials"] as Map<String, String>
                Credentials(
                    username = credentialsMap["username"]!!,
                    password = credentialsMap["password"]!!
                )
            }
        )
    }

    private fun getStrategy(strategy: Any?): Strategy {
        val strategyMap = strategy as Map<String, Any?>
        return Strategy(
            matrix = strategyMap["matrix"],
            fastKill = getNullValue("fast-kill", strategyMap)?.toBoolean(),
            maxParallel = getNullValue("max-parallel", strategyMap)
        )
    }

    private fun <T> transNullValue(key: String, map: Map<String, Any?>): T? {
        return if (map[key] == null) {
            null
        } else {
            map[key] as T
        }
    }

    private fun getNullValue(key: String, map: Map<String, Any?>): String? {
        return if (map[key] == null) {
            null
        } else {
            map[key].toString()
        }
    }

    private fun getNotNullValue(key: String, mapName: String, map: Map<String, Any?>): String {
        return if (map[key] == null) {
            throw RuntimeException("$mapName need $key")
        } else {
            map[key].toString()
        }
    }
}

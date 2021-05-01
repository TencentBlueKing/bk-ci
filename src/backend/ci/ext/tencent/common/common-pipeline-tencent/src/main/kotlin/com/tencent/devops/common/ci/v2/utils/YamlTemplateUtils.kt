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

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.Container
import com.tencent.devops.common.ci.v2.Credentials
import com.tencent.devops.common.ci.v2.PreJob
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.v2.PreStage
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.Service
import com.tencent.devops.common.ci.v2.ServiceWith
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.Strategy
import com.tencent.devops.common.ci.v2.Variable
import com.tencent.devops.common.ci.v2.templates.JobsTemplate
import com.tencent.devops.common.ci.v2.templates.Parameters
import com.tencent.devops.common.ci.v2.templates.PipelineTemplate
import com.tencent.devops.common.ci.v2.templates.StagesTemplate
import com.tencent.devops.common.ci.v2.templates.StepsTemplate
import com.tencent.devops.common.ci.v2.templates.TemplateType
import com.tencent.devops.common.ci.v2.templates.VariablesTemplate

class YamlTemplateUtils(
    val yamlObject: PreTemplateScriptBuildYaml,
    val templates: Map<String, String?>
) {

    fun replaceTemplate(): PreScriptBuildYaml {
        return replaceStageTemplate()
    }

    private fun replaceStageTemplate(): PreScriptBuildYaml {
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
            val path = yamlObject.extends.template
            val parameters = yamlObject.extends.parameters
            val template = parseTemplateParameters(path, templates[path], parameters, TemplateType.PIPELINE)
            val templateObject = YamlUtil.getObjectMapper().readValue(template, PipelineTemplate::class.java)
            with(templateObject) {
                preYamlObject.label = label
                preYamlObject.variables = variables
                preYamlObject.onFail = onFail
                preYamlObject.extends = extends
                preYamlObject.resource = resource
                preYamlObject.notices = notices
                preYamlObject.stages = stages
                preYamlObject.jobs = jobs
                preYamlObject.steps = steps
            }
        }
        if (yamlObject.variables != null) {
            val variableMap = mutableMapOf<String, Variable>()
            yamlObject.variables.forEach { variable ->
                val newVariable = replaceVariableTemplate(variable, templates)
                if (variable.key == "template" && newVariable.keys.contains(variable.key)) {
                    throw RuntimeException("Job template's id ${variable.key} Duplicate with Job id")
                } else {
                    variableMap.putAll(newVariable)
                }
            }
            preYamlObject.variables = variableMap
        }
        if (yamlObject.steps != null) {
            val stepList = mutableListOf<Step>()
            yamlObject.steps.forEach { step ->
                stepList.addAll(replaceStepTemplate(step, templates))
            }
            preYamlObject.steps = stepList
        }
        if (yamlObject.jobs != null) {
            val jobMap = mutableMapOf<String, PreJob>()
            yamlObject.jobs.forEach { job ->
                val newJob = replaceJobTemplate(job, templates)
                if (job.key == "template" && newJob.keys.contains(job.key)) {
                    throw RuntimeException("Job template's id ${job.key} Duplicate with Job id")
                } else {
                    jobMap.putAll(newJob)
                }
            }
            preYamlObject.jobs = jobMap
        }
        if (yamlObject.stages != null) {
            val stageList = mutableListOf<PreStage>()
            yamlObject.stages.forEach { stage ->
                stageList.addAll(replaceStageTemplate(stage, templates))
            }
            preYamlObject.stages = stageList
        }

        return preYamlObject
    }

    private fun replaceVariableTemplate(
        variable: Map.Entry<String, Any>,
        templates: Map<String, String?>
    ): Map<String, Variable> {
        var isString = false
        return if (variable.key == "template") {
            val path = try {
                (variable.value as Map<String, Any>)["name"]
            } catch (e: Exception) {
                isString = true
                variable.value.toString()
            }
            val parameters = if (isString) {
                null
            } else {
                transNullValue<Map<String, String?>>(
                    key = "parameters",
                    map = variable.value as Map<String, Any>
                )
            }
            val template =
                parseTemplateParameters(
                    path = path.toString(),
                    template = templates[path],
                    parameters = parameters,
                    templateType = TemplateType.VARIABLE
                )
            val templateObject = YamlUtil.getObjectMapper().readValue(template, VariablesTemplate::class.java)
            templateObject.variables
        } else {
            mapOf(
                variable.key to getVariable((variable.value as Map<String, Any>))
            )
        }
    }

    private fun replaceStageTemplate(
        stage: Map<String, Any>,
        templates: Map<String, String?>
    ): List<PreStage> {
        return if ("template" in stage.keys) {
            val path = stage["template"].toString()
            val parameters = transNullValue<Map<String, String?>>(
                key = "parameters",
                map = stage
            )
            val template = parseTemplateParameters(path, templates[path], parameters, TemplateType.STAGE)
            val templateObject = YamlUtil.getObjectMapper().readValue(template, StagesTemplate::class.java)
            templateObject.stages
        } else {
            listOf(
                getStage(stage, templates)
            )
        }
    }

    private fun replaceJobTemplate(
        job: Map.Entry<String, Any>,
        templates: Map<String, String?>
    ): Map<String, PreJob> {
        var isString = false
        return if (job.key == "template") {
            val path = try {
                (job.value as Map<String, Any>)["name"]
            } catch (e: Exception) {
                isString = true
                job.value.toString()
            }
            val parameters = if (isString) {
                null
            } else {
                transNullValue<Map<String, String?>>(
                    key = "parameters",
                    map = job.value as Map<String, Any>
                )
            }
            val template =
                parseTemplateParameters(
                    path = path.toString(),
                    template = templates[path],
                    parameters = parameters,
                    templateType = TemplateType.JOB
                )
            val templateObject = YamlUtil.getObjectMapper().readValue(template, JobsTemplate::class.java)
            templateObject.jobs
        } else {
            mapOf(
                job.key to getJob((job.value as Map<String, Any>), templates)
            )
        }
    }

    private fun replaceStepTemplate(
        step: Map<String, Any>,
        templates: Map<String, String?>
    ): List<Step> {
        return if ("template" in step.keys) {
            val path = step["template"].toString()
            val parameters = transNullValue<Map<String, String?>>(
                key = "parameters",
                map = step
            )
            val template = parseTemplateParameters(path, templates[path], parameters, TemplateType.STEP)
            val templateObject = YamlUtil.getObjectMapper().readValue(template, StepsTemplate::class.java)
            templateObject.steps
        } else {
            listOf(
                getStep(step)
            )
        }
    }

    // 为模板中的变量赋值
    private fun parseTemplateParameters(
        path: String,
        template: String?,
        parameters: Map<String, String?>?,
        templateType: TemplateType
    ): String {
        if (template == null) {
            throw RuntimeException("template file: $path not find")
        }
        var newParameters: MutableList<Parameters>? = null
        when (templateType) {
            TemplateType.PIPELINE -> {
                newParameters = getPipelineTemplate(path, template).parameters?.toMutableList()
            }
            TemplateType.VARIABLE -> {
            }
            TemplateType.PARAMETER -> {
            }
            TemplateType.STAGE -> {
                newParameters = getStageTemplate(path, template).parameters?.toMutableList()
            }
            TemplateType.JOB -> {
                newParameters = getJobTemplate(path, template).parameters?.toMutableList()
            }
            TemplateType.STEP -> {
                newParameters = getStepTemplate(path, template).parameters?.toMutableList()
            }
        }
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

    private fun getPipelineTemplate(path: String, template: String): PipelineTemplate {
        return try {
            YamlUtil.getObjectMapper().readValue(template, PipelineTemplate::class.java)
        } catch (e: Exception) {
            throw RuntimeException("$path wrong format！ ${e.message}")
        }
    }

    private fun getStageTemplate(path: String, template: String): StagesTemplate {
        return try {
            YamlUtil.getObjectMapper().readValue(template, StagesTemplate::class.java)
        } catch (e: Exception) {
            throw RuntimeException("$path wrong format！")
        }
    }

    private fun getJobTemplate(path: String, template: String): JobsTemplate {
        return try {
            YamlUtil.getObjectMapper().readValue(template, JobsTemplate::class.java)
        } catch (e: Exception) {
            throw RuntimeException("$path wrong format！")
        }
    }

    private fun getStepTemplate(path: String, template: String): StepsTemplate {
        return try {
            YamlUtil.getObjectMapper().readValue(template, StepsTemplate::class.java)
        } catch (e: Exception) {
            throw RuntimeException("$path wrong format！")
        }
    }

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
            timeoutMinutes = getNullValue("timeoutMinutes", step)?.toInt(),
            continueOnError = getNullValue("with", step)?.toBoolean(),
            retryTimes = step["retryTimes"]?.toString(),
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
                    list.addAll(replaceStepTemplate(it, templates))
                }
                list
            },
            timeoutMinutes = getNullValue("timeoutMinutes", job)?.toInt(),
            env = if (job["env"] == null) {
                emptyMap()
            } else {
                job["env"] as Map<String, String>
            },
            continueOnError = getNullValue("continueOnError", job)?.toBoolean(),
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
            fastKill = getNullValue("fastKill", stage)?.toBoolean(),
            jobs = if (stage["jobs"] == null) {
                null
            } else {
                val jobs = stage["jobs"] as Map<String, Any>
                val map = mutableMapOf<String, PreJob>()
                jobs.forEach { job ->
                    val newJob = replaceJobTemplate(job, templates)
                    if (job.key == "template" && newJob.keys.contains(job.key)) {
                        throw RuntimeException("Job template's id ${job.key} Duplicate with Job id")
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
            fastKill = getNullValue("fastKill", strategyMap)?.toBoolean(),
            maxParallel = getNullValue("maxParallel", strategyMap)
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

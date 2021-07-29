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

package com.tencent.devops.gitci.trigger.template

import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.Extends
import com.tencent.devops.common.ci.v2.PreJob
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.v2.PreStage
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.Repositories
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.Variable
import com.tencent.devops.gitci.trigger.template.pojo.ParametersTemplateNull
import com.tencent.devops.gitci.trigger.template.pojo.TemplateGraph
import com.tencent.devops.gitci.trigger.template.pojo.enums.TemplateType
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.gitci.trigger.template.pojo.NoReplaceTemplate
import org.slf4j.LoggerFactory

@Suppress("ALL")
class YamlTemplate(
    // 发起者的库ID,用户名,分支
    val triggerProjectId: Long,
    // sourceProjectId，在fork时是源库的ID
    val sourceProjectId: Long,
    val triggerUserId: String,
    val triggerRef: String,
    val triggerToken: String,

    // 添加图防止远程库之间循环依赖
    val repoTemplateGraph: TemplateGraph<String>,

    // 来自文件
    val fileFromPath: String? = null,
    // 当前文件
    var filePath: String,
    // 文件对象
    var yamlObject: PreTemplateScriptBuildYaml?,
    // 当前库信息(发起库没有库信息)
    val repo: Repositories?,
    // 远程模板类型(用来校验远程打平的模板的格式)
    val resTemplateType: TemplateType? = null,

    // 每个文件使用的模板个数（不能超过10）
    val templateNumb: MutableMap<String, Int> = mutableMapOf(),
    // 嵌套的总模板深度（不能超过5）
    var templateDeep: Int = 0,

    // 获取模板文件函数，将模板替换过程与获取文件解耦，方便测试或链接其他代码库
    val getTemplateMethod: (
        token: String?,
        gitProjectId: Long,
        targetRepo: String?,
        ref: String,
        personalAccessToken: String?,
        fileName: String
    ) -> String

) {
    companion object {
        private val logger = LoggerFactory.getLogger(YamlTemplate::class.java)
        private const val templateDirectory = ".ci/templates/"

        // 引用模板的关键字
        private const val TEMPLATE_KEY = "template"

        //  模板变量关键字
        private const val PARAMETERS_KEY = "parameters"

        // 对象类型模板Job,Variable的模板路径关键字
        private const val OBJECT_TEMPLATE_PATH = "name"

        // 分隔远程库和文件关键字
        private const val FILE_REPO_SPLIT = "@"

        // 模板最多引用数和最大深度
        private const val MAX_TEMPLATE_NUMB = 10
        private const val MAX_TEMPLATE_DEEP = 5

        // 异常模板
        const val TEMPLATE_ID_DUPLICATE = "Format error: ID [%s] in template [%s] and template [%s] are duplicated"
        const val TEMPLATE_ROOT_ID_DUPLICATE = "[%s] Format error: IDs [%s] are duplicated"
        const val TRANS_AS_ERROR = "[%s]Keyword [%s] format error"
        const val REPO_NOT_FOUND_ERROR =
            "[%s]The referenced repository [%s] should first be declared by the resources keyword"
        const val REPO_CYCLE_ERROR = "Repository: Cyclic dependency"
        const val TEMPLATE_CYCLE_ERROR = "There is a [%s] circular dependency in template [%s] and template [%s]"
        const val TEMPLATE_NUMB_BEYOND =
            "[%s]The number of referenced template files exceeds the threshold [$MAX_TEMPLATE_NUMB] "
        const val TEMPLATE_DEEP_BEYOND = "[%s]The template nesting depth exceeds the threshold [$MAX_TEMPLATE_DEEP]"
        const val TEMPLATE_FORMAT_ERROR = "[%s]Template YAML does not meet the specification"
        const val YAML_FORMAT_ERROR = "[%s] Format error: %s"
        const val ATTR_MISSING_ERROR = "[%s]Required attributes [%s] are missing"
        const val TEMPLATE_KEYWORDS_ERROR = "[%s]Template YAML does not meet the specification. " +
            "The %s template can only contain parameters, resources and %s keywords"
        const val EXTENDS_TEMPLATE_EXTENDS_ERROR = "[%s]The extends keyword cannot be nested"
        const val EXTENDS_TEMPLATE_ON_ERROR = "[%s]Triggers are not supported in the template"
        const val VALUE_NOT_IN_ENUM = "[%s][%s=%s]Parameter error, the expected value is [%s]"
        const val FINALLY_FORMAT_ERROR = "final stage not support stage's template"
    }

    // 存储当前库的模板信息，减少重复获取 key: templatePath value： template
    var templates = mutableMapOf<String, String>()

    // 添加图防止模版的循环嵌套
    var varTemplateGraph = TemplateGraph<String>()
    var stageTemplateGraph = TemplateGraph<String>()
    var jobTemplateGraph = TemplateGraph<String>()
    var stepTemplateGraph = TemplateGraph<String>()

    fun replace(
        parameters: Map<String, Any?>? = null
    ): PreScriptBuildYaml {
        // 针对远程库进行打平替换时，根文件没有被替换Parameters
        val newYamlObject = if (repo != null) {
            val template = parseTemplateParameters(
                fromPath = fileFromPath ?: "",
                path = filePath,
                template = getTemplate(filePath),
                parameters = parameters
            )
            // 将根文件也保存在模板库中方便取出
            setTemplate(filePath, template)
            YamlObjects.getObjectFromYaml<PreTemplateScriptBuildYaml>(filePath, template)
        } else {
            setTemplate(filePath, YamlUtil.toYaml(yamlObject!!))
            yamlObject
        }

        // 针对远程库打平替换时格式无法被校验到
        if (resTemplateType != null) {
            YamlObjects.checkTemplate(filePath, getTemplate(filePath), resTemplateType)
        }

        val preYamlObject = with(newYamlObject!!) {
            PreScriptBuildYaml(
                version = version,
                name = name,
                label = label,
                triggerOn = triggerOn,
                resources = resources,
                notices = notices
            )
        }

        if (newYamlObject.extends != null) {
            replaceExtends(newYamlObject.extends!!, preYamlObject)
        }
        if (newYamlObject.variables != null) {
            replaceVariables(newYamlObject.variables!!, preYamlObject)
        }
        if (newYamlObject.stages != null) {
            replaceStages(newYamlObject.stages!!, preYamlObject)
        }
        if (newYamlObject.jobs != null) {
            replaceJobs(newYamlObject.jobs!!, preYamlObject)
        }
        if (newYamlObject.steps != null) {
            replaceSteps(newYamlObject.steps!!, preYamlObject)
        }
        if (newYamlObject.finally != null) {
            replaceFinally(newYamlObject.finally!!, preYamlObject)
        }

        return preYamlObject
    }

    private fun replaceExtends(
        extend: Extends,
        preYamlObject: PreScriptBuildYaml
    ) {
        // extend引用深度增加
        addAndCheckTemplateDeep()
        val toPath = extend.template
        val parameters = extend.parameters
        // 根据远程模板获取
        val templateObject = replaceResAndParam(TemplateType.EXTEND, toPath, parameters, filePath)
        // 获取extends模板后filePath就为被替换的文件了
        this.filePath = toPath
        // 需要替换模板的的递归替换
        if (templateObject[TemplateType.VARIABLE.content] != null) {
            replaceVariables(
                transValue(filePath, TemplateType.VARIABLE.text, templateObject[TemplateType.VARIABLE.content]),
                preYamlObject
            )
        }
        if (templateObject[TemplateType.STAGE.content] != null) {
            replaceStages(
                transValue(filePath, TemplateType.STAGE.text, templateObject[TemplateType.STAGE.content]),
                preYamlObject
            )
        }
        if (templateObject[TemplateType.JOB.content] != null) {
            replaceJobs(
                transValue(filePath, TemplateType.JOB.text, templateObject[TemplateType.JOB.content]),
                preYamlObject
            )
        }
        if (templateObject[TemplateType.STEP.content] != null) {
            replaceSteps(
                transValue(filePath, TemplateType.STEP.text, templateObject[TemplateType.STEP.content]),
                preYamlObject
            )
        }
        if (templateObject[TemplateType.FINALLY.content] != null) {
            replaceFinally(
                transValue(filePath, TemplateType.FINALLY.text, templateObject[TemplateType.FINALLY.content]),
                preYamlObject
            )
        }
        // 将不用替换的直接传入
        val newYaml = YamlObjects.getObjectFromYaml<NoReplaceTemplate>(toPath, YamlUtil.toYaml(templateObject))
        preYamlObject.label = newYaml.label
        preYamlObject.resources = newYaml.resources
        preYamlObject.notices = newYaml.notices
        // 用户没写就用模板的名字
        if (preYamlObject.name.isNullOrBlank()) {
            preYamlObject.name = newYaml.name
        }
    }

    private fun replaceVariables(
        variables: Map<String, Any>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val variableMap = mutableMapOf<String, Variable>()
        variables.forEach { (key, value) ->
            val newVariable = replaceVariableTemplate(mapOf(key to value), filePath)
            if (key == TEMPLATE_KEY) {
                // 通过取交集判断除template关键字之外的ID是否重复
                checkDuplicateKey(filePath = filePath, keys = variables.keys, newKeys = newVariable.keys)
            }
            variableMap.putAll(newVariable)

            // 每个参数独立计算模板深度
            refreshTemplateDeep()
        }
        preYamlObject.variables = variableMap
    }

    private fun replaceStages(
        stages: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val stageList = mutableListOf<PreStage>()
        stages.forEach { stage ->
            stageList.addAll(replaceStageTemplate(listOf(stage), filePath))
            // 每个参数独立计算模板深度
            refreshTemplateDeep()
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
            val newJob = replaceJobTemplate(mapOf(key to value), filePath)
            if (key == TEMPLATE_KEY) {
                checkDuplicateKey(filePath = filePath, keys = jobs.keys, newKeys = newJob.keys)
            }
            jobMap.putAll(newJob)
            // 每个参数独立计算模板深度
            refreshTemplateDeep()
        }
        preYamlObject.jobs = jobMap
    }

    private fun replaceSteps(
        steps: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYaml
    ) {
        val stepList = mutableListOf<Step>()
        steps.forEach { step ->
            stepList.addAll(replaceStepTemplate(listOf(step), filePath))
            // 每个参数独立计算模板深度
            refreshTemplateDeep()
        }
        preYamlObject.steps = stepList
    }

    private fun replaceFinally(
        finally: Map<String, Any>,
        preYamlObject: PreScriptBuildYaml
    ) {
        // finally: 与jobs: 的结构相同
        val finallyMap = mutableMapOf<String, PreJob>()
        finally.forEach { (key, value) ->
            // 检查根文件处job_id重复
            val newFinally = replaceJobTemplate(mapOf(key to value), filePath)
            if (key == TEMPLATE_KEY) {
                checkDuplicateKey(filePath = filePath, keys = finally.keys, newKeys = newFinally.keys)
            }
            finallyMap.putAll(newFinally)
            // 每个参数独立计算模板深度
            refreshTemplateDeep()
        }
        preYamlObject.finally = finallyMap
    }

    // 检查是否具有重复的ID，job，variable中使用
    private fun checkDuplicateKey(
        filePath: String,
        keys: Set<String>,
        newKeys: Set<String>,
        toPath: String? = null
    ): Boolean {
        val interSet = newKeys intersect keys
        if (interSet.isNullOrEmpty() || (interSet.size == 1 && interSet.last() == TEMPLATE_KEY)) {
            return true
        } else {
            if (toPath == null) {
                error(
                    TEMPLATE_ROOT_ID_DUPLICATE.format(
                        filePath,
                        interSet.filter { it != TEMPLATE_KEY }
                    )
                )
            } else {
                error(
                    TEMPLATE_ID_DUPLICATE.format(
                        interSet.filter { it != TEMPLATE_KEY },
                        filePath,
                        toPath
                    )
                )
            }
            return false
        }
    }

    // 进行模板替换
    private fun replaceVariableTemplate(
        variables: Map<String, Any>,
        fromPath: String
    ): Map<String, Variable> {
        val variableMap = mutableMapOf<String, Variable>()
        variables.forEach { (key, value) ->
            // 如果是模板文件则进行模板替换
            // 每一层只进行一次深度统计
            val deepFlag = false
            if (key == TEMPLATE_KEY) {
                if (!deepFlag) {
                    addAndCheckTemplateDeep()
                }
                // 每个文件做数量统计
                addAndCheckTemplateNumb(fromPath)
                val toPathList = transValue<List<Map<String, Any>>>(fromPath, TemplateType.VARIABLE.text, value)
                toPathList.forEach { item ->
                    val toPath = item[OBJECT_TEMPLATE_PATH].toString()
                    // 保存并检查是否存在循环引用模板
                    saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.VARIABLE)
                    // 获取需要替换的变量
                    val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                        file = fromPath,
                        type = PARAMETERS_KEY,
                        key = PARAMETERS_KEY,
                        map = item
                    )
                    // 对模板文件进行远程库和参数替换，并实例化
                    val templateObject = replaceResAndParam(TemplateType.VARIABLE, toPath, parameters, fromPath)
                    // 判断实例化后的模板文件中是否引用了模板文件，如果有，则递归替换
                    val newVar = replaceVariableTemplate(
                        variables = transValue(
                            toPath, TemplateType.VARIABLE.text, templateObject[TemplateType.VARIABLE.content]
                        ),
                        fromPath = toPath
                    )
                    // 检测variable是否存在重复的key
                    checkDuplicateKey(
                        filePath = filePath,
                        keys = variableMap.keys,
                        newKeys = newVar.keys,
                        toPath = toPath
                    )
                    variableMap.putAll(newVar)
                }
            } else {
                // 不是模板文件则直接实例化
                if (value !is Map<*, *>) {
                    variableMap[key] = Variable(value.toString(), false)
                } else {
                    variableMap[key] = YamlObjects.getVariable(transValue(fromPath, TemplateType.VARIABLE.text, value))
                }
            }
        }
        return variableMap
    }

    private fun replaceStageTemplate(
        stages: List<Map<String, Any>>,
        fromPath: String
    ): List<PreStage> {
        val stageList = mutableListOf<PreStage>()
        stages.forEach { stage ->
            // 每一层只进行一次深度统计
            val deepFlag = false
            if (TEMPLATE_KEY in stage.keys) {
                if (!deepFlag) {
                    addAndCheckTemplateDeep()
                }
                // 每个文件做数量统计
                addAndCheckTemplateNumb(fromPath)
                val toPath = stage[TEMPLATE_KEY].toString()
                saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.STAGE)

                val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                    file = fromPath,
                    type = PARAMETERS_KEY,
                    key = PARAMETERS_KEY,
                    map = stage
                )
                // 对模板文件进行远程库和参数替换，并实例化
                val templateObject = replaceResAndParam(TemplateType.STAGE, toPath, parameters, fromPath)
                stageList.addAll(
                    replaceStageTemplate(
                        stages = transValue(
                            toPath,
                            TemplateType.STAGE.text,
                            templateObject[TemplateType.STAGE.content]
                        ),
                        fromPath = toPath
                    )
                )
            } else {
                stageList.add(getStage(fromPath, stage))
            }
        }
        return stageList
    }

    private fun replaceJobTemplate(
        jobs: Map<String, Any>,
        fromPath: String
    ): Map<String, PreJob> {
        val jobMap = mutableMapOf<String, PreJob>()
        jobs.forEach { (key, value) ->
            // 每一层只进行一次深度统计
            val deepFlag = false
            if (key == TEMPLATE_KEY) {
                if (!deepFlag) {
                    addAndCheckTemplateDeep()
                }
                // 每个文件做数量统计
                addAndCheckTemplateNumb(fromPath)
                val toPathList = transValue<List<Map<String, Any>>>(fromPath, TemplateType.JOB.text, value)
                toPathList.forEach { item ->
                    val toPath = item[OBJECT_TEMPLATE_PATH].toString()
                    saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.JOB)

                    val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                        file = fromPath,
                        type = PARAMETERS_KEY,
                        key = PARAMETERS_KEY,
                        map = item
                    )

                    // 对模板文件进行远程库和参数替换，并实例化
                    val templateObject = replaceResAndParam(TemplateType.JOB, toPath, parameters, fromPath)

                    val newJob = replaceJobTemplate(
                        jobs = transValue(
                            toPath, TemplateType.JOB.text, templateObject[TemplateType.JOB.content]
                        ),
                        fromPath = toPath
                    )
                    // 检测job是否存在重复的key
                    checkDuplicateKey(
                        filePath = filePath,
                        keys = jobMap.keys,
                        newKeys = newJob.keys,
                        toPath = toPath
                    )
                    jobMap.putAll(newJob)
                }
            } else {
                jobMap[key] = getJob(fromPath, transValue(fromPath, TemplateType.JOB.text, value))
            }
        }
        return jobMap
    }

    private fun replaceStepTemplate(
        steps: List<Map<String, Any>>,
        fromPath: String
    ): List<Step> {
        val stepList = mutableListOf<Step>()
        steps.forEach { step ->
            // 每一层只进行一次深度统计
            val deepFlag = false
            if (TEMPLATE_KEY in step.keys) {
                if (!deepFlag) {
                    addAndCheckTemplateDeep()
                }
                // 每个文件做数量统计
                addAndCheckTemplateNumb(fromPath)
                val toPath = step[TEMPLATE_KEY].toString()
                saveAndCheckCyclicTemplate(fromPath, toPath, TemplateType.STEP)

                val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                    file = fromPath,
                    type = PARAMETERS_KEY,
                    key = PARAMETERS_KEY,
                    map = step
                )

                // 对模板文件进行远程库和参数替换，并实例化
                val templateObject = replaceResAndParam(TemplateType.STEP, toPath, parameters, fromPath)

                stepList.addAll(
                    replaceStepTemplate(
                        steps = transValue(
                            toPath,
                            TemplateType.STEP.text,
                            templateObject[TemplateType.STEP.content]
                        ),
                        fromPath = toPath
                    )
                )
            } else {
                stepList.add(YamlObjects.getStep(fromPath, step))
            }
        }
        return stepList
    }

    // 替换远程库和参数信息
    private fun replaceResAndParam(
        templateType: TemplateType,
        toPath: String,
        parameters: Map<String, Any?>?,
        fromPath: String
    ): Map<String, Any> {
        // 判断是否为远程库，如果是远程库将其远程库文件打平进行替换
        var newTemplate = if (toPath.contains(FILE_REPO_SPLIT)) {
            replaceResTemplateFile(
                templateType = templateType,
                toPath = toPath,
                parameters = parameters,
                toRepo = checkAndGetRepo(
                    fromPath,
                    toPath.split(FILE_REPO_SPLIT)[1]
                )
            )
        } else {
            getTemplate(toPath)
        }
        // 检查模板格式
        YamlObjects.checkTemplate(toPath, newTemplate, templateType)
        // 将需要替换的变量填入模板文件
        newTemplate = parseTemplateParameters(
            fromPath = fromPath,
            path = toPath,
            template = newTemplate,
            parameters = parameters
        )
        // 将模板文件实例化
        return YamlObjects.getObjectFromYaml(toPath, newTemplate)
    }

    // 校验当前模板的远程库信息，每个文件只可以使用当前文件下引用的远程库
    private fun checkAndGetRepo(fromPath: String, repoName: String): Repositories {
        val repos =
            YamlObjects.getObjectFromYaml<NoReplaceTemplate>(fromPath, getTemplate(fromPath)).resources?.repositories
        repos?.forEach {
            if (it.name == repoName) {
                return it
            }
        }
        throw RuntimeException(REPO_NOT_FOUND_ERROR.format(fromPath, repoName))
    }

    // 对远程仓库中的模板进行远程仓库替换
    private fun replaceResTemplateFile(
        templateType: TemplateType,
        toPath: String,
        parameters: Map<String, Any?>?,
        toRepo: Repositories
    ): String {
        // 判断是否有库之间的循环依赖
        repoTemplateGraph.addEdge(repo?.name ?: filePath, toRepo.name)
        if (repoTemplateGraph.hasCyclic()) {
            error(REPO_CYCLE_ERROR)
        }

        val resYamlObject = YamlTemplate(
            yamlObject = null,
            fileFromPath = filePath,
            filePath = toPath.split(FILE_REPO_SPLIT)[0],
            sourceProjectId = sourceProjectId,
            triggerProjectId = triggerProjectId,
            triggerUserId = triggerUserId,
            triggerRef = triggerRef,
            triggerToken = triggerToken,
            repo = toRepo,
            repoTemplateGraph = repoTemplateGraph,
            templateDeep = templateDeep,
            resTemplateType = templateType,
            getTemplateMethod = getTemplateMethod
        ).replace(parameters = parameters)
        // 替换后的远程模板去除不必要参数
        resYamlObject.resources = null
        return YamlUtil.toYaml(resYamlObject)
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
                    error(TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.VARIABLE.text))
                }
            }
            TemplateType.STAGE -> {
                stageTemplateGraph.addEdge(fromPath, toPath)
                if (stageTemplateGraph.hasCyclic()) {
                    error(TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.STAGE.text))
                }
            }
            TemplateType.JOB -> {
                jobTemplateGraph.addEdge(fromPath, toPath)
                if (jobTemplateGraph.hasCyclic()) {
                    error(TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.JOB.text))
                }
            }
            TemplateType.STEP -> {
                stepTemplateGraph.addEdge(fromPath, toPath)
                if (stepTemplateGraph.hasCyclic()) {
                    error(TEMPLATE_CYCLE_ERROR.format(toPath, fromPath, TemplateType.STEP.text))
                }
            }
            else -> {
                return
            }
        }
    }

    // 为模板中的变量赋值
    private fun parseTemplateParameters(
        fromPath: String,
        path: String,
        template: String,
        parameters: Map<String, Any?>?
    ): String {
        val newParameters =
            YamlObjects.getObjectFromYaml<ParametersTemplateNull>(path, template).parameters?.toMutableList()
        if (!newParameters.isNullOrEmpty()) {
            newParameters.forEachIndexed { index, param ->
                if (parameters != null) {
                    val valueName = param.name
                    val newValue = parameters[param.name]
                    if (parameters.keys.contains(valueName)) {
                        if (!param.values.isNullOrEmpty() && !param.values.contains(newValue)) {
                            error(VALUE_NOT_IN_ENUM.format(fromPath, valueName, newValue,
                                param.values.joinToString(",")))
                        } else {
                            newParameters[index] = param.copy(default = newValue)
                        }
                    }
                }
            }
        } else {
            return template
        }
        // 模板替换 先替换调用模板传入的参数，再替换模板的默认参数
        val parametersMap = newParameters.filter { it.default != null }.associate {
            "parameters.${it.name}" to if (it.default == null) {
                null
            } else {
                it.default.toString()
            }
        }
        return ScriptYmlUtils.parseParameterValue(template, parametersMap)!!
    }

    // 构造对象,因为未保存远程库的template信息，所以在递归回溯时无法通过yaml文件直接生成，故手动构造
    private fun getJob(fromPath: String, job: Map<String, Any>): PreJob {
        return PreJob(
            name = job["name"]?.toString(),
            runsOn = job["runs-on"],
            container = if (job["container"] == null) {
                null
            } else {
                YamlObjects.getContainer(fromPath, job["container"]!!)
            },
            services = if (job["services"] == null) {
                null
            } else {
                YamlObjects.getService(fromPath, job["services"]!!)
            },
            ifField = job["if"]?.toString(),
            steps = if (job["steps"] == null) {
                null
            } else {
                val steps = transValue<List<Map<String, Any>>>(fromPath, TemplateType.STEP.text, job["steps"])
                val list = mutableListOf<Step>()
                steps.forEach {
                    list.addAll(replaceStepTemplate(listOf(it), filePath))
                }
                list
            },
            timeoutMinutes = YamlObjects.getNullValue("timeout-minutes", job)?.toInt(),
            env = if (job["env"] == null) {
                null
            } else {
                transValue<Map<String, String>>(fromPath, "env", job["env"])
            },
            continueOnError = YamlObjects.getNullValue("continue-on-error", job)?.toBoolean(),
            strategy = if (job["strategy"] == null) {
                null
            } else {
                YamlObjects.getStrategy(fromPath, job["strategy"]!!)
            },
            dependOn = if (job["depend-on"] == null) {
                null
            } else {
                transValue<List<String>>(fromPath, "depend-on", job["depend-on"])
            }
        )
    }

    private fun getStage(fromPath: String, stage: Map<String, Any>): PreStage {
        return PreStage(
            name = stage["name"]?.toString(),
            id = stage["id"]?.toString(),
            label = stage["label"],
            ifField = stage["if"]?.toString(),
            fastKill = YamlObjects.getNullValue("fast-kill", stage)?.toBoolean(),
            jobs = if (stage["jobs"] == null) {
                null
            } else {
                val jobs = transValue<Map<String, Any>>(fromPath, TemplateType.JOB.text, stage["jobs"])
                val map = mutableMapOf<String, PreJob>()
                jobs.forEach { (key, value) ->
                    // 检查根文件处jobId重复
                    val newJob = replaceJobTemplate(mapOf(key to value), filePath)
                    if (key == TEMPLATE_KEY) {
                        checkDuplicateKey(filePath = filePath, keys = jobs.keys, newKeys = newJob.keys)
                    }
                    map.putAll(newJob)
                }
                map
            }
        )
    }

    // 从模板库中获得数据，如果有直接取出，没有则根据保存的库信息从远程仓库拉取，再没有则报错
    private fun getTemplate(path: String): String {
        if (!templates.keys.contains(path)) {
//             没有库信息说明是触发库
            val template = if (repo == null) {
                getTemplateMethod(
                    triggerToken,
                    triggerProjectId,
                    null,
                    triggerRef,
                    null,
                    path
                )
            } else {
                getTemplateMethod(
                    null,
                    sourceProjectId,
                    repo.repository,
                    repo.ref ?: triggerRef,
                    repo.credentials?.personalAccessToken,
                    path
                )
            }
            setTemplate(path, template)
        }
        return templates[path]!!
    }

    private fun addAndCheckTemplateNumb(file: String) {
        if (templateNumb.containsKey(file)) {
            templateNumb[file] = templateNumb[file]!! + 1
        } else {
            templateNumb[file] = 1
        }
        if (templateNumb[file]!! > MAX_TEMPLATE_NUMB) {
            error(TEMPLATE_NUMB_BEYOND.format(file))
        }
    }

    private fun addAndCheckTemplateDeep() {
        templateDeep++
        if (templateDeep > MAX_TEMPLATE_DEEP) {
            error(TEMPLATE_DEEP_BEYOND.format(filePath))
        }
    }

    private fun refreshTemplateDeep() {
        templateDeep = 0
    }

    private fun setTemplate(path: String, template: String) {
        templates[path] = template
    }

    private fun <T> transValue(file: String, type: String, value: Any?): T {
        if (value == null) {
            throw RuntimeException(TRANS_AS_ERROR.format(file, type))
        }
        return try {
            value as T
        } catch (e: Exception) {
            val newFile = if (repo == null) {
                "$templateDirectory$file"
            } else {
                "${repo.repository}/$templateDirectory$file"
            }
            throw RuntimeException(TRANS_AS_ERROR.format(newFile, type))
        }
    }

    private fun error(content: String) {
        throw RuntimeException(content)
    }
}

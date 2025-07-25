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

package com.tencent.devops.process.yaml.v3.parsers.template

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_YAML_FORMAT_EXCEPTION
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_YAML_FORMAT_EXCEPTION_LENGTH_LIMIT_EXCEEDED
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.Repositories
import com.tencent.devops.common.pipeline.pojo.transfer.ResourcesPools
import com.tencent.devops.common.pipeline.pojo.transfer.TemplateInfo
import com.tencent.devops.common.pipeline.pojo.transfer.format
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v3.check.Gate
import com.tencent.devops.process.yaml.v3.check.GateTemplate
import com.tencent.devops.process.yaml.v3.check.PreStageCheck
import com.tencent.devops.process.yaml.v3.check.PreTemplateStageCheck
import com.tencent.devops.process.yaml.v3.enums.TemplateType
import com.tencent.devops.process.yaml.v3.exception.YamlFormatException
import com.tencent.devops.process.yaml.v3.models.Extends
import com.tencent.devops.process.yaml.v3.models.GitNotices
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.models.PacNotices
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYamlIParser
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.Variable
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v3.parsers.template.models.NoReplaceTemplate
import com.tencent.devops.process.yaml.v3.parsers.template.models.TemplateDeepTreeNode

@Suppress("ALL")
class YamlTemplate<T>(
    val extraParameters: T,

    // 当前文件
    var filePath: TemplatePath,
    // 文件对象
    var yamlObject: ITemplateFilter?,
    // 当前库信息
    val nowRepo: Repositories?,
    // 目标库信息(发起库没有库信息)
    val repo: Repositories?,

    // 额外所有引用的resource-pool，这样不会干扰替换逻辑
    val resourcePoolMapExt: MutableMap<String, ResourcesPools>? = null,

    // 模板替换配置，针对某些替换功能提供可配置项
    val conf: YamlTemplateConf = YamlTemplateConf(),

    // 来自文件
    private val fileFromPath: TemplatePath? = null,

    // 远程模板类型(用来校验远程打平的模板的格式)
    private val resTemplateType: TemplateType? = null,

    // 校验模板深度和广度树
    private val rootDeepTree: TemplateDeepTreeNode = TemplateDeepTreeNode(
        path = filePath,
        parent = null,
        children = mutableListOf()
    ),

    // 获取模板文件函数，将模板替换过程与获取文件解耦，方便测试或链接其他代码库
    val getTemplateMethod: (
        param: GetTemplateParam<T>
    ) -> String
) {
    // 存储当前库的模板信息，减少重复获取 key: templatePath value： template
    private val templateLib = TemplateLibrary(extraParameters, getTemplateMethod)

    // 添加图防止模版的循环嵌套
    private val templateGraph = TemplateGraph()

    @Throws(
        YamlFormatException::class,
        JsonProcessingException::class,
        StackOverflowError::class
    )
    fun replace(
        parameters: Map<String, Any?>? = null
    ): PreScriptBuildYamlIParser {
        // 针对远程库进行打平替换时，根文件没有被替换Parameters
        val newYamlObject = if (repo != null) {
//            val template = parseTemplateParameters(
//                fromPath = fileFromPath ?: TemplatePath(""),
//                path = filePath,
//                template = templateLib.getTemplate(
//                    path = filePath,
//                    templateType = resTemplateType,
//                    nowRepo = nowRepo,
//                    toRepo = repo
//                ),
//                parameters = parameters,
//                deepTree = rootDeepTree
//            )
            val template = templateLib.getTemplate(
                path = filePath,
                templateType = resTemplateType,
                nowRepo = nowRepo,
                toRepo = repo
            )
            // 将根文件也保存在模板库中方便取出
            templateLib.setTemplate(filePath, template)
            YamlObjects.getObjectFromYaml<ITemplateFilter>(filePath, template)
        } else {
            templateLib.setTemplate(filePath, TemplateYamlMapper.toYaml(yamlObject!!))
            yamlObject
        } ?: throw RuntimeException("yamlObject cannot be null")

        val preYamlObject = newYamlObject.initPreScriptBuildYamlI()

        if (newYamlObject.extends != null) {
            replaceExtends(newYamlObject.extends!!, preYamlObject, rootDeepTree)
        }
        if (newYamlObject.variables != null) {
            replaceVariables(newYamlObject.variables!!, preYamlObject, rootDeepTree)
        }
        if (newYamlObject.stages != null) {
            replaceStages(newYamlObject.stages!!, preYamlObject, rootDeepTree)
        }
        if (newYamlObject.jobs != null) {
            replaceJobs(newYamlObject.jobs!!, preYamlObject, rootDeepTree)
        }
        if (newYamlObject.steps != null) {
            replaceSteps(newYamlObject.steps!!, preYamlObject, rootDeepTree)
        }
        if (newYamlObject.finally != null) {
            replaceFinally(newYamlObject.finally!!, preYamlObject, rootDeepTree)
        }

        return preYamlObject
    }

    private fun replaceExtends(
        extend: Extends,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        val toPath = TemplatePath(extend.template, extend.ref)
        // 根据远程模板获取
        val templateObject = replaceResAndParam(
            templateType = TemplateType.EXTEND,
            toPath = toPath,
            parameters = null,
            fromPath = filePath,
            deepTree = deepTree
        )
        // 获取extends模板后filePath就为被替换的文件了
        this.filePath = toPath
        // 需要替换模板的的递归替换
        if (templateObject[TemplateType.TRIGGER_ON.content] != null) {
            replaceTriggerOn(
                triggerOn = YamlObjects.transValue(
                    file = filePath,
                    type = TemplateType.TRIGGER_ON.text,
                    value = templateObject[TemplateType.TRIGGER_ON.content]
                ),
                preYamlObject = preYamlObject,
                deepTree = deepTree
            )
        }

        if (templateObject[TemplateType.VARIABLE.content] != null) {
            replaceVariables(
                variables = YamlObjects.transValue(
                    file = filePath,
                    type = TemplateType.VARIABLE.text,
                    value = templateObject[TemplateType.VARIABLE.content]
                ),
                preYamlObject = preYamlObject,
                deepTree = deepTree
            )
        }
        if (templateObject[TemplateType.STAGE.content] != null) {
            replaceStages(
                YamlObjects.transValue(filePath, TemplateType.STAGE.text, templateObject[TemplateType.STAGE.content]),
                preYamlObject,
                deepTree
            )
        }
        if (templateObject[TemplateType.JOB.content] != null) {
            replaceJobs(
                YamlObjects.transValue(filePath, TemplateType.JOB.text, templateObject[TemplateType.JOB.content]),
                preYamlObject,
                deepTree
            )
        }
        if (templateObject[TemplateType.STEP.content] != null) {
            replaceSteps(
                YamlObjects.transValue(filePath, TemplateType.STEP.text, templateObject[TemplateType.STEP.content]),
                preYamlObject,
                deepTree
            )
        }
        if (templateObject[TemplateType.FINALLY.content] != null) {
            replaceFinally(
                finally = YamlObjects.transValue(
                    file = filePath,
                    type = TemplateType.FINALLY.text,
                    value = templateObject[TemplateType.FINALLY.content]
                ),
                preYamlObject = preYamlObject,
                deepTree = deepTree
            )
        }
        // notices只用做一次模板替换没有嵌套模板
        if (templateObject["notices"] != null && preYamlObject is PreScriptBuildYamlParser) {
            val notices = mutableListOf<GitNotices>()
            val temNotices =
                YamlObjects.transValue<List<Map<String, Any?>>>(filePath, "notices", templateObject["notices"])
            temNotices.forEach {
                notices.add(YamlObjects.getNoticeV2(filePath, it))
            }
            preYamlObject.notices = notices
        }

        // notices只用做一次模板替换没有嵌套模板
        if (templateObject["notices"] != null && preYamlObject is PreScriptBuildYamlV3Parser) {
            val notices = mutableListOf<PacNotices>()
            val temNotices =
                YamlObjects.transValue<List<Map<String, Any?>>>(filePath, "notices", templateObject["notices"])
            temNotices.forEach {
                notices.add(YamlObjects.getNoticeV3(filePath, it))
            }
            preYamlObject.notices = notices
        }

        // 将不用替换的直接传入
        val newYaml =
            YamlObjects.getObjectFromYaml<NoReplaceTemplate>(toPath, TemplateYamlMapper.toYaml(templateObject))
        preYamlObject.label = newYaml.label
        preYamlObject.resources = newYaml.resources
        // 用户没写就用模板的名字
        if (preYamlObject.name.isNullOrBlank()) {
            preYamlObject.name = newYaml.name
        }
    }

    private fun replaceTriggerOn(
        triggerOn: Any,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        if (preYamlObject.yamlVersion() != YamlVersion.V3_0) return
        val triggerOnV3s = when (triggerOn) {
            // 简写方式
            is Map<*, *> -> listOf(JsonUtil.anyTo(triggerOn, object : TypeReference<PreTriggerOnV3>() {}))
            is List<*> -> JsonUtil.anyTo(triggerOn, object : TypeReference<List<PreTriggerOnV3>>() {})
            else -> null
        }
        (preYamlObject as PreScriptBuildYamlV3Parser).triggerOn = triggerOnV3s
    }

    private fun replaceVariables(
        variables: Map<String, Any>,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        val variableMap = mutableMapOf<String, Variable>()
        variables.forEach { (key, value) ->
            if (key == Constants.TEMPLATE_KEY) {
                throw YamlFormatException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ERROR_YAML_FORMAT_EXCEPTION,
                        params = arrayOf(
                            "variables",
                            "变量名",
                            "除了template关键字的其他字符串",
                            "template作为关键字保留"
                        )
                    )
                )
            }
            if (value !is Map<*, *>) {
                variableMap[key] = Variable(value.toString())
            } else {
                variableMap[key] = YamlObjects.getVariable(
                    fromPath = filePath,
                    key = key,
                    variable = YamlObjects.transValue(filePath, TemplateType.VARIABLE.text, value)
                )
            }
        }
        preYamlObject.variables = variableMap
    }

    private fun replaceStages(
        stages: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        val stageList = mutableListOf<PreStage>()
        stages.forEach { stage ->
            stageList.addAll(replaceStageTemplate(listOf(stage), filePath, deepTree))
        }
        preYamlObject.stages = stageList
    }

    private fun replaceJobs(
        jobs: Map<String, Any>,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        val jobMap = LinkedHashMap<String, PreJob>()
        jobs.forEach { (key, value) ->
            // 检查根文件处job_id重复
            val newJob = replaceJobTemplate(mapOf(key to value), filePath, deepTree)
            if (key == Constants.TEMPLATE_KEY) {
                TemplateYamlUtil.checkDuplicateKey(filePath = filePath, keys = jobs.keys, newKeys = newJob.keys)
            }
            jobMap.putAll(newJob)
        }
        preYamlObject.jobs = jobMap
    }

    private fun replaceSteps(
        steps: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        val stepList = mutableListOf<PreStep>()
        steps.forEach { step ->
            stepList.addAll(replaceStepTemplate(listOf(step), filePath, deepTree))
        }
        preYamlObject.steps = stepList
    }

    private fun replaceFinally(
        finally: Map<String, Any>,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        // finally: 与jobs: 的结构相同
        val finallyMap = LinkedHashMap<String, PreJob>()
        finally.forEach { (key, value) ->
            // 检查根文件处job_id重复
            val newFinally = replaceJobTemplate(mapOf(key to value), filePath, deepTree)
            if (key == Constants.TEMPLATE_KEY) {
                TemplateYamlUtil.checkDuplicateKey(filePath = filePath, keys = finally.keys, newKeys = newFinally.keys)
            }
            finallyMap.putAll(newFinally)
        }
        preYamlObject.finally = finallyMap
    }

    // 进行模板替换
    private fun replaceVariableTemplate(
        variables: Map<String, Any>,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): Map<String, Variable> {
        val variableMap = mutableMapOf<String, Variable>()
        variables.forEach { (key, value) ->
            // 如果是模板文件则进行模板替换
            if (key == Constants.TEMPLATE_KEY) {
                val toPathList =
                    YamlObjects.transValue<List<Map<String, Any>>>(fromPath, TemplateType.VARIABLE.text, value)
                toPathList.forEach { item ->
                    val toPath = TemplatePath(
                        item[Constants.OBJECT_TEMPLATE_PATH].toString(), item[Constants.REF]?.toString()
                    )
                    // 保存并检查是否存在循环引用模板
                    templateGraph.saveAndCheckCyclicTemplate(
                        fromPath.toString(), toPath.toString(), TemplateType.VARIABLE
                    )
                    // 获取需要替换的变量
                    val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                        file = fromPath,
                        type = Constants.PARAMETERS_KEY,
                        key = Constants.PARAMETERS_KEY,
                        map = item
                    )
                    // 对模板文件进行远程库和参数替换，并实例化
                    val templateObject = replaceResAndParam(
                        TemplateType.VARIABLE, toPath, parameters, fromPath,
                        deepTree
                    )
                    // 判断实例化后的模板文件中是否引用了模板文件，如果有，则递归替换
                    val newVar = replaceVariableTemplate(
                        variables = YamlObjects.transValue(
                            toPath, TemplateType.VARIABLE.text, templateObject[TemplateType.VARIABLE.content]
                        ),
                        fromPath = toPath,
                        deepTree = deepTree.add(toPath)
                    )
                    // 检测variable是否存在重复的key
                    TemplateYamlUtil.checkDuplicateKey(
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
                    variableMap[key] = Variable(value.toString())
                } else {
                    variableMap[key] = YamlObjects.getVariable(
                        fromPath = fromPath,
                        key = key,
                        variable = YamlObjects.transValue(fromPath, TemplateType.VARIABLE.text, value)
                    )
                }
            }
        }
        return variableMap
    }

    private fun replaceStageTemplate(
        stages: List<Map<String, Any>>,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): List<PreStage> {
        val stageList = mutableListOf<PreStage>()
        stages.forEach { stage ->
            if (Constants.TEMPLATE_KEY in stage.keys) {
                val toPath = TemplatePath(
                    stage[Constants.TEMPLATE_KEY].toString(), stage[Constants.REF]?.toString()
                )
                templateGraph.saveAndCheckCyclicTemplate(fromPath.toString(), toPath.toString(), TemplateType.STAGE)

                val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                    file = fromPath,
                    type = Constants.PARAMETERS_KEY,
                    key = Constants.PARAMETERS_KEY,
                    map = stage
                )
                // 对模板文件进行远程库和参数替换，并实例化
                val templateObject = replaceResAndParam(TemplateType.STAGE, toPath, parameters, fromPath, deepTree)
                stageList.addAll(
                    replaceStageTemplate(
                        stages = YamlObjects.transValue(
                            toPath,
                            TemplateType.STAGE.text,
                            templateObject[TemplateType.STAGE.content]
                        ),
                        fromPath = toPath,
                        deepTree = deepTree.add(toPath)
                    )
                )
            } else {
                stageList.add(getStage(fromPath, stage, deepTree))
            }
        }
        return stageList
    }

    fun replaceJobTemplate(
        jobs: Map<String, Any>,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): Map<String, PreJob> {
        val jobMap = mutableMapOf<String, PreJob>()
        jobs.forEach { (key, value) ->
            if (key == Constants.TEMPLATE_KEY) {
                val toPathList = YamlObjects.transValue<List<Map<String, Any>>>(fromPath, TemplateType.JOB.text, value)
                toPathList.forEach { item ->
                    val toPath = TemplatePath(
                        item[Constants.OBJECT_TEMPLATE_PATH].toString(), item[Constants.REF]?.toString()
                    )
                    templateGraph.saveAndCheckCyclicTemplate(fromPath.toString(), toPath.toString(), TemplateType.JOB)

                    val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                        file = fromPath,
                        type = Constants.PARAMETERS_KEY,
                        key = Constants.PARAMETERS_KEY,
                        map = item
                    )

                    // 对模板文件进行远程库和参数替换，并实例化
                    val templateObject = replaceResAndParam(TemplateType.JOB, toPath, parameters, fromPath, deepTree)

                    val newJob = replaceJobTemplate(
                        jobs = YamlObjects.transValue(
                            toPath, TemplateType.JOB.text, templateObject[TemplateType.JOB.content]
                        ),
                        fromPath = toPath,
                        deepTree = deepTree.add(toPath)
                    )
                    // 检测job是否存在重复的key
                    TemplateYamlUtil.checkDuplicateKey(
                        filePath = filePath,
                        keys = jobMap.keys,
                        newKeys = newJob.keys,
                        toPath = toPath
                    )
                    jobMap.putAll(newJob)
                }
            } else {
                // 校验id不能超过64，因为id可能为数字无法在schema支持，放到后台
                if (key.length > 64) {
                    throw YamlFormatException(
                        I18nUtil.getCodeLanMessage(
                            messageCode = ERROR_YAML_FORMAT_EXCEPTION_LENGTH_LIMIT_EXCEEDED,
                            params = arrayOf(fromPath.toString(), key)
                        )
                    )
                }
                jobMap[key] = getJob(fromPath, YamlObjects.transValue(fromPath, TemplateType.JOB.text, value), deepTree)
            }
        }
        return jobMap
    }

    fun replaceStepTemplate(
        steps: List<Map<String, Any>>,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): List<PreStep> {
        val stepList = mutableListOf<PreStep>()
        steps.forEach { step ->
            if (Constants.TEMPLATE_KEY in step.keys) {
                val toPath = TemplatePath(
                    step[Constants.TEMPLATE_KEY].toString(), step[Constants.REF]?.toString()
                )

                templateGraph.saveAndCheckCyclicTemplate(fromPath.toString(), toPath.toString(), TemplateType.STEP)

                val parameters = YamlObjects.transNullValue<Map<String, Any?>>(
                    file = fromPath,
                    type = Constants.PARAMETERS_KEY,
                    key = Constants.PARAMETERS_KEY,
                    map = step
                )

                // 对模板文件进行远程库和参数替换，并实例化
                val templateObject = replaceResAndParam(TemplateType.STEP, toPath, parameters, fromPath, deepTree)

                stepList.addAll(
                    replaceStepTemplate(
                        steps = YamlObjects.transValue(
                            toPath,
                            TemplateType.STEP.text,
                            templateObject[TemplateType.STEP.content]
                        ),
                        fromPath = toPath,
                        deepTree = deepTree.add(toPath)
                    )
                )
            } else {
                stepList.add(
                    YamlObjects.getStep(
                        fromPath,
                        step,
                        TemplateInfo(
                            remote = repo != null,
                            remoteTemplateProjectId = repo?.repository
                        )
                    )
                )
            }
        }
        return stepList
    }

    // 替换Stage准入准出信息
    fun replaceStageCheckTemplate(
        stageName: String,
        check: Map<String, Any>?,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): PreStageCheck? {
        if (check == null) {
            return null
        }
        val gateList = mutableListOf<Gate>()

        if (check["gates"] != null) {
            val gates = YamlObjects.transValue<List<Map<String, Any>>>(fromPath, TemplateType.GATE.text, check["gates"])
            var isTemplate = false
            gates.forEach { gate ->
                if (Constants.TEMPLATE_KEY in gate.keys) {
                    isTemplate = true
                }
            }
            if (!isTemplate) {
                return YamlObjects.getObjectFromYaml<PreStageCheck>(fromPath, TemplateYamlMapper.toYaml(check))
            }
        }

        val checkObject =
            YamlObjects.getObjectFromYaml<PreTemplateStageCheck>(fromPath, TemplateYamlMapper.toYaml(check))

        checkObject.gates?.forEach { gate ->
            val toPath = TemplatePath(gate.template, gate.ref)
            val templateObject = replaceResAndParam(
                templateType = TemplateType.GATE,
                toPath = toPath,
                parameters = gate.parameters,
                fromPath = fromPath,
                deepTree = deepTree
            )
            val gateTemplate =
                YamlObjects.getObjectFromYaml<GateTemplate>(toPath, TemplateYamlMapper.toYaml(templateObject))
            gateList.addAll(
                gateTemplate.gates
            )
            gateTemplate.gates.forEach {
                if (it.rule.size > Constants.STAGE_CHECK_GATE_RULE_NUMB) {
                    error(Constants.STAGE_CHECK_GATE_RULE_NUMB_BEYOND.format(fromPath, stageName, it.name))
                }
            }
            if (gateList.size > Constants.STAGE_CHECK_GATE_NUMB) {
                error(Constants.STAGE_CHECK_GATE_NUMB_BEYOND.format(fromPath, stageName))
            }
        }
        return PreStageCheck(
            reviews = checkObject.reviews,
            gates = gateList,
            timeoutHours = checkObject.timeoutHours
        )
    }

    // 替换远程库和参数信息
    private fun replaceResAndParam(
        templateType: TemplateType,
        toPath: TemplatePath,
        parameters: Map<String, Any?>?,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): Map<String, Any> {
        // 判断是否为远程库，如果是远程库将其远程库文件打平进行替换
        var newTemplate = if (toPath.path.contains(Constants.FILE_REPO_SPLIT)) {
            // 针对没有循环嵌套做特殊处理
            if (templateType == TemplateType.GATE || templateType == TemplateType.PARAMETERS) {
                templateLib.getTemplate(
                    path = TemplatePath(
                        path = toPath.path.split(Constants.FILE_REPO_SPLIT).first(),
                        ref = toPath.ref
                    ),
                    nowRepo = repo,
                    toRepo = TemplateYamlUtil.checkAndGetRepo(
                        fromPath = fromPath,
                        repoName = toPath.path.split(Constants.FILE_REPO_SPLIT)[1],
                        templateType = templateType,
                        templateLib = templateLib,
                        nowRepo = nowRepo,
                        toRepo = repo
                    ),
                    templateType = templateType
                )
            } else {
                replaceResTemplateFile(
                    templateType = templateType,
                    toPath = toPath,
                    parameters = parameters,
                    toRepo = TemplateYamlUtil.checkAndGetRepo(
                        fromPath = fromPath,
                        repoName = toPath.path.split(Constants.FILE_REPO_SPLIT)[1],
                        templateType = templateType,
                        templateLib = templateLib,
                        nowRepo = nowRepo,
                        toRepo = repo
                    ),
                    deepTree = deepTree
                )
            }
        } else {
            templateLib.getTemplate(
                path = toPath,
                templateType = templateType,
                nowRepo = nowRepo,
                toRepo = repo
            )
        }
        // 将需要替换的变量填入模板文件，参数模板不用替换
//        if (templateType != TemplateType.PARAMETERS) {
//            newTemplate = parseTemplateParameters(
//                fromPath = fromPath,
//                path = toPath,
//                template = newTemplate,
//                parameters = parameters,
//                deepTree = deepTree
//            )
//        }
        // 将模板文件实例化
        val yamlObject = YamlObjects.getObjectFromYaml<Map<String, Any>>(toPath, newTemplate)
        // 将模板引用的pools加入
        if (yamlObject["resources"] != null) {
            YamlObjects.getResourcePools(toPath, yamlObject["resources"]!!).forEach { pool ->
                resourcePoolMapExt?.put(pool.format(), pool)
            }
        }
        return yamlObject
    }

    // 对远程仓库中的模板进行远程仓库替换
    private fun replaceResTemplateFile(
        templateType: TemplateType,
        toPath: TemplatePath,
        parameters: Map<String, Any?>?,
        toRepo: Repositories,
        deepTree: TemplateDeepTreeNode
    ): String {
        // 判断是否有库之间的循环依赖
        templateGraph.saveAndCheckCyclicRepo(fromPath = filePath.toString(), repo = repo, toRepo = toRepo)

        val resYamlObject = YamlTemplate(
            yamlObject = null,
            fileFromPath = filePath,
            filePath = TemplatePath(
                toPath.path.split(Constants.FILE_REPO_SPLIT)[0], toPath.path
            ),
            extraParameters = extraParameters,
            nowRepo = repo,
            repo = toRepo,
            rootDeepTree = deepTree,
            resTemplateType = templateType,
            getTemplateMethod = getTemplateMethod,
            resourcePoolMapExt = resourcePoolMapExt,
            conf = conf
        ).replace(parameters = parameters)
//
//        // 将远程模板引用的pools加入
//        if (resYamlObject.resources?.pools != null) {
//            resYamlObject.resources?.pools?.forEach { pool ->
//                resourcePoolMapExt?.put(pool.format(), pool)
//            }
//        }
        // 替换后的远程模板去除不必要参数
        resYamlObject.resources = null
        return TemplateYamlMapper.toYaml(resYamlObject)
    }
//
//    // 在parameters替换前做统一处理，例如替换模板
//    private fun parseTemplateParameters(
//        fromPath: TemplatePath,
//        path: TemplatePath,
//        template: String,
//        parameters: Map<String, Any?>?,
//        deepTree: TemplateDeepTreeNode
//    ): String {
//        // 后面的需要覆盖前面的，所以使用map
//        val templateMap = mutableMapOf<String, Variable>()
//
//        val preTemplateParam =
//            YamlObjects.getObjectFromYaml<PreParametersTemplate>(path, template).variables
//
//        val templates = preTemplateParam?.get(Constants.TEMPLATE_KEY)
//        if (templates != null && templates is List<*>) {
//            templates.forEach { pre ->
//                val t = YamlObjects.transValue<Map<String, Any>>(fromPath, Constants.TEMPLATE_KEY, pre)
//                val toPath = TemplatePath(
//                    t[Constants.OBJECT_TEMPLATE_PATH].toString(), t[Constants.REF]?.toString()
//                )
//                // 因为这里是替换模板的模板参数，所以frompath需要使用模板文件而不是来源文件
//                val templateObject = replaceResAndParam(
//                    templateType = TemplateType.PARAMETERS,
//                    toPath = toPath,
//                    parameters = null,
//                    fromPath = path,
//                    deepTree = deepTree
//                )
//                val parametersTemplate = YamlObjects.getObjectFromYaml<ParametersTemplateNull>(
//                    path = toPath,
//                    template = TemplateYamlMapper.toYaml(templateObject)
//                )
//                parametersTemplate.variables?.let { p -> templateMap.putAll(p) }
//            }
//        }
//
//        preTemplateParam?.forEach { (key, value) ->
//            if (value !is Map<*, *>) {
//                templateMap[key] = Variable(value.toString())
//            } else {
//                templateMap[key] = YamlObjects.getVariable(
//                    fromPath = fromPath,
//                    key = key,
//                    variable = YamlObjects.transValue(fromPath, TemplateType.VARIABLE.text, value)
//                )
//            }
//        }
//
//        return template
//        // 不替换parameters变量了
//        /*
//                return if (conf.useOldParametersExpression) {
//                    TemplateYamlUtil.parseTemplateParametersOld(
//                        fromPath = fromPath,
//                        path = path,
//                        template = template,
//                        templateParameters = templateMap.values.toMutableList(),
//                        parameters = parameters
//                    )
//                } else {
//                    ParametersExpressionParse.parseTemplateParameters(
//                        fromPath = fromPath,
//                        path = path,
//                        template = template,
//                        templateParameters = templateMap.values.toMutableList(),
//                        parameters = parameters
//                    )
//                }*/
//    }

    private fun error(content: String) {
        throw YamlFormatException(content)
    }
}

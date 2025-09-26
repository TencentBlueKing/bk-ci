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
import com.tencent.devops.common.pipeline.pojo.transfer.IPreStep
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
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYamlIParser
import com.tencent.devops.process.yaml.v3.models.PreScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.Variable
import com.tencent.devops.process.yaml.v3.models.job.IPreJob
import com.tencent.devops.process.yaml.v3.models.job.PreJobTemplate
import com.tencent.devops.process.yaml.v3.models.job.PreJobTemplateList
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3
import com.tencent.devops.process.yaml.v3.models.stage.IPreStage
import com.tencent.devops.process.yaml.v3.parsers.template.models.GetTemplateParam
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
        val stageList = mutableListOf<IPreStage>()
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
        val jobMap = LinkedHashMap<String, IPreJob>()
        jobs.forEach { (key, value) ->
            // 检查根文件处job_id重复
            val newJob = replaceJobTemplate(mapOf(key to value), filePath, deepTree)
            jobMap.putAll(newJob)
        }
        preYamlObject.jobs = jobMap
    }

    private fun replaceSteps(
        steps: List<Map<String, Any>>,
        preYamlObject: PreScriptBuildYamlIParser,
        deepTree: TemplateDeepTreeNode
    ) {
        val stepList = mutableListOf<IPreStep>()
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
        val finallyMap = LinkedHashMap<String, IPreJob>()
        finally.forEach { (key, value) ->
            finallyMap.putAll(replaceJobTemplate(mapOf(key to value), filePath, deepTree))
        }
        preYamlObject.finally = finallyMap
    }

    private fun replaceStageTemplate(
        stages: List<Map<String, Any>>,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): List<IPreStage> {
        val stageList = mutableListOf<IPreStage>()
        stages.forEach { stage ->
            if (keysInTemplateKey(stage.keys)) {
                stageList.add(getStageTemplate(fromPath, stage, deepTree))
            } else {
                stageList.add(getStage(fromPath, stage, deepTree))
            }
        }
        return stageList
    }

    private fun keysInTemplateKey(keys: Collection<String>): Boolean {
        return Constants.TEMPLATE_KEY in keys ||
            Constants.TEMPLATE_KEY_NAME in keys ||
            Constants.TEMPLATE_KEY_ID in keys
    }

    fun replaceJobTemplate(
        jobs: Map<String, Any>,
        fromPath: TemplatePath,
        deepTree: TemplateDeepTreeNode
    ): Map<String, IPreJob> {
        val jobMap = mutableMapOf<String, IPreJob>()
        jobs.forEach { (key, value) ->
            if (key == Constants.TEMPLATE_KEY) {
                val templates = mutableListOf<PreJobTemplate>()
                val toPathList = YamlObjects.transValue<List<Map<String, Any>>>(fromPath, TemplateType.JOB.text, value)
                toPathList.forEach { item ->
                    templates.add(getJobTemplate(fromPath, item, deepTree))
                }
                jobMap[key] = PreJobTemplateList(templates)
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
    ): List<IPreStep> {
        val stepList = mutableListOf<IPreStep>()
        steps.forEach { step ->
            if (keysInTemplateKey(step.keys)) {
                stepList.add(
                    YamlObjects.getStepTemplate(
                        fromPath,
                        step,
                        TemplateInfo(
                            remote = repo != null,
                            remoteTemplateProjectId = repo?.repository
                        )
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

    private fun error(content: String) {
        throw YamlFormatException(content)
    }
}

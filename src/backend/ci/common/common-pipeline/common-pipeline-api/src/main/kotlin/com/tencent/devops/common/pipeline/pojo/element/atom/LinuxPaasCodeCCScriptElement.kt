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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.utils.CoverityUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory

@ApiModel("CodeCC代码检查任务", description = LinuxPaasCodeCCScriptElement.classType)
data class LinuxPaasCodeCCScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "执行Linux脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("脚本类型", required = true)
    override val scriptType: BuildScriptType,
    @ApiModelProperty("脚本内容", required = true)
    override val script: String = "",
    @ApiModelProperty("CodeCC Task Name", required = false, hidden = true)
    override var codeCCTaskName: String? = null,
    @ApiModelProperty("CodeCC Task CN Name", required = false, hidden = true)
    override var codeCCTaskCnName: String? = null,
    @ApiModelProperty("CodeCC Task Id", required = false, hidden = true)
    var codeCCTaskId: String? = null,
    @ApiModelProperty("是否异步", required = false)
    override var asynchronous: Boolean? = false,
    @ApiModelProperty("扫描类型（0：全量, 1：增量）", required = false)
    override var scanType: String?,
    @ApiModelProperty("代码存放路径", required = false)
    override val path: String? = null,
    @ApiModelProperty("工程语言", required = true)
    override val languages: List<ProjectLanguage>
) : LinuxCodeCCScriptElement(name, id, status, scriptType, script, codeCCTaskName, codeCCTaskCnName, languages, asynchronous, scanType, path) {

    companion object {
        const val classType = "linuxPaasCodeCCScript"
        private val logger = LoggerFactory.getLogger(LinuxPaasCodeCCScriptElement::class.java)
    }

    fun postGenerateElement(projectId: String, pipelineId: String, pipelineName: String, userId: String, create: Boolean) {
        if (languages.isEmpty()) {
            throw OperationException("工程语言不能为空")
        }
        val toolRuleSet = genToolRuleSet()
        try {
            if ((!create) && (!codeCCTaskId.isNullOrEmpty())) {
                if (CoverityUtils.isTaskExist(codeCCTaskId!!, userId)) {
                    // Update the coverity
                    CoverityUtils.updateTask(pipelineName, userId, codeCCTaskId!!, languages,
                            compilePlat ?: "LINUX", tools ?: listOf("COVERITY"), pyVersion ?: "", eslintRc
                            ?: "", scanType ?: "1", phpcsStandard ?: "", goPath ?: "", ccnThreshold, needCodeContent, toolRuleSet)
                    return
                }
            }
            // Create a new one
            val task = CoverityUtils.createTask(projectId, pipelineId, pipelineName, userId, languages,
                    compilePlat ?: "LINUX", tools ?: listOf("COVERITY"), pyVersion ?: "", eslintRc ?: "", scanType
                    ?: "1", phpcsStandard ?: "", goPath ?: "", ccnThreshold, needCodeContent, toolRuleSet)
            codeCCTaskId = task.task_info!!.task_id
            codeCCTaskName = task.task_info.task_en_name
            codeCCTaskCnName = task.task_info.task_cn_name
            logger.info("Create the coverity task($task)")
        } catch (e: Exception) {
            logger.warn("Fail to create the coverity codecc task($projectId|$pipelineId|$pipelineName|$userId|$name)", e)
            throw OperationException("代码检查任务创建失败，请联系【蓝盾助手】")
        }
    }

    fun genToolRuleSet(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (!coverityToolSetId.isNullOrBlank()) map["COVERITY"] = coverityToolSetId!!
        if (!klocworkToolSetId.isNullOrBlank()) map["KLOCWORK"] = klocworkToolSetId!!
        if (!cpplintToolSetId.isNullOrBlank()) map["CPPLINT"] = cpplintToolSetId!!
        if (!eslintToolSetId.isNullOrBlank()) map["ESLINT"] = eslintToolSetId!!
        if (!pylintToolSetId.isNullOrBlank()) map["PYLINT"] = pylintToolSetId!!
        if (!gometalinterToolSetId.isNullOrBlank()) map["GOML"] = gometalinterToolSetId!!
        if (!checkStyleToolSetId.isNullOrBlank()) map["CHECKSTYLE"] = checkStyleToolSetId!!
        if (!styleCopToolSetId.isNullOrBlank()) map["STYLECOP"] = styleCopToolSetId!!
        if (!detektToolSetId.isNullOrBlank()) map["DETEKT"] = detektToolSetId!!
        if (!phpcsToolSetId.isNullOrBlank()) map["PHPCS"] = phpcsToolSetId!!
        if (!sensitiveToolSetId.isNullOrBlank()) map["SENSITIVE"] = sensitiveToolSetId!!
        if (!occheckToolSetId.isNullOrBlank()) map["OCCHECK"] = occheckToolSetId!!
        if (!gociLintToolSetId.isNullOrBlank()) map["GOCILINT"] = gociLintToolSetId!!
        return map
    }

    fun onDeleteElement(userId: String) {
        logger.info("Start to delete the task($codeCCTaskId) in codecc by user $userId")
        if (codeCCTaskId.isNullOrEmpty()) {
            logger.warn("The codecc task id is empty")
            return
        }
        if (!CoverityUtils.deleteTask(codeCCTaskId!!, userId)) {
            logger.warn("Fail to delete the task($codeCCTaskId) in codecc")
            throw OperationException("删除代码扫描原子失败")
        } else {
            codeCCTaskId = null
        }
    }

    override fun cleanUp() {
        codeCCTaskId = null
        codeCCTaskName = null
    }

    override fun getClassType() = classType
}

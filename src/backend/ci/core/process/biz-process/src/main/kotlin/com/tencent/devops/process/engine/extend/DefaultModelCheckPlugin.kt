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

package com.tencent.devops.process.engine.extend

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_PARAM_IN_JOB_CONDITION
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_PUBLIC_WINDOWS_BUILDER
import com.tencent.devops.process.constant.ProcessMessageCode.MODEL_ATOMCODE_PROJECT_NOT_INSTALL
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.control.DependOnUtils
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.plugin.load.ContainerBizRegistrar
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import org.slf4j.LoggerFactory

open class DefaultModelCheckPlugin constructor(open val client: Client) : ModelCheckPlugin {

    override fun checkModelIntegrity(model: Model, projectId: String?) {

        // 检查流水线名称
        PipelineUtils.checkPipelineName(model.name)

        val stage = model.stages.getOrNull(0)
            ?: throw ErrorCodeException(
                defaultMessage = "流水线Stage为空",
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
            )
        if (stage.containers.size != 1) {
            logger.warn("The trigger stage contain more than one container (${stage.containers.size})")
            throw ErrorCodeException(
                defaultMessage = "流水线只能有一个触发Stage",
                errorCode = ProcessMessageCode.ONLY_ONE_TRIGGER_JOB_IN_PIPELINE
            )
        }

        // 检查触发容器
        checkTriggerContainer(stage)

        val elementCnt = mutableMapOf<String, Int>()
        val containerCnt = mutableMapOf<String, Int>()
        val storeAtomList = mutableListOf<String>()
        model.stages.forEach { s ->
            if (s.containers.isEmpty()) {
                throw ErrorCodeException(
                    defaultMessage = "流水线Stage为空",
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
                )
            }
            if (s.stageControlOption?.manualTrigger == true && s.stageControlOption?.triggerUsers?.isEmpty() == true)
                throw ErrorCodeException(
                    defaultMessage = "手动触发的Stage没有未配置可执行人",
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_NO_TRIGGER_USER
                )
            s.containers.forEach { c ->
                val cCnt = containerCnt.computeIfPresent(c.getClassType()) { _, oldValue -> oldValue + 1 }
                    ?: containerCnt.computeIfAbsent(c.getClassType()) { 1 } // 第一次时出现1次
                ContainerBizRegistrar.getPlugin(c)?.check(c, cCnt)
                c.elements.forEach { e ->
                    val eCnt = elementCnt.computeIfPresent(e.getAtomCode()) { _, oldValue -> oldValue + 1 }
                        ?: elementCnt.computeIfAbsent(e.getAtomCode()) { 1 } // 第一次时出现1次
                    ElementBizRegistrar.getPlugin(e)?.check(e, eCnt)
                    if (isStoreAtom(e)) {
                        storeAtomList.add(e.getAtomCode())
                        checkoutAtomExist(e)
                    }
                }
            }
            DependOnUtils.checkRepeatedJobId(stage)
        }

        if (storeAtomList.isNotEmpty() && !projectId.isNullOrEmpty()) {
            val projectInstallCheck = AtomUtils.isProjectInstallAtom(storeAtomList, projectId!!, client)
            if (projectInstallCheck.isNotEmpty()) {
                logger.warn("save model project not install atom  $projectId| ${model.name}| $storeAtomList")
                throw ErrorCodeException(
                        defaultMessage = "Model内包含项目未安装插件${projectInstallCheck[0]}",
                        errorCode = MODEL_ATOMCODE_PROJECT_NOT_INSTALL
                )
            }
        }
    }

    open fun checkTriggerContainer(stage: Stage) {
        val triggerContainer = (stage.containers.getOrNull(0) ?: throw ErrorCodeException(
            defaultMessage = "流水线Stage为空",
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
        )) as TriggerContainer
        PipelineUtils.checkPipelineParams(triggerContainer.params)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultModelCheckPlugin::class.java)
    }

    private fun checkoutAtomExist(e: Element) {
        if (e is MarketBuildLessAtomElement || e is MarketBuildAtomElement) {
            if (!AtomUtils.isAtomExist(e.getAtomCode(), client)) {
                logger.warn("save model atom is notExist  ${e.getAtomCode()}")
                throw ErrorCodeException(
                        defaultMessage = "Model内包含商店不存在插件${e.getAtomCode()}",
                        errorCode = ProcessMessageCode.MODEL_ATOMCODE_NOT_EXSIT
                )
            }
        }
    }

    private fun isStoreAtom(element: Element): Boolean {
        if (element is MarketBuildLessAtomElement || element is MarketBuildAtomElement) {
            return true
        }
        return false
    }

    /**
     * When edit the pipeline, it need to check if the element delete
     */
    private fun Model.elementExist(elementId: String?): Boolean {
        stages.forEach { s ->
            s.containers.forEach { c ->
                if (loopFind(c.elements, elementId)) {
                    return true
                }
            }
        }
        return false
    }

    private fun loopFind(elements: List<Element>, elementId: String?): Boolean {
        elements.forEach { e ->
            if (e.id == elementId) {
                return true
            }
        }
        return false
    }

    override fun beforeDeleteElementInExistsModel(
        existModel: Model,
        sourceModel: Model?,
        param: BeforeDeleteParam
    ) {
        logger.info("before delete element source model: $sourceModel")

        existModel.stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    deletePrepare(sourceModel, e, param)
                }
            }
        }
    }

    private fun deletePrepare(sourceModel: Model?, e: Element, param: BeforeDeleteParam) {
        if (sourceModel == null || !sourceModel.elementExist(e.id)) {
            logger.info("The element(${e.name}/${e.id}) is delete")
            ElementBizRegistrar.getPlugin(e)?.beforeDelete(e, param)
        } else {
            logger.info("The element(${e.name}/${e.id}) is not delete")
        }
    }

    override fun clearUpModel(model: Model) {
        model.stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    e.cleanUp()
                }
            }
        }
    }

    override fun checkJob(jobContainer: Container, projectId: String, pipelineId: String, userId: String) {
        if (jobContainer is VMBuildContainer && jobContainer.baseOS == VMBaseOS.WINDOWS) {
            if (isThirdPartyAgentEmpty(jobContainer)) {
                throw ErrorCodeException(
                    errorCode = ERROR_NO_PUBLIC_WINDOWS_BUILDER,
                    defaultMessage = "请设置Windows构建机"
                )
            }
        }

        val jobControlOption = when (jobContainer) {
            is VMBuildContainer -> jobContainer.jobControlOption
            is NormalContainer -> jobContainer.jobControlOption
            else -> null
        }

        if (jobControlOption?.runCondition == JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN ||
            jobControlOption?.runCondition == JobRunCondition.CUSTOM_VARIABLE_MATCH
        ) {
            if (jobControlOption.customVariables == null ||
                jobControlOption.customVariables!!.isEmpty()
            ) {
                throw ErrorCodeException(
                    errorCode = ERROR_NO_PARAM_IN_JOB_CONDITION,
                    defaultMessage = "请设置Job运行条件时的自定义变量"
                )
            }
        }
    }

    private fun isThirdPartyAgentEmpty(vmBuildContainer: VMBuildContainer): Boolean {
        // Old logic
        if (vmBuildContainer.thirdPartyAgentId.isNullOrBlank() && vmBuildContainer.thirdPartyAgentEnvId.isNullOrBlank()) {
            // New logic
            val dispatchType = vmBuildContainer.dispatchType ?: return true
            return when (dispatchType.buildType()) {
                BuildType.THIRD_PARTY_AGENT_ID, BuildType.THIRD_PARTY_AGENT_ENV -> dispatchType.value.isBlank()
                else -> true
            }
        }

        return false
    }
}

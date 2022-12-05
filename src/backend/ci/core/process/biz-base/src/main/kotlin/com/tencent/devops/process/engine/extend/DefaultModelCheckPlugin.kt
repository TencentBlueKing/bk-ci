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

package com.tencent.devops.process.engine.extend

import com.tencent.devops.common.api.check.Preconditions
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
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
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.control.DependOnUtils
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.plugin.load.ContainerBizRegistrar
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import com.tencent.devops.process.pojo.config.JobCommonSettingConfig
import com.tencent.devops.process.pojo.config.PipelineCommonSettingConfig
import com.tencent.devops.process.pojo.config.StageCommonSettingConfig
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import com.tencent.devops.process.utils.KEY_JOB
import com.tencent.devops.process.utils.KEY_STAGE
import com.tencent.devops.process.utils.KEY_TASK
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.StoreParam
import com.tencent.devops.store.pojo.common.StoreVersion
import org.slf4j.LoggerFactory

open class DefaultModelCheckPlugin constructor(
    open val client: Client,
    open val pipelineCommonSettingConfig: PipelineCommonSettingConfig,
    open val stageCommonSettingConfig: StageCommonSettingConfig,
    open val jobCommonSettingConfig: JobCommonSettingConfig,
    open val taskCommonSettingConfig: TaskCommonSettingConfig
) : ModelCheckPlugin {

    override fun checkModelIntegrity(model: Model, projectId: String?): Int {
        var metaSize = 0
        // 检查流水线名称
        PipelineUtils.checkPipelineName(
            name = model.name,
            maxPipelineNameSize = pipelineCommonSettingConfig.maxPipelineNameSize
        )
        PipelineUtils.checkPipelineDescLength(
            desc = model.desc,
            maxPipelineNameSize = pipelineCommonSettingConfig.maxPipelineDescSize
        )
        // 检查流水线model是否过大
        val modelSize = JsonUtil.toJson(model).length
        if (modelSize > pipelineCommonSettingConfig.maxModelSize.toLong()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_TOO_LARGE,
                params = arrayOf(pipelineCommonSettingConfig.maxModelSize.toString())
            )
        }
        val stages = model.stages
        // 判断stage数量是否超过系统限制
        if (stages.size > pipelineCommonSettingConfig.maxStageNum) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE,
                params = arrayOf("", KEY_STAGE, pipelineCommonSettingConfig.maxStageNum.toString())
            )
        }
        val stage = stages.getOrNull(0)
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
        val lastPosition = model.stages.size - 1
        model.stages.forEachIndexed { nowPosition, s ->
            val containers = s.containers
            // 判断stage下container数量是否超过系统限制
            if (containers.size > stageCommonSettingConfig.maxJobNum) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE,
                    params = arrayOf(
                        s.name ?: "stage_$nowPosition",
                        KEY_JOB,
                        stageCommonSettingConfig.maxJobNum.toString()
                    )
                )
            }
            if (containers.isEmpty()) {
                throw ErrorCodeException(
                    defaultMessage = "流水线Stage为空",
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
                )
            }

            if (s.finally) { // finallyStage只能存在于最后一个
                if (nowPosition < lastPosition) {
                    throw ErrorCodeException(
                        defaultMessage = "流水线: 每个Model只能包含一个FinallyStage，并且处于最后位置",
                        errorCode = ProcessMessageCode.ERROR_FINALLY_STAGE
                    )
                }
            }

            // #4531 检查stage审核组配置是否符合要求
            if (s.stageControlOption?.manualTrigger == true || s.checkIn?.manualTrigger == true) {
                checkStageReviewers(s)
            }

            val atomVersions = mutableSetOf<StoreVersion>()
            val atomInputParamList = mutableListOf<StoreParam>()
            metaSize += checkElements(
                stage = s,
                containerCnt = containerCnt,
                elementCnt = elementCnt,
                atomVersions = atomVersions,
                atomInputParamList = atomInputParamList
            )
            if (!projectId.isNullOrEmpty() && atomVersions.isNotEmpty()) {
                AtomUtils.checkModelAtoms(
                    projectCode = projectId,
                    atomVersions = atomVersions,
                    atomInputParamList = atomInputParamList,
                    inputTypeConfigMap = AtomUtils.getInputTypeConfigMap(taskCommonSettingConfig),
                    client = client
                )
            }
            DependOnUtils.checkRepeatedJobId(stage)
        }

        return metaSize
    }

    private fun checkStageReviewers(stage: Stage) {
        stage.resetBuildOption()
        if (stage.checkIn?.reviewGroups.isNullOrEmpty()) {
            throw ErrorCodeException(
                defaultMessage = "Stage(${stage.name})准入配置不正确",
                errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_NO_REVIEW_GROUP,
                params = arrayOf(stage.name ?: stage.id ?: "")
            )
        }
        stage.checkIn?.reviewGroups?.forEach { group ->
            if (group.reviewers.isEmpty()) throw ErrorCodeException(
                defaultMessage = "Stage(${stage.name})中审核组(${group.name})未配置审核人",
                errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_REVIEW_GROUP_NO_USER,
                params = arrayOf(stage.name!!, group.name)
            )
        }
        PipelineUtils.checkStageReviewParam(stage.checkIn?.reviewParams)

        stage.checkIn?.timeout = if (stage.checkIn?.timeout in 1..(Timeout.DEFAULT_STAGE_TIMEOUT_HOURS * 30)) {
            stage.checkIn?.timeout
        } else {
            Timeout.DEFAULT_STAGE_TIMEOUT_HOURS
        }
    }

    private fun checkElements(
        stage: Stage,
        containerCnt: MutableMap<String, Int>,
        elementCnt: MutableMap<String, Int>,
        atomVersions: MutableSet<StoreVersion>,
        atomInputParamList: MutableList<StoreParam>
    ): Int /* MetaSize*/ {
        var metaSize = 0
        stage.containers.forEach { c ->
            // 判断job下task数量是否超过系统限制
            metaSize += c.elements.size
            if (c.elements.size > jobCommonSettingConfig.maxTaskNum) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE,
                    params = arrayOf(c.name, KEY_TASK, jobCommonSettingConfig.maxTaskNum.toString())
                )
            }
            val cCnt = containerCnt.computeIfPresent(c.getClassType()) { _, oldValue -> oldValue + 1 }
                ?: containerCnt.computeIfAbsent(c.getClassType()) { 1 } // 第一次时出现1次
            ContainerBizRegistrar.getPlugin(c)?.check(c, cCnt)
            Preconditions.checkTrue(
                condition = c.elements.isNotEmpty(),
                exception = ErrorCodeException(
                    defaultMessage = "流水线: Model信息不完整，Stage[{0}] Job[{1}]下没有插件",
                    errorCode = ProcessMessageCode.ERROR_EMPTY_JOB, params = arrayOf(stage.name!!, c.name)
                )
            )
            c.elements.forEach { e ->
                val eCnt = elementCnt.computeIfPresent(e.getAtomCode()) { _, oldValue -> oldValue + 1 }
                    ?: elementCnt.computeIfAbsent(e.getAtomCode()) { 1 } // 第一次时出现1次
                ElementBizRegistrar.getPlugin(e)?.check(e, eCnt)
                addAtomInputDataInfo(e, atomVersions, atomInputParamList)
            }
        }
        return metaSize + stage.containers.size
    }

    @Suppress("UNCHECKED_CAST")
    private fun addAtomInputDataInfo(
        e: Element,
        atomVersions: MutableSet<StoreVersion>,
        atomInputParamList: MutableList<StoreParam>
    ) {
        var version = e.version
        if (version.isBlank()) {
            version = "1.*"
        }
        val atomCode = e.getAtomCode()
        atomVersions.add(
            StoreVersion(
                storeCode = atomCode,
                storeName = e.name,
                version = version,
                historyFlag = AtomUtils.isHisAtomElement(e)
            )
        )
        // 获取插件的输入参数
        val atomInputDataMap = when (e) {
            is MarketBuildAtomElement -> {
                e.data[KEY_INPUT]
            }
            is MarketBuildLessAtomElement -> {
                e.data[KEY_INPUT]
            }
            else -> {
                // 获取老插件的输入参数
                val baseFields = Element::class.java.declaredFields.filter { it.name != "Companion" }
                val filterFieldNames = baseFields.map { it.name }.toMutableSet()
                filterFieldNames.addAll(
                    setOf(
                        "@type",
                        "classType",
                        "elementEnable",
                        "atomCode",
                        "taskAtom"
                    )
                )
                JsonUtil.toMap(e).filter { it.key !in filterFieldNames }
            }
        } as? Map<String, Any?>
        if (atomInputDataMap != null) {
            atomInputParamList.add(
                StoreParam(
                    storeCode = atomCode,
                    storeName = e.name,
                    version = version,
                    inputParam = atomInputDataMap
                )
            )
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

        private fun isThirdPartyAgentEmpty(vmBuildContainer: VMBuildContainer): Boolean {
            // Old logic
            if (vmBuildContainer.thirdPartyAgentId.isNullOrBlank() &&
                vmBuildContainer.thirdPartyAgentEnvId.isNullOrBlank()) {
                // New logic
                val dispatchType = vmBuildContainer.dispatchType ?: return true
                return when (dispatchType.buildType()) {
                    BuildType.THIRD_PARTY_AGENT_ID, BuildType.THIRD_PARTY_AGENT_ENV -> dispatchType.value.isBlank()
                    BuildType.WINDOWS -> false
                    else -> true
                }
            }

            return false
        }
    }

    /**
     * When edit the pipeline, it needs to check if the element delete
     */
    private fun Model.elementExist(originElement: Element): Boolean {
        stages.forEach { s ->
            s.containers.forEach { c ->
                if (loopFind(c.elements, originElement)) {
                    return true
                }
            }
        }
        return false
    }

    private fun loopFind(elements: List<Element>, originElement: Element): Boolean {
        elements.forEach { e ->
            if (e.stepId?.let { it == originElement.stepId } == true || e.id == originElement.id) {
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
        existModel.stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    deletePrepare(sourceModel, e, param)
                }
            }
        }
    }

    private fun deletePrepare(sourceModel: Model?, originElement: Element, param: BeforeDeleteParam) {
        if (sourceModel == null || !sourceModel.elementExist(originElement)) {
            logger.info("The element(${originElement.name}/${originElement.id}) is delete")
            ElementBizRegistrar.getPlugin(originElement)?.beforeDelete(originElement, param)
        } else {
            logger.info("The element(${originElement.name}/${originElement.id}) is not delete")
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

    override fun checkJob(
        jobContainer: Container,
        projectId: String,
        pipelineId: String,
        userId: String,
        finallyStage: Boolean
    ) {
        if (jobContainer is VMBuildContainer && jobContainer.baseOS == VMBaseOS.WINDOWS) {
            if (isThirdPartyAgentEmpty(jobContainer)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NO_PUBLIC_WINDOWS_BUILDER,
                    defaultMessage = "请设置Windows构建机"
                )
            }
        }

        checkJobCondition(finallyStage = finallyStage, jobContainer = jobContainer)
    }

    private fun checkJobCondition(finallyStage: Boolean, jobContainer: Container) {

        val jobControlOption = when (jobContainer) {
            is VMBuildContainer -> jobContainer.jobControlOption
            is NormalContainer -> jobContainer.jobControlOption
            else -> null
        }

        if (jobControlOption?.runCondition == null) {
            return
        }

        // 非finallyStage下不允许有finallyStageJobRunConditionSet下的条件
        if (finallyStage) {
            if (!finallyStageJobRunConditionSet.contains(jobControlOption.runCondition)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_FINALLY_STAGE_JOB_CONDITION,
                    defaultMessage = "流水线: [{0}]下的Job运行条件配置错误: {1}",
                    params = arrayOf(jobContainer.name, jobControlOption.runCondition.name)
                )
            }
            return
        } else {
            if (!normalStageJobRunConditionSet.contains(jobControlOption.runCondition)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NORMAL_STAGE_JOB_CONDITION,
                    defaultMessage = "流水线: [{0}]下的Job运行条件配置错误: {1}",
                    params = arrayOf(jobContainer.name, jobControlOption.runCondition.name)
                )
            }
        }

        if (jobControlOption.runCondition == JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN ||
            jobControlOption.runCondition == JobRunCondition.CUSTOM_VARIABLE_MATCH
        ) {
            if (jobControlOption.customVariables == null ||
                jobControlOption.customVariables!!.isEmpty()
            ) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_NO_PARAM_IN_JOB_CONDITION,
                    defaultMessage = "请设置Job运行条件时的自定义变量"
                )
            }
        }
    }

    private val finallyStageJobRunConditionSet = setOf(
        JobRunCondition.PREVIOUS_STAGE_CANCEL,
        JobRunCondition.PREVIOUS_STAGE_FAILED,
        JobRunCondition.PREVIOUS_STAGE_SUCCESS,
        JobRunCondition.STAGE_RUNNING
    )

    private val normalStageJobRunConditionSet = setOf(
        JobRunCondition.CUSTOM_CONDITION_MATCH,
        JobRunCondition.CUSTOM_VARIABLE_MATCH,
        JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
        JobRunCondition.STAGE_RUNNING
    )
}

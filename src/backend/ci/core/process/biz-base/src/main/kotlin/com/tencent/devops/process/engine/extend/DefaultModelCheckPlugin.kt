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
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.plugin.load.ContainerBizRegistrar
import com.tencent.devops.process.plugin.load.ElementBizRegistrar
import com.tencent.devops.process.pojo.config.JobCommonSettingConfig
import com.tencent.devops.process.pojo.config.PipelineCommonSettingConfig
import com.tencent.devops.process.pojo.config.StageCommonSettingConfig
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import com.tencent.devops.process.utils.DependOnUtils
import com.tencent.devops.process.utils.KEY_JOB
import com.tencent.devops.process.utils.KEY_STAGE
import com.tencent.devops.process.utils.KEY_TASK
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PROJECT_NAME
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

        val trigger = stages.getOrNull(0)
            ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB)
        // 检查触发容器
        val paramsMap = checkTriggerContainer(trigger)
        val contextMap = paramsMap.mapValues { it.value.defaultValue.toString() }
        val elementCnt = mutableMapOf<String, Int>()
        val containerCnt = mutableMapOf<String, Int>()
        val lastPosition = model.stages.size - 1
        model.stages.forEachIndexed { nowPosition, stage ->
            val containers = stage.containers
            // 判断stage下container数量是否超过系统限制
            if (containers.size > stageCommonSettingConfig.maxJobNum) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE,
                    params = arrayOf(
                        stage.name ?: "stage_$nowPosition",
                        KEY_JOB,
                        stageCommonSettingConfig.maxJobNum.toString()
                    )
                )
            }
            if (containers.isEmpty()) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB)
            }

            if (stage.finally) { // finallyStage只能存在于最后一个
                if (nowPosition < lastPosition) {
                    throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_FINALLY_STAGE)
                }
            }

            // #4531 检查stage审核组配置是否符合要求
            stage.checkStageReviewers()

            val atomVersions = mutableSetOf<StoreVersion>()
            val atomInputParamList = mutableListOf<StoreParam>()

            metaSize += stage.checkJob(
                containerCnt = containerCnt,
                elementCnt = elementCnt,
                atomVersions = atomVersions,
                contextMap = contextMap,
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

    private fun Stage.checkStageReviewers() {
        if (stageControlOption?.manualTrigger != true && checkIn?.manualTrigger != true) {
            return
        }
        resetBuildOption()
        if (checkIn?.reviewGroups.isNullOrEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_NO_REVIEW_GROUP,
                params = arrayOf(name ?: id ?: "")
            )
        }
        checkIn?.reviewGroups?.forEach { group ->
            if (group.reviewers.isEmpty()) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_REVIEW_GROUP_NO_USER,
                params = arrayOf(name!!, group.name)
            )
        }
        PipelineUtils.checkStageReviewParam(checkIn?.reviewParams)

        checkIn?.timeout = if (checkIn?.timeout in 1..(Timeout.DEFAULT_STAGE_TIMEOUT_HOURS * 30)) {
            checkIn?.timeout
        } else {
            Timeout.DEFAULT_STAGE_TIMEOUT_HOURS
        }
    }

    private fun Stage.checkJob(
        containerCnt: MutableMap<String, Int>,
        elementCnt: MutableMap<String, Int>,
        atomVersions: MutableSet<StoreVersion>,
        contextMap: Map<String, String>,
        atomInputParamList: MutableList<StoreParam>
    ): Int /* MetaSize*/ {
        var metaSize = 0
        containers.forEach { container ->

            checkMutexGroup(container = container, contextMap = contextMap)

            checkJobCondition(container = container, finallyStage = finally, contextMap = contextMap)

            // 判断job下task数量是否超过系统限制
            metaSize += container.elements.size
            if (container.elements.size > jobCommonSettingConfig.maxTaskNum) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_COMPONENT_NUM_TOO_LARGE,
                    params = arrayOf(container.name, KEY_TASK, jobCommonSettingConfig.maxTaskNum.toString())
                )
            }
            val cCnt = containerCnt.computeIfPresent(container.getClassType()) { _, oldValue -> oldValue + 1 }
                ?: containerCnt.computeIfAbsent(container.getClassType()) { 1 } // 第一次时出现1次
            ContainerBizRegistrar.getPlugin(container)?.check(container, cCnt)
            Preconditions.checkTrue(
                condition = container.elements.isNotEmpty(),
                exception = ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_EMPTY_JOB, params = arrayOf(name!!, container.name)
                )
            )
            container.elements.forEach { e ->
                container.checkElement(e, elementCnt, atomVersions, atomInputParamList, contextMap)
            }
        }
        return metaSize + containers.size
    }

    private fun Container.checkElement(
        element: Element,
        elementCnt: MutableMap<String, Int>,
        atomVersions: MutableSet<StoreVersion>,
        atomInputParamList: MutableList<StoreParam>,
        contextMap: Map<String, String>
    ) {
        val eCnt = elementCnt.computeIfPresent(element.getAtomCode()) { _, oldValue -> oldValue + 1 }
            ?: elementCnt.computeIfAbsent(element.getAtomCode()) { 1 } // 第一次时出现1次
        ElementBizRegistrar.getPlugin(element)?.check(element, eCnt)
        addAtomInputDataInfo(element, atomVersions, atomInputParamList)

        checkElementTimeoutVar(container = this, element = element, contextMap = contextMap)
    }

    override fun checkElementTimeoutVar(container: Container, element: Element, contextMap: Map<String, String>) {
        if (!element.additionalOptions?.timeoutVar.isNullOrBlank()) {
            val obj = Timeout.decTimeout(timeoutVar = element.additionalOptions?.timeoutVar, contextMap = contextMap)
            if (obj.change && obj.replaceByVar) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TASK_TIME_OUT_PARAM_VAR,
                    params = arrayOf(
                        container.name, // Job名称
                        element.name, // 插件名称
                        element.additionalOptions!!.timeoutVar!!, // 互斥组超时配置项的变量字符串
                        obj.beforeChangeStr!!, // 变量替换后的值
                        Timeout.MAX_MINUTES.toString() // 合理的最大值
                    )
                )
            }
            element.additionalOptions?.timeout = obj.minutes.toLong()
        } else { // 历史0值兼容
            element.additionalOptions?.timeoutVar = element.additionalOptions?.timeout.toString()
            val obj = Timeout.decTimeout(timeoutVar = element.additionalOptions?.timeoutVar, contextMap = contextMap)

            if (obj.change) {
                element.additionalOptions?.timeoutVar = obj.minutes.toString()
                element.additionalOptions?.timeout = obj.minutes.toLong()
                logger.info(
                    "BKSystemMonitor|[${contextMap[PROJECT_NAME]}]|[${contextMap[PIPELINE_ID]}]" +
                            "|bad timeout: ${obj.beforeChangeStr}"
                )
            }
        }
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

    open fun checkTriggerContainer(trigger: Stage): Map<String /* 流水线变量名 */, BuildFormProperty> {
        if (trigger.containers.size != 1) {
            logger.warn("The trigger stage contain more than one container (${trigger.containers.size})")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ONLY_ONE_TRIGGER_JOB_IN_PIPELINE
            )
        }
        val triggerContainer = (trigger.containers.getOrNull(0) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
        )) as TriggerContainer
        return PipelineUtils.checkPipelineParams(triggerContainer.params)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultModelCheckPlugin::class.java)
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

    override fun checkMutexGroup(container: Container, contextMap: Map<String, String>) {

        val mutexGroup = when (container) {
            is VMBuildContainer -> container.mutexGroup
            is NormalContainer -> container.mutexGroup
            else -> return
        }

        if (mutexGroup != null) {
            val obj = Timeout.decTimeout(timeoutVar = mutexGroup.timeoutVar, contextMap = contextMap)
            if (obj.change && obj.replaceByVar) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_JOB_MUTEX_TIME_OUT_PARAM_VAR,
                    params = arrayOf(
                        container.name, // job名称
                        mutexGroup.mutexGroupName!!, // 互斥组名称
                        mutexGroup.timeoutVar!!, // 互斥组超时配置项的变量字符串
                        obj.beforeChangeStr!!, // 变量替换后的值
                        Timeout.MAX_MINUTES.toString() // 合理的最大值
                    )
                )
            }
            mutexGroup.timeout = obj.minutes
        }
    }

    override fun checkJobCondition(container: Container, finallyStage: Boolean, contextMap: Map<String, String>) {

        val jobControlOption = when (container) {
            is VMBuildContainer -> container.jobControlOption
            is NormalContainer -> container.jobControlOption
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
                    params = arrayOf(container.name, jobControlOption.runCondition.name)
                )
            }
        } else if (!normalStageJobRunConditionSet.contains(jobControlOption.runCondition)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NORMAL_STAGE_JOB_CONDITION,
                params = arrayOf(container.name, jobControlOption.runCondition.name)
            )
        } else if (customVarJobRunConditionSet.contains(jobControlOption.runCondition)) {
            if (jobControlOption.customVariables.isNullOrEmpty()) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_NO_PARAM_IN_JOB_CONDITION)
            }
        }

        if (!jobControlOption.timeoutVar.isNullOrBlank()) {
            val obj = Timeout.decTimeout(timeoutVar = jobControlOption.timeoutVar, contextMap = contextMap)
            if (obj.change && obj.replaceByVar) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_JOB_TIME_OUT_PARAM_VAR,
                    params = arrayOf(
                        container.name, // Job名称
                        jobControlOption.timeoutVar!!, // 超时配置项的变量字符串
                        obj.beforeChangeStr!!, // 变量替换后的值
                        Timeout.MAX_MINUTES.toString() // 合理的最大值
                    )
                )
            }
            jobControlOption.timeout = obj.minutes
        } else { // 历史0值兼容
            jobControlOption.timeoutVar = jobControlOption.timeout.toString()
            val obj = Timeout.decTimeout(timeoutVar = jobControlOption.timeoutVar, contextMap = contextMap)
            if (obj.change) {
                jobControlOption.timeoutVar = obj.minutes.toString()
                jobControlOption.timeout = obj.minutes
                logger.info(
                    "BKSystemMonitor|[${contextMap[PROJECT_NAME]}]|[${contextMap[PIPELINE_ID]}]" +
                            "|bad timeout: ${obj.beforeChangeStr}"
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

    private val customVarJobRunConditionSet = setOf(
        JobRunCondition.CUSTOM_VARIABLE_MATCH,
        JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN
    )
}

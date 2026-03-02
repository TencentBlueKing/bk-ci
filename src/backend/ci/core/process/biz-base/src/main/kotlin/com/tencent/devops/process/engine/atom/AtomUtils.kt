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

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_ELEMENT_CAN_PAUSE_BEFORE_RUN_NOT_SUPPORT
import com.tencent.devops.common.api.constant.CommonMessageCode.ELEMENT_NOT_SUPPORT_TRANSFER
import com.tencent.devops.common.api.constant.CommonMessageCode.TEMPLATE_PLUGIN_NOT_ALLOWED_USE
import com.tencent.devops.common.api.constant.KEY_CODE_EDITOR
import com.tencent.devops.common.api.constant.KEY_DEFAULT
import com.tencent.devops.common.api.constant.KEY_INPUT
import com.tencent.devops.common.api.constant.KEY_TEXTAREA
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspectElement
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspectModel
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferJoinPoint
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomEnvResource
import com.tencent.devops.store.pojo.atom.AtomCodeVersionReqItem
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.StoreParam
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersion
import java.util.LinkedList

object AtomUtils {

    /**
     * 解析出Container中的市场插件，如果市场插件相应版本找不到就抛出异常
     */
    @Suppress("ComplexMethod")
    fun parseContainerMarketAtom(
        container: Container,
        task: PipelineBuildTask,
        client: Client,
        buildLogPrinter: BuildLogPrinter
    ): MutableMap<String, String> {
        val atoms = mutableMapOf<String, String>()
        val atomVersions = getAtomVersions(container)
        if (atomVersions.isEmpty()) {
            // 如果job容器内没有新插件，则直接返回
            return atoms
        }
        // 批量获取插件运行时信息
        val atomRunInfoMap = batchGetAtomInfo(client = client, task = task, atomVersions = atomVersions)
        for (element in container.elements) {
            if (isHisAtomElement(element)) {
                continue
            }
            var version = element.version
            if (version.isBlank()) {
                version = "1.*"
            }
            val atomCode = element.getAtomCode()
            val atomRunInfo = atomRunInfoMap?.get("$atomCode:$version")
            if (atomRunInfo == null) {
                val message = "Can't found task($atomCode:$version):${element.name}."
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ProcessMessageCode.ERROR_ATOM_NOT_FOUND.toInt(),
                    errorMsg = message,
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    taskId = task.taskId
                )
            }
            // 从原始 JOB_TYPE（可能为 JSON）解析出所有 JobTypeEnum，判断插件是否有权限在该 job 环境下运行
            val allJobTypes = JobTypeEnum.parseAllFromRaw(atomRunInfo.jobType)
            val hasBuildEnvType = allJobTypes.any { it.isBuildEnv() }
            val hasBuildLessType = allJobTypes.any { !it.isBuildEnv() }
            val jobRunFlag = when {
                allJobTypes.isEmpty() -> false
                container is VMBuildContainer -> hasBuildEnvType ||
                    (hasBuildLessType && atomRunInfo.buildLessRunFlag == true)
                container is NormalContainer -> hasBuildLessType
                else -> false
            }
            if (!jobRunFlag) {
                throw BuildTaskException(
                    errorType = ErrorType.USER,
                    errorCode = ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID.toInt(),
                    errorMsg = I18nUtil.getCodeLanMessage(
                        messageCode = ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID,
                        params = arrayOf(element.name)
                    ),
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    taskId = task.taskId
                )
            }

            buildLogPrinter.addLine(
                buildId = task.buildId,
                message = "Prepare ${element.name}(${atomRunInfo.atomName})",
                tag = task.taskId,
                containerHashId = task.containerHashId,
                executeCount = task.executeCount ?: 1,
                jobId = null,
                stepId = task.stepId
            )
            atoms[atomCode] = atomRunInfo.initProjectCode
        }
        return atoms
    }

    private fun batchGetAtomInfo(
        client: Client,
        task: PipelineBuildTask,
        atomVersions: MutableSet<StoreVersion>
    ): Map<String, AtomRunInfo>? {

        val atomRunInfoResult = try {
            client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(task.projectId, atomVersions)
        } catch (ignored: Exception) {
            Result<Map<String, AtomRunInfo>?>(
                status = ProcessMessageCode.ERROR_ATOM_NOT_FOUND.toInt(),
                message = ignored.message
            )
        }

        if (atomRunInfoResult.isNotOk()) {
            throw BuildTaskException(
                errorType = ErrorType.USER,
                errorCode = ProcessMessageCode.ERROR_ATOM_NOT_FOUND.toInt(),
                errorMsg = atomRunInfoResult.message ?: "query tasks error",
                pipelineId = task.pipelineId,
                buildId = task.buildId,
                taskId = task.taskId
            )
        }
        return atomRunInfoResult.data
    }

    private fun getAtomVersions(container: Container): MutableSet<StoreVersion> {
        val atomVersions = mutableSetOf<StoreVersion>()
        container.elements.forEach nextOne@{ element ->
            if (isHisAtomElement(element)) {
                return@nextOne
            }
            var version = element.version
            if (version.isBlank()) {
                version = "1.*"
            }
            val atomCode = element.getAtomCode()
            atomVersions.add(
                StoreVersion(
                    storeCode = atomCode,
                    storeName = element.name,
                    version = version,
                    historyFlag = false
                )
            )
        }
        return atomVersions
    }

    fun isHisAtomElement(element: Element) =
        element !is MarketBuildAtomElement && element !is MarketBuildLessAtomElement

    fun getInputTypeConfigMap(taskCommonSettingConfig: TaskCommonSettingConfig): Map<String, Int> {
        val inputTypeConfigMap = mutableMapOf(
            KEY_INPUT to taskCommonSettingConfig.maxInputComponentSize,
            KEY_TEXTAREA to taskCommonSettingConfig.maxTextareaComponentSize,
            KEY_CODE_EDITOR to taskCommonSettingConfig.maxCodeEditorComponentSize,
            KEY_DEFAULT to taskCommonSettingConfig.maxDefaultInputComponentSize
        )
        val multipleInputComponents = taskCommonSettingConfig.multipleInputComponents.split(",")
        multipleInputComponents.forEach {
            inputTypeConfigMap[it] = taskCommonSettingConfig.maxMultipleInputComponentSize
        }
        return inputTypeConfigMap
    }

    fun checkTemplateRealVersionAtoms(
        codeVersions: Set<AtomCodeVersionReqItem>,
        userId: String,
        client: Client
    ) {
        val atomInfos = client.get(ServiceAtomResource::class)
            .getAtomInfos(
                codeVersions = codeVersions
            ).data
        atomInfos?.forEach {
            val atomStatus = AtomStatusEnum.getAtomStatus(it.atomStatus!!.toInt())
            if (atomStatus != AtomStatusEnum.RELEASED.name) {
                throw ErrorCodeException(
                    errorCode = TEMPLATE_PLUGIN_NOT_ALLOWED_USE,
                    params = arrayOf(
                        it.atomName,
                        it.version,
                        AtomStatusEnum.valueOf(atomStatus).getI18n(I18nUtil.getLanguage(userId))
                    )
                )
            }
        }
    }

    fun checkModelAtoms(
        projectCode: String,
        atomVersions: Set<StoreVersion>,
        atomInputParamList: MutableList<StoreParam>,
        inputTypeConfigMap: Map<String, Int>,
        client: Client
    ) {
        if (atomVersions.isEmpty()) return
        val atomRunInfoMap = client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(
            projectCode = projectCode,
            atomVersions = atomVersions
        ).data
        atomInputParamList.forEach { storeParam ->
            val atomRunInfo = atomRunInfoMap?.get("${storeParam.storeCode}:${storeParam.version}") ?: return@forEach
            checkAtomServiceScopeForChannel(atomRunInfo, storeParam.storeName)
            validateAtomParam(
                atomParamDataMap = storeParam.inputParam,
                atomRunInfo = atomRunInfo,
                inputTypeConfigMap = inputTypeConfigMap,
                atomName = storeParam.storeName
            )
        }
    }

    /**
     * 检查插件在当前渠道下的服务范围是否允许使用，不满足时抛出 [ErrorCodeException]。
     */
    private fun checkAtomServiceScopeForChannel(atomRunInfo: AtomRunInfo, atomName: String) {
        val serviceScope = atomRunInfo.serviceScope
        if (serviceScope.isNullOrEmpty()) return
        val channelCode = ChannelCode.getRequestChannelCode()
        val requiredScope = when (channelCode) {
            ChannelCode.CREATIVE_STREAM -> ServiceScopeEnum.CREATIVE_STREAM.name
            else -> ServiceScopeEnum.PIPELINE.name
        }
        require(serviceScope.contains(requiredScope)) {
            ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID,
                params = arrayOf(atomName),
                defaultMessage = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID,
                    params = arrayOf(atomName)
                ),
            )
        }
    }

    private fun validateAtomParam(
        atomParamDataMap: Map<String, Any?>?,
        atomRunInfo: AtomRunInfo,
        inputTypeConfigMap: Map<String, Int>,
        atomName: String
    ) {
        if (atomParamDataMap?.isNotEmpty() == true) {
            val inputTypeInfos = atomRunInfo.inputTypeInfos
            atomParamDataMap.forEach { (paramName, paramValue) ->
                if (paramValue == null) {
                    return@forEach
                }
                val inputType = inputTypeInfos?.get(paramName)
                val maxInputTypeSize = inputTypeConfigMap[inputType] ?: inputTypeConfigMap[KEY_DEFAULT]
                if (paramValue.toString().length > maxInputTypeSize!!) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_ATOM_PARAM_VALUE_TOO_LARGE,
                        params = arrayOf(atomName, paramName, maxInputTypeSize.toString())
                    )
                }
            }
        }
    }

    fun getModelElementSensitiveParamInfos(
        projectId: String,
        model: Model,
        client: Client
    ): Map<String, String>? {
        val atomVersions = mutableSetOf<StoreVersion>()
        model.stages.forEach { stage ->
            stage.containers.forEach {
                atomVersions.addAll(getAtomVersions(it))
            }
        }
        if (atomVersions.isEmpty()) return null
        val result = client.get(ServiceMarketAtomEnvResource::class).batchGetAtomSensitiveParamInfos(
            projectCode = projectId,
            atomVersions = atomVersions
        )
        return if (result.isNotOk()) {
            null
        } else {
            result.data
        }
    }

    // YAML2MODEL 时使用
    fun checkElementCanPauseBeforeRun(
        client: Client,
        projectId: String,
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ): LinkedList<IPipelineTransferAspect> {
        val elementUse = mutableSetOf<StoreVersion>()
        aspects.add(
            object : IPipelineTransferAspectElement {
                override fun after(jp: PipelineTransferJoinPoint) {
                    if (jp.modelElement() != null && jp.modelElement()?.additionalOptions?.pauseBeforeExec == true) {
                        val element = jp.modelElement()!!
                        var version = element.version
                        if (version.isBlank()) {
                            version = "1.*"
                        }
                        val atomCode = element.getAtomCode()
                        elementUse.add(
                            StoreVersion(
                                storeCode = atomCode,
                                storeName = element.name,
                                version = version,
                                historyFlag = isHisAtomElement(element)
                            )
                        )
                    }
                }
            }
        )

        aspects.add(
            object : IPipelineTransferAspectModel {
                override fun after(jp: PipelineTransferJoinPoint) {
                    if (jp.model() != null && elementUse.isNotEmpty()) {
                        val atomRunInfoResult = kotlin.runCatching {
                            client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(
                                projectCode = projectId,
                                atomVersions = elementUse
                            ).data
                        }.getOrNull() ?: return
                        // 筛选出canPauseBeforeRun不为true的插件，然后抛错给用户，因为这些插件不让执行前暂停
                        val check = atomRunInfoResult.filter {
                            it.value.canPauseBeforeRun != true
                        }
                        if (check.isNotEmpty()) {
                            throw PipelineTransferException(
                                ELEMENT_NOT_SUPPORT_TRANSFER,
                                arrayOf(check.values.joinToString("\n- ", "- ") {
                                    I18nUtil.getCodeLanMessage(
                                        BK_ELEMENT_CAN_PAUSE_BEFORE_RUN_NOT_SUPPORT,
                                        params = arrayOf("${it.atomName}[${it.atomCode}]")
                                    )
                                })
                            )
                        }
                    }
                }
            }
        )
        return aspects
    }
}
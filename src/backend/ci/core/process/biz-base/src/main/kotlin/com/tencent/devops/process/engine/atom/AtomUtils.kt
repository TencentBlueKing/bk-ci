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
        buildLogPrinter: BuildLogPrinter,
        channelCode: ChannelCode = ChannelCode.BS,
        stageName: String = ""
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
            // 服务范围校验(创作流/流水线) + 构建环境匹配校验，均属于「插件与该 Job 运行环境不匹配」
            if (!isAtomServiceScopeAllowed(atomRunInfo = atomRunInfo, channelCode = channelCode) ||
                !isAtomJobTypeMatchContainer(atomRunInfo = atomRunInfo, container = container)) {
                throw buildAtomRunInvalidException(
                    task = task,
                    stageName = stageName,
                    jobName = container.name,
                    atomName = element.name
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
        atomCheckParams: List<AtomCheckParam>,
        inputTypeConfigMap: Map<String, Int>,
        client: Client
    ) {
        if (atomVersions.isEmpty()) return
        // 复用同一次批量查询结果，服务范围/构建环境/参数三类校验共享，避免额外远程调用
        val atomRunInfoMap = client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(
            projectCode = projectCode,
            atomVersions = atomVersions
        ).data
        // 保存/校验阶段：渠道取自请求上下文
        val channelCode = ChannelCode.getRequestChannelCode()
        atomCheckParams.forEach { checkParam ->
            val storeParam = checkParam.storeParam
            val atomRunInfo = atomRunInfoMap?.get("${storeParam.storeCode}:${storeParam.version}") ?: return@forEach
            // 服务范围校验：只允许保存支持当前渠道(创作流/流水线)服务范围的插件
            // 构建环境匹配校验：插件 jobType 需与其所在容器(有/无构建环境)匹配
            if (!isAtomServiceScopeAllowed(atomRunInfo = atomRunInfo, channelCode = channelCode) ||
                !isAtomJobTypeMatch(atomRunInfo = atomRunInfo, containerEnvType = checkParam.containerEnvType)) {
                throw atomCheckException(checkParam)
            }
            validateAtomParam(
                atomParamDataMap = storeParam.inputParam,
                atomRunInfo = atomRunInfo,
                inputTypeConfigMap = inputTypeConfigMap,
                atomName = storeParam.storeName
            )
        }
    }

    /**
     * 构造保存阶段插件校验失败异常，提示中明确指出不满足运行环境要求的 Stage / Job 及插件名。
     */
    private fun atomCheckException(checkParam: AtomCheckParam): ErrorCodeException {
        val messageCode = ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID
        val params = arrayOf(checkParam.stageName, checkParam.jobName, checkParam.storeParam.storeName)
        return ErrorCodeException(
            errorCode = messageCode,
            params = params,
            defaultMessage = I18nUtil.getCodeLanMessage(messageCode = messageCode, params = params)
        )
    }

    /**
     * 渠道(ChannelCode)到插件服务范围(ServiceScopeEnum)的映射关系。
     *
     * 这是「渠道-服务范围」唯一的扩展点：未来新增需要做服务范围隔离的渠道时，
     * 只需在此处补充对应分支即可，调用方无需改动。
     */
    private fun resolveRequiredServiceScope(channelCode: ChannelCode): ServiceScopeEnum = when (channelCode) {
        ChannelCode.CREATIVE_STREAM -> ServiceScopeEnum.CREATIVE_STREAM
        else -> ServiceScopeEnum.PIPELINE
    }

    /**
     * 判断插件是否允许在指定渠道(创作流/流水线)下运行。
     *
     * 当插件未声明服务范围([AtomRunInfo.serviceScope] 为空)时默认放行，以保证存量插件逻辑不受影响；
     * 仅当插件显式声明了服务范围且不包含当前渠道所需范围时才拒绝运行。
     */
    private fun isAtomServiceScopeAllowed(atomRunInfo: AtomRunInfo, channelCode: ChannelCode): Boolean {
        val serviceScope = atomRunInfo.serviceScope
        if (serviceScope.isNullOrEmpty()) return true
        val requiredScope = resolveRequiredServiceScope(channelCode).name
        return serviceScope.any { it.equals(requiredScope, ignoreCase = true) }
    }

    /**
     * Job 容器的构建环境类型，作为 jobType 匹配校验的抽象输入，
     * 使「校验逻辑」与「具体 Container 类型」解耦，运行时/保存时可复用同一套判断。
     */
    enum class AtomContainerEnvType {
        BUILD_ENV, // 有编译环境（对应 VMBuildContainer）
        BUILD_LESS, // 无编译环境（对应 NormalContainer）
        UNKNOWN // 其它容器（如触发器），不参与 jobType 匹配校验
    }

    /**
     * 保存阶段单个插件的校验上下文：插件参数 + 其所在 Stage/Job 名称 + 容器构建环境类型，
     * 其中 Stage/Job 名称用于在校验失败时给出「具体哪个 Job」的精确提示。
     */
    data class AtomCheckParam(
        val storeParam: StoreParam,
        val containerEnvType: AtomContainerEnvType,
        val stageName: String,
        val jobName: String
    )

    fun resolveContainerEnvType(container: Container): AtomContainerEnvType = when (container) {
        is VMBuildContainer -> AtomContainerEnvType.BUILD_ENV
        is NormalContainer -> AtomContainerEnvType.BUILD_LESS
        else -> AtomContainerEnvType.UNKNOWN
    }

    /**
     * 判断插件的 jobType 是否与给定的构建环境类型匹配。
     * - [AtomContainerEnvType.BUILD_ENV]：编译环境插件可运行；无构建环境插件需开启 buildLessRunFlag 才可运行。
     * - [AtomContainerEnvType.BUILD_LESS]：仅无编译环境插件可运行。
     * - [AtomContainerEnvType.UNKNOWN]：不做匹配校验，默认放行（保持历史逻辑）。
     */
    private fun isAtomJobTypeMatch(atomRunInfo: AtomRunInfo, containerEnvType: AtomContainerEnvType): Boolean {
        if (containerEnvType == AtomContainerEnvType.UNKNOWN) return true
        val allJobTypes = JobTypeEnum.resolveAllFromFields(atomRunInfo.jobType, atomRunInfo.jobTypeMap)
        if (allJobTypes.isEmpty()) return false
        val hasBuildEnvType = allJobTypes.any { it.isBuildEnv() }
        val hasBuildLessType = allJobTypes.any { !it.isBuildEnv() }
        return when (containerEnvType) {
            AtomContainerEnvType.BUILD_ENV ->
                hasBuildEnvType || (hasBuildLessType && atomRunInfo.buildLessRunFlag == true)
            AtomContainerEnvType.BUILD_LESS -> hasBuildLessType
            else -> true
        }
    }

    /**
     * 判断插件的 jobType 是否与当前 Job 容器匹配。
     */
    private fun isAtomJobTypeMatchContainer(atomRunInfo: AtomRunInfo, container: Container): Boolean =
        isAtomJobTypeMatch(atomRunInfo = atomRunInfo, containerEnvType = resolveContainerEnvType(container))

    private fun buildAtomRunInvalidException(
        task: PipelineBuildTask,
        stageName: String,
        jobName: String,
        atomName: String,
        messageCode: String = ProcessMessageCode.ERROR_ATOM_RUN_BUILD_ENV_INVALID
    ): BuildTaskException {
        // stageName 为空时回退到 stageId，保证提示中的 Stage 位不为空
        val params = arrayOf(stageName.ifBlank { task.stageId }, jobName, atomName)
        return BuildTaskException(
            errorType = ErrorType.USER,
            errorCode = messageCode.toInt(),
            errorMsg = I18nUtil.getCodeLanMessage(messageCode = messageCode, params = params),
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            taskId = task.taskId
        )
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
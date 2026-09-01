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
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.PipelineRunEnvOsChange
import com.tencent.devops.common.pipeline.pojo.PipelineRunEnvOsCheckParam
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.JobTemplateModel
import com.tencent.devops.common.pipeline.template.StageTemplateModel
import com.tencent.devops.common.pipeline.template.StepTemplateModel
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
import org.slf4j.LoggerFactory
import java.util.LinkedList

object AtomUtils {

    private val logger = LoggerFactory.getLogger(AtomUtils::class.java)

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

    /**
     * 解析出编排中所有市场插件的版本信息，用于批量查询插件运行时信息。
     */
    fun getModelAtomVersions(model: Model): Set<StoreVersion> {
        val atomVersions = mutableSetOf<StoreVersion>()
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                atomVersions.addAll(getAtomVersions(container))
            }
        }
        return atomVersions
    }

    /**
     * 解析出模板编排中所有市场插件的版本信息，各模板类型的层级不同，逐类展开到 Container 后复用同一份解析。
     *
     * 与 [com.tencent.devops.common.pipeline.utils.ModelUtils.getTemplateModelAtoms] 覆盖的类型保持一致，
     * 新增模板类型时两处需同步。
     */
    fun getTemplateModelAtomVersions(templateModel: ITemplateModel): Set<StoreVersion> = when (templateModel) {
        is Model -> getModelAtomVersions(templateModel)
        is StageTemplateModel -> templateModel.stages.flatMap { it.containers }.flatMapTo(mutableSetOf()) {
            getAtomVersions(it)
        }
        is JobTemplateModel -> templateModel.containers.flatMapTo(mutableSetOf()) { getAtomVersions(it) }
        is StepTemplateModel -> getAtomVersions(templateModel.container)
        else -> emptySet()
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
        client: Client,
        runEnvOsCheckParam: PipelineRunEnvOsCheckParam? = null
    ) {
        if (atomVersions.isEmpty()) return
        // 复用同一次批量查询结果，服务范围/构建环境/参数/运行环境操作系统四类校验共享，避免额外远程调用
        val atomRunInfoMap = client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(
            projectCode = projectCode,
            atomVersions = atomVersions
        ).data
        // 服务范围校验的渠道口径不变，仍取自请求上下文
        val channelCode = ChannelCode.getRequestChannelCode()
        // 不适配插件在本次调用内收集后统一抛出
        val osIncompatibleAtoms = mutableListOf<OsIncompatibleAtom>()
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
            // 该插件运行所在节点的操作系统，无从确定时为空，即本项校验跳过
            val targetOs = runEnvOsCheckParam?.let {
                resolveRunEnvOs(
                    settingRunEnvOs = it.settingRunEnvOsChange?.currentOs,
                    jobRunEnvOs = checkParam.jobRunEnvOs
                )
            }
            if (runEnvOsCheckParam != null && targetOs != null) {
                findOsIncompatibleAtom(
                    atomRunInfo = atomRunInfo,
                    atomCode = storeParam.storeCode,
                    atomName = storeParam.storeName,
                    version = storeParam.version,
                    jobName = checkParam.jobName,
                    containerEnvType = checkParam.containerEnvType,
                    osJobTypeName = runEnvOsCheckParam.osJobTypeName,
                    targetOs = targetOs
                )?.let { osIncompatibleAtoms.add(it) }
            }
        }
        if (runEnvOsCheckParam != null && osIncompatibleAtoms.isNotEmpty()) {
            checkOsIncompatibleIntroduced(
                runEnvOsCheckParam = runEnvOsCheckParam,
                incompatibleAtoms = osIncompatibleAtoms
            )
        }
    }

    /**
     * 解析插件运行所在节点的操作系统，即该与插件声明比对的目标。
     *
     * 这是「目标操作系统」唯一的取值规则，编排校验、设置校验、豁免基准三处都经由此处，
     * 三者口径一旦分叉，同一个组合就会因两侧算出的目标不同而被误判。
     *
     * [settingRunEnvOs] 为运行环境由设置指定的渠道(如创作流)所指定的操作系统，该环境适用于编排中所有 Job，
     * 优先于 Job 自身的声明：这类渠道的 Job 并不自行决定跑在哪台机器上。
     * 其余渠道取 [jobRunEnvOs]，即 Job 自身声明的构建环境操作系统。两者都为空表示无从确定，跳过校验。
     */
    private fun resolveRunEnvOs(settingRunEnvOs: OS?, jobRunEnvOs: OS?): OS? = settingRunEnvOs ?: jobRunEnvOs

    /**
     * 剔除基准编排中已存在的组合后，若仍有本次新引入的不适配插件则抛出异常。
     *
     * 见 [PipelineRunEnvOsCheckParam.exemptedRunEnvOsAtomKeys]：存量编排中的不适配组合不阻断本次保存，
     * 仅拦本次新引入的。豁免集合在此处才首次读取，未发现不适配项时不会产生任何查询。
     */
    private fun checkOsIncompatibleIntroduced(
        runEnvOsCheckParam: PipelineRunEnvOsCheckParam,
        incompatibleAtoms: List<OsIncompatibleAtom>
    ) {
        val exemptedKeys = runEnvOsCheckParam.exemptedRunEnvOsAtomKeys.value
        val introducedAtoms = incompatibleAtoms.filterNot { exemptedKeys.contains(it.runEnvOsAtomKey) }
        if (introducedAtoms.isEmpty()) return
        val settingRunEnvOsChange = runEnvOsCheckParam.settingRunEnvOsChange
        throw if (settingRunEnvOsChange != null) {
            settingOsIncompatibleException(
                runEnvOsChange = settingRunEnvOsChange,
                incompatibleAtoms = introducedAtoms
            )
        } else {
            jobOsIncompatibleException(incompatibleAtoms = introducedAtoms)
        }
    }

    /**
     * 渠道到「插件操作系统声明所属 jobType」的映射。
     *
     * 与 [resolveRequiredServiceScope] 同为「渠道」的扩展点：插件的 OS 按 jobType 分别声明，
     * 未来新增需要做运行环境操作系统校验的渠道时，只需在此处补充对应分支。
     *
     * 保存校验与前端查询插件适用操作系统时共用该映射，保证两侧读取的是插件同一份声明。
     * 返回 [JobTypeEnum.AGENT] 表示该渠道沿用插件的遗留 OS 字段语义。
     */
    fun resolveOsJobType(channelCode: ChannelCode): JobTypeEnum = when (channelCode) {
        ChannelCode.CREATIVE_STREAM -> JobTypeEnum.CREATIVE_STREAM
        else -> JobTypeEnum.AGENT
    }

    /**
     * 该渠道的运行环境是否由流水线设置指定，而非由编排里的 Job 各自声明。
     *
     * 与 [resolveOsJobType] 同为「渠道」的扩展点，新增渠道时两处需同步补充。
     *
     * 返回 true 的渠道不得退而取 Job 的 baseOS 作为校验目标：创作流的 Job 跑在设置所选的创作环境上，
     * 其 baseOS 并非用户对运行系统的声明，而是 YAML 与编排互转时落下的默认值(见 DispatchTransfer.getBaseOs
     * 与 ScriptYmlUtils.formatRunsOn，创作流 Job 级 runs-on 为空时会得到 LINUX)，取它比对会得出错误结论。
     */
    fun isRunEnvSpecifiedBySetting(channelCode: ChannelCode) = channelCode == ChannelCode.CREATIVE_STREAM

    /**
     * 该渠道的流水线是否由平台自身维护，而非由用户编排。AM 为研发商店按插件模板生成的指标计算等内置流水线。
     *
     * 与 [resolveOsJobType]、[isRunEnvSpecifiedBySetting] 同为「渠道」的扩展点，新增渠道时一并考虑。
     *
     * 这类流水线的编排由平台生成与改写(如插件发布后自动改写其中的插件版本)，面向用户的保存期校验对它们
     * 没有意义：没有用户可以去修正编排，拦下来只会让平台流程失败。
     */
    fun isPlatformMaintainedChannel(channelCode: ChannelCode) = channelCode == ChannelCode.AM

    /**
     * 判断插件是否适用于运行环境的目标操作系统，不适用时返回明细项。
     *
     * 仅校验有编译环境的 Job：插件运行在运行环境的节点上，
     * 无编译环境插件与触发器插件不受运行环境操作系统约束。
     * 插件未声明当前渠道对应的操作系统范围时默认放行，保证存量插件逻辑不受影响。
     */
    private fun findOsIncompatibleAtom(
        atomRunInfo: AtomRunInfo,
        atomCode: String,
        atomName: String,
        version: String,
        jobName: String,
        containerEnvType: AtomContainerEnvType,
        osJobTypeName: String,
        targetOs: OS
    ): OsIncompatibleAtom? {
        if (containerEnvType != AtomContainerEnvType.BUILD_ENV) return null
        if (isBuildLessAtomRunInBuildEnv(atomRunInfo)) return null
        val supportedOsList = atomRunInfo.osMap?.get(osJobTypeName)
        if (supportedOsList.isNullOrEmpty()) return null
        if (supportedOsList.any { it.equals(targetOs.name, ignoreCase = true) }) return null
        return OsIncompatibleAtom(
            jobName = jobName,
            targetOs = targetOs,
            atomName = atomName,
            supportedOsList = supportedOsList,
            runEnvOsAtomKey = runEnvOsAtomKey(os = targetOs, atomCode = atomCode, version = version)
        )
    }

    /**
     * 解析 Job 自身声明的构建环境操作系统，无法确定唯一操作系统时返回空，该 Job 内的插件不做适配校验。
     *
     * [VMBuildContainer.baseOS] 是用户在编排里的声明，而非调度时的实际结果：未声明(null)、声明为不限制
     * ([VMBaseOS.ALL]，如第三方构建机环境未指定 agentSelector)、以及构建机由矩阵上下文决定时，
     * 都取不到唯一确定的操作系统。这些情况一律跳过而不做任何推断：这项校验会阻断保存，
     * 误拦一条能正常运行的流水线的代价远高于漏拦。
     */
    fun resolveJobRunEnvOs(container: Container): OS? {
        if (container !is VMBuildContainer) return null
        // 矩阵 Job 的构建机可由矩阵变量决定，baseOS 只是转换期的单一取值，不足以代表每种组合
        if (container.matrixGroupFlag == true) return null
        return when (container.baseOS) {
            VMBaseOS.LINUX -> OS.LINUX
            VMBaseOS.WINDOWS -> OS.WINDOWS
            VMBaseOS.MACOS -> OS.MACOS
            VMBaseOS.ALL, null -> null
        }
    }

    /**
     * 收集编排中「运行环境操作系统 + 插件版本」的组合，作为判断不适配项是否为本次保存新引入的基准。
     *
     * 传入的应是上一次落库的编排与其当时的运行环境操作系统，[settingRunEnvOs] 的含义见 [resolveRunEnvOs]。
     * 与前向校验共用同一次遍历，两侧的操作系统口径与版本归一方式因而必然一致，
     * 否则同一个组合会因两侧算出的 key 不同而被误判为新引入。
     */
    fun collectRunEnvOsAtomKeys(model: Model?, settingRunEnvOs: OS?): Set<String> {
        if (model == null) return emptySet()
        return collectRunEnvOsCheckItems(
            model = model,
            settingRunEnvOs = settingRunEnvOs,
            // 基准只用于回答「该组合此前是否已存在于编排中」，与其是否会被调度无关，
            // 故不跳过被禁用的 Stage / Job：把禁用的 Job 重新启用不该被当作新引入而拦下
            skipDisabled = false
        ).mapTo(mutableSetOf()) { it.runEnvOsAtomKey() }
    }

    /**
     * 仅校验编排中的插件是否都适用于运行环境的目标操作系统，不做服务范围/构建环境匹配/插件参数等其他校验。
     *
     * 供「保存流水线设置」的入口使用：设置里可能指定运行环境(如创作流的创作环境)，改设置即改变了插件的运行系统，
     * 而这类入口本身不经过编排校验。此处不复用 checkModelIntegrity，因为后者会带上编排的全部完整性校验，
     * 存量编排里与本次变更无关的历史问题会反过来阻断设置保存。
     *
     * 判定口径与 [checkModelAtoms] 完全一致：共用 [resolveRunEnvOs] 取目标操作系统、[findOsIncompatibleAtom]
     * 做判定、[checkOsIncompatibleIntroduced] 做存量豁免，jobType 同样取自入参而不按请求渠道自行解析。
     */
    fun checkModelRunEnvOs(
        projectCode: String,
        model: Model,
        runEnvOsCheckParam: PipelineRunEnvOsCheckParam,
        client: Client
    ) {
        val checkItems = collectRunEnvOsCheckItems(
            model = model,
            settingRunEnvOs = runEnvOsCheckParam.settingRunEnvOsChange?.currentOs,
            // 被禁用的 Stage / Job 运行时不会被调度，与 checkModelAtoms 一样跳过，保证保存校验不严于运行时
            skipDisabled = true
        )
        if (checkItems.isEmpty()) return
        // 编排中存在项目下已不可用的插件时该查询会抛异常。此处并未改动编排，不应因编排里与本次变更
        // 无关的历史问题而失败，故降级为跳过本项校验；编排保存入口仍会照常拦截这类插件
        val atomRunInfoMap = try {
            client.get(ServiceMarketAtomEnvResource::class).batchGetAtomRunInfos(
                projectCode = projectCode,
                atomVersions = checkItems.mapTo(mutableSetOf()) { it.storeVersion }
            ).data
        } catch (ignored: Throwable) {
            logger.warn("Failed to batch get atom run infos on run env os check|$projectCode", ignored)
            null
        } ?: return
        val incompatibleAtoms = checkItems.mapNotNull { checkItem ->
            val storeVersion = checkItem.storeVersion
            val atomRunInfo = atomRunInfoMap["${storeVersion.storeCode}:${storeVersion.version}"]
                ?: return@mapNotNull null
            findOsIncompatibleAtom(
                atomRunInfo = atomRunInfo,
                atomCode = storeVersion.storeCode,
                atomName = storeVersion.storeName,
                version = storeVersion.version,
                jobName = checkItem.jobName,
                // 收集时已限定为有编译环境的 Job
                containerEnvType = AtomContainerEnvType.BUILD_ENV,
                osJobTypeName = runEnvOsCheckParam.osJobTypeName,
                targetOs = checkItem.targetOs
            )
        }
        if (incompatibleAtoms.isNotEmpty()) {
            checkOsIncompatibleIntroduced(
                runEnvOsCheckParam = runEnvOsCheckParam,
                incompatibleAtoms = incompatibleAtoms
            )
        }
    }

    /**
     * 收集编排中需要做操作系统适配校验的插件，及其所在 Job 的名称与目标操作系统。
     *
     * 只有有编译环境的 Job 才会把插件调度到运行环境的节点上，其余容器(无编译环境、触发器)不受约束。
     * 目标操作系统无从确定的 Job 一并跳过，规则见 [resolveRunEnvOs] 与 [resolveJobRunEnvOs]。
     */
    private fun collectRunEnvOsCheckItems(
        model: Model,
        settingRunEnvOs: OS?,
        skipDisabled: Boolean
    ): List<RunEnvOsCheckItem> {
        val checkItems = mutableListOf<RunEnvOsCheckItem>()
        model.stages.forEach nextStage@{ stage ->
            if (skipDisabled && !stage.stageEnabled()) return@nextStage
            stage.containers.forEach nextContainer@{ container ->
                if (skipDisabled && !container.containerEnabled()) return@nextContainer
                if (resolveContainerEnvType(container) != AtomContainerEnvType.BUILD_ENV) return@nextContainer
                val targetOs = resolveRunEnvOs(
                    settingRunEnvOs = settingRunEnvOs,
                    jobRunEnvOs = resolveJobRunEnvOs(container)
                ) ?: return@nextContainer
                getAtomVersions(container).forEach { storeVersion ->
                    checkItems.add(
                        RunEnvOsCheckItem(
                            jobName = container.name,
                            targetOs = targetOs,
                            storeVersion = storeVersion
                        )
                    )
                }
            }
        }
        return checkItems
    }

    /**
     * 待做操作系统适配校验的插件及其上下文。
     */
    private data class RunEnvOsCheckItem(
        val jobName: String,
        val targetOs: OS,
        val storeVersion: StoreVersion
    ) {
        fun runEnvOsAtomKey() = runEnvOsAtomKey(
            os = targetOs,
            atomCode = storeVersion.storeCode,
            version = storeVersion.version
        )
    }

    /**
     * 「运行环境操作系统 + 插件版本」的标识，同一组合在前向校验与豁免基准两侧必须算出同一个值。
     */
    private fun runEnvOsAtomKey(os: OS, atomCode: String, version: String) = "${os.name}:$atomCode:$version"

    /**
     * 判断插件是否为「借助 buildLessRunFlag 运行在有编译环境 Job 中的无编译环境插件」。
     *
     * 这类插件自身不具备编译环境 jobType，即使被放进有编译环境的 Job 也不在运行环境的节点上执行，
     * 不受运行环境操作系统约束，需要排除在操作系统适配校验之外。
     * 同时声明了编译环境 jobType 的插件按编译环境插件运行，仍需校验操作系统。
     */
    private fun isBuildLessAtomRunInBuildEnv(atomRunInfo: AtomRunInfo): Boolean {
        if (atomRunInfo.buildLessRunFlag != true) return false
        val allJobTypes = JobTypeEnum.resolveAllFromFields(atomRunInfo.jobType, atomRunInfo.jobTypeMap)
        return allJobTypes.isNotEmpty() && allJobTypes.none { it.isBuildEnv() }
    }

    /**
     * 构造插件与「设置所指定运行环境」的操作系统不适配的异常，文案中列出全部不适配插件及其适用系统。
     *
     * 该环境适用于编排中所有 Job，故文案只呈现环境的操作系统，不逐个点出 Job。
     * 仅在本次确实换过操作系统时呈现「由 A 变更为 B」，否则(环境未变、首次指定环境、变更前系统相同)
     * 只呈现当前环境的操作系统，避免提示用户一个他本次并未做过的变更。
     */
    private fun settingOsIncompatibleException(
        runEnvOsChange: PipelineRunEnvOsChange,
        incompatibleAtoms: List<OsIncompatibleAtom>
    ): ErrorCodeException {
        val currentOsName = osDisplayName(runEnvOsChange.currentOs.name)
        // 同一插件被多个 Job 引用时，明细项内容相同，按插件与其适用范围去重
        val atomDetail = incompatibleAtoms.distinctBy { it.atomName to it.supportedOsList }
            .joinToString(separator = "\n") { atom ->
                I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BK_ATOM_RUN_ENV_OS_INCOMPATIBLE_ITEM,
                    params = arrayOf(
                        atom.atomName,
                        atom.supportedOsList.joinToString(separator = " / ") { osDisplayName(it) },
                        currentOsName
                    )
                )
            }
        val previousOs = runEnvOsChange.previousOs?.takeIf { it != runEnvOsChange.currentOs }
        val messageCode = if (previousOs == null) {
            ProcessMessageCode.ERROR_ATOM_RUN_ENV_OS_UNSUPPORTED
        } else {
            ProcessMessageCode.ERROR_ATOM_RUN_ENV_OS_INCOMPATIBLE
        }
        val params = if (previousOs == null) {
            arrayOf(currentOsName, atomDetail)
        } else {
            arrayOf(osDisplayName(previousOs.name), currentOsName, atomDetail)
        }
        return ErrorCodeException(
            errorCode = messageCode,
            params = params,
            defaultMessage = I18nUtil.getCodeLanMessage(messageCode = messageCode, params = params)
        )
    }

    /**
     * 构造插件与其所在 Job 的构建环境操作系统不适配的异常。
     *
     * 与 [settingOsIncompatibleException] 的区别在于目标操作系统逐 Job 而定，
     * 同一插件在不同 Job 下结论可能不同，故明细必须点出 Job 名与该 Job 的操作系统，用户才知道去哪改。
     */
    private fun jobOsIncompatibleException(incompatibleAtoms: List<OsIncompatibleAtom>): ErrorCodeException {
        val atomDetail = incompatibleAtoms.distinct().joinToString(separator = "\n") { atom ->
            I18nUtil.getCodeLanMessage(
                messageCode = ProcessMessageCode.BK_ATOM_JOB_OS_INCOMPATIBLE_ITEM,
                params = arrayOf(
                    atom.jobName,
                    osDisplayName(atom.targetOs.name),
                    atom.atomName,
                    atom.supportedOsList.joinToString(separator = " / ") { osDisplayName(it) }
                )
            )
        }
        val messageCode = ProcessMessageCode.ERROR_ATOM_JOB_OS_INCOMPATIBLE
        val params = arrayOf(atomDetail)
        return ErrorCodeException(
            errorCode = messageCode,
            params = params,
            defaultMessage = I18nUtil.getCodeLanMessage(messageCode = messageCode, params = params)
        )
    }

    /**
     * 操作系统展示名称，为跨语言一致的专有名词，不做国际化。
     */
    private fun osDisplayName(osName: String): String = when (osName.uppercase()) {
        OS.WINDOWS.name -> "Windows"
        OS.LINUX.name -> "Linux"
        OS.MACOS.name -> "macOS"
        else -> osName
    }

    /**
     * 与运行环境操作系统不适配的插件明细。
     *
     * [jobName] 只在目标操作系统逐 Job 而定时才会呈现给用户：运行环境由设置指定时该环境适用于所有 Job，
     */
    private data class OsIncompatibleAtom(
        val jobName: String,
        val targetOs: OS,
        val atomName: String,
        val supportedOsList: List<String>,
        val runEnvOsAtomKey: String
    )

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
     *
     * [jobRunEnvOs] 为该插件所在 Job 声明的构建环境操作系统，由 [resolveJobRunEnvOs] 解析，
     * 无从确定唯一操作系统时为空。运行环境由设置指定的渠道不使用该字段。
     */
    data class AtomCheckParam(
        val storeParam: StoreParam,
        val containerEnvType: AtomContainerEnvType,
        val stageName: String,
        val jobName: String,
        val jobRunEnvOs: OS? = null
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
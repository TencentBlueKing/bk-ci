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

package com.tencent.devops.stream.trigger

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.form.FormBuilder
import com.tencent.devops.common.web.form.data.CheckboxPropData
import com.tencent.devops.common.web.form.data.CompanyStaffPropData
import com.tencent.devops.common.web.form.data.FormDataType
import com.tencent.devops.common.web.form.data.InputPropData
import com.tencent.devops.common.web.form.data.InputPropType
import com.tencent.devops.common.web.form.data.RadioPropData
import com.tencent.devops.common.web.form.data.SelectPropData
import com.tencent.devops.common.web.form.data.SelectPropOption
import com.tencent.devops.common.web.form.data.SelectPropOptionConf
import com.tencent.devops.common.web.form.data.SelectPropOptionItem
import com.tencent.devops.common.web.form.data.TimePropData
import com.tencent.devops.common.web.form.data.TipPropData
import com.tencent.devops.common.web.form.models.Form
import com.tencent.devops.common.web.form.models.ui.DataSourceItem
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.pojo.pipeline.DynamicParameterInfo
import com.tencent.devops.process.pojo.pipeline.DynamicParameterInfoParam
import com.tencent.devops.process.pojo.pipeline.StartUpInfo
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.VariablePropType
import com.tencent.devops.process.yaml.v2.models.on.EnableType
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.ManualTriggerInfo
import com.tencent.devops.stream.pojo.StreamGitYamlString
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamPipelineService
import com.tencent.devops.stream.service.StreamYamlService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.util.GitCommonUtils
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("LongParameterList", "ThrowsCount", "ComplexMethod")
class ManualTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val actionFactory: EventActionFactory,
    streamGitConfig: StreamGitConfig,
    streamEventService: StreamEventService,
    streamBasicSettingService: StreamBasicSettingService,
    private val streamPipelineService: StreamPipelineService,
    private val streamYamlService: StreamYamlService,
    streamYamlTrigger: StreamYamlTrigger,
    streamBasicSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    gitPipelineResourceDao: GitPipelineResourceDao,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    streamYamlBuild: StreamYamlBuild,
    private val yamlTemplateService: YamlTemplateService
) : BaseManualTriggerService(
    client = client,
    dslContext = dslContext,
    streamGitConfig = streamGitConfig,
    streamEventService = streamEventService,
    streamBasicSettingService = streamBasicSettingService,
    streamYamlTrigger = streamYamlTrigger,
    streamBasicSettingDao = streamBasicSettingDao,
    gitPipelineResourceDao = gitPipelineResourceDao,
    gitRequestEventBuildDao = gitRequestEventBuildDao,
    streamYamlBuild = streamYamlBuild
) {

    fun getManualTriggerInfo(
        userId: String,
        pipelineId: String,
        projectId: String,
        branchName: String,
        commitId: String?
    ): ManualTriggerInfo {
        val (yaml, preYaml) = getYamlAndCheck(projectId, pipelineId, commitId, branchName, userId)

        // 关闭了手动触发的直接返回
        if (preYaml.triggerOn?.manual == EnableType.FALSE.value) {
            return ManualTriggerInfo(yaml = yaml, schema = null, enable = false)
        }

        // 获取蓝盾流水线的pipelineAsCodeSetting
        val pipelineSettings = client.get(ServicePipelineSettingResource::class).getPipelineSetting(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.GIT
        ).data ?: throw RuntimeException("get pipeline setting error")

        val variables = parseManualVariables(
            userId = userId,
            triggerBuildReq = TriggerBuildReq(
                projectId = projectId,
                branch = branchName,
                customCommitMsg = null,
                yaml = yaml,
                description = null,
                commitId = commitId,
                payload = null,
                eventType = null,
                inputs = null
            ),
            yamlObject = preYaml,
            pipelineSetting = pipelineSettings.pipelineAsCodeSettings
        )

        if (variables.isNullOrEmpty()) {
            return ManualTriggerInfo(yaml = yaml, schema = null)
        }

        val schema = parseVariablesToForm(variables)

        return ManualTriggerInfo(yaml = yaml, schema = schema)
    }

    private fun getYamlAndCheck(
        projectId: String,
        pipelineId: String,
        commitId: String?,
        branchName: String,
        userId: String
    ): Pair<String?, PreTemplateScriptBuildYaml> {
        val gitProjectId = GitCommonUtils.getGitProjectId(projectId)

        // 获取yaml对象，除了需要替换的 variables，extends和一些信息剩余全部设置为空
        val yaml = try {
            streamPipelineService.getYamlByPipeline(
                gitProjectId, pipelineId,
                if (commitId.isNullOrBlank()) {
                    branchName
                } else {
                    commitId
                }
            )
        } catch (e: RemoteServiceException) {
            if (e.httpStatus == 404) {
                throw ErrorCodeException(
                    statusCode = ErrorCodeEnum.MANUAL_TRIGGER_YAML_NULL.errorCode,
                    errorCode = ErrorCodeEnum.MANUAL_TRIGGER_YAML_NULL.errorCode.toString(),
                    defaultMessage = ErrorCodeEnum.MANUAL_TRIGGER_YAML_NULL.getErrorMessage()
                )
            } else {
                throw e
            }
        }
        if (yaml.isNullOrBlank()) {
            throw ErrorCodeException(
                statusCode = ErrorCodeEnum.MANUAL_TRIGGER_YAML_NULL.errorCode,
                errorCode = ErrorCodeEnum.MANUAL_TRIGGER_YAML_NULL.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.MANUAL_TRIGGER_YAML_NULL.getErrorMessage()
            )
        }

        // 进行读取yaml对象之前对yaml做校验
        val (message, ok) = streamYamlService.checkYaml(userId, StreamGitYamlString(yaml))
        if (!ok) {
            throw ErrorCodeException(
                statusCode = ErrorCodeEnum.MANUAL_TRIGGER_YAML_INVALID.errorCode,
                errorCode = ErrorCodeEnum.MANUAL_TRIGGER_YAML_INVALID.errorCode.toString(),
                defaultMessage = message.message
            )
        }

        val preYaml = YamlUtil.getObjectMapper().readValue(
            ScriptYmlUtils.formatYaml(yaml),
            PreTemplateScriptBuildYaml::class.java
        ).copy(
            stages = null,
            jobs = null,
            steps = null,
            notices = null,
            finally = null,
            concurrency = null
        )
        return Pair(yaml, preYaml)
    }

    fun getManualStartUpInfo(
        userId: String,
        pipelineId: String,
        projectId: String,
        branchName: String,
        commitId: String?
    ): List<DynamicParameterInfo> {
        val (yaml, preYaml) = getYamlAndCheck(projectId, pipelineId, commitId, branchName, userId)

        // 关闭了手动触发的直接返回
        if (preYaml.triggerOn?.manual == EnableType.FALSE.value) {
            return emptyList()
        }
        // 获取蓝盾流水线的pipelineAsCodeSetting
        val pipelineSettings = client.get(ServicePipelineSettingResource::class).getPipelineSetting(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.GIT
        ).data ?: throw RuntimeException("get pipeline setting error")

        val variables = parseManualVariables(
            userId = userId,
            triggerBuildReq = TriggerBuildReq(
                projectId = projectId,
                branch = branchName,
                customCommitMsg = null,
                yaml = yaml,
                description = null,
                commitId = commitId,
                payload = null,
                eventType = null,
                inputs = null
            ),
            yamlObject = preYaml,
            pipelineSetting = pipelineSettings.pipelineAsCodeSettings
        )

        if (variables.isNullOrEmpty()) {
            return emptyList()
        }

        return parseVariablesToStartUp(variables)
    }

    private fun parseManualVariables(
        userId: String,
        triggerBuildReq: TriggerBuildReq,
        yamlObject: PreTemplateScriptBuildYaml,
        pipelineSetting: PipelineAsCodeSettings?
    ): Map<String, Variable>? {
        val streamTriggerSetting = getSetting(triggerBuildReq)

        val action = loadAction(streamTriggerSetting, userId, triggerBuildReq)

        return YamlTemplate(
            yamlObject = yamlObject,
            filePath = StreamYamlTrigger.STREAM_TEMPLATE_ROOT_FILE,
            extraParameters = action,
            getTemplateMethod = yamlTemplateService::getTemplate,
            nowRepo = null,
            repo = null,
            resourcePoolMapExt = null,
            conf = YamlTemplateConf(
                useOldParametersExpression = pipelineSetting?.enable != true
            )
        ).replace().variables
    }

    override fun loadAction(
        streamTriggerSetting: StreamTriggerSetting,
        userId: String,
        triggerBuildReq: TriggerBuildReq
    ): BaseAction {
        val action = actionFactory.loadManualAction(
            setting = streamTriggerSetting,
            event = StreamManualEvent(
                userId = userId,
                gitProjectId = GitCommonUtils.getGitProjectId(triggerBuildReq.projectId).toString(),
                triggerBuildReq = triggerBuildReq
            )
        )
        val request = action.buildRequestEvent("") ?: throw CustomException(
            status = Response.Status.BAD_REQUEST,
            message = "event invalid"
        )
        val id = gitRequestEventDao.saveGitRequest(dslContext, request)
        action.data.context.requestEventId = id

        return action
    }

    override fun getStartParams(action: BaseAction, triggerBuildReq: TriggerBuildReq): Map<String, String> {
        return emptyMap()
    }

    override fun getInputParams(action: BaseAction, triggerBuildReq: TriggerBuildReq): Map<String, String>? {
        return triggerBuildReq.inputs
    }

    companion object {
        fun parseVariablesToForm(variables: Map<String, Variable>): Form {
            val builder = FormBuilder().setTitle("").setDescription("")

            // 去掉不能在前端页面展示的
            variables.filter { it.value.allowModifyAtStartup == true }.forEach { (name, value) ->
                when (VariablePropType.findType(value.props?.type)) {
                    VariablePropType.VUEX_INPUT -> builder.setProp(
                        InputPropData(
                            id = name,
                            type = FormDataType.STRING,
                            title = value.props?.label ?: name,
                            default = value.value,
                            required = value.props?.required,
                            description = value.props?.description
                        )
                    )

                    VariablePropType.VUEX_TEXTAREA -> builder.setProp(
                        InputPropData(
                            id = name,
                            type = FormDataType.STRING,
                            title = value.props?.label ?: name,
                            default = value.value,
                            required = value.props?.required,
                            description = value.props?.description,
                            inputType = InputPropType.TEXTAREA
                        )
                    )

                    VariablePropType.SELECTOR -> {
                        builder.setProp(
                            SelectPropData(
                                id = name,
                                type = if (value.props?.multiple == true) {
                                    FormDataType.ARRAY
                                } else {
                                    FormDataType.STRING
                                },
                                title = value.props?.label ?: name,
                                default = if (value.props?.options.isNullOrEmpty()) {
                                    // 从url拿的value为了和前端统一转一下类型
                                    if (value.props?.multiple == true) {
                                        value.value?.split(",")?.map { it.trim().stringToOther() }?.toSet()
                                            ?: emptySet<String>()
                                    } else {
                                        value.value?.trim()?.stringToOther()
                                    }
                                } else {
                                    if (value.props?.multiple == true) {
                                        value.value?.split(",")?.map { it.trim() }?.toSet() ?: emptySet<String>()
                                    } else {
                                        value.value?.trim()
                                    }
                                },
                                required = value.props?.required,
                                description = value.props?.description,
                                componentName = "selector",
                                option = SelectPropOption(
                                    items = if (!value.props?.options.isNullOrEmpty()) {
                                        value.props?.options?.map { option ->
                                            SelectPropOptionItem(
                                                id = option.id.toString(),
                                                name = option.label ?: option.id.toString(),
                                                description = option.description
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    conf = SelectPropOptionConf(
                                        url = value.props?.datasource?.url,
                                        dataPath = value.props?.datasource?.dataPath,
                                        paramId = value.props?.datasource?.paramId,
                                        paramName = value.props?.datasource?.paramName,
                                        hasAddItem = value.props?.datasource?.hasAddItem,
                                        itemTargetUrl = value.props?.datasource?.itemTargetUrl,
                                        itemText = value.props?.datasource?.itemText,
                                        multiple = value.props?.multiple,
                                        clearable = true
                                    )
                                )
                            )
                        )
                    }

                    VariablePropType.CHECKBOX -> builder.setProp(
                        CheckboxPropData(
                            id = name,
                            type = FormDataType.ARRAY,
                            title = value.props?.label ?: name,
                            default = value.value?.split(",")?.map { it.trim() }?.toSet() ?: emptySet<String>(),
                            required = value.props?.required,
                            description = value.props?.description,
                            dataSource = value.props?.options?.map { option ->
                                DataSourceItem(
                                    label = option.label ?: option.id.toString(),
                                    value = option.id.toString()
                                )
                            }
                        )
                    )

                    VariablePropType.BOOLEAN -> builder.setProp(
                        RadioPropData(
                            id = name,
                            type = FormDataType.BOOLEAN,
                            title = value.props?.label ?: name,
                            default = value.value?.toBoolean(),
                            required = value.props?.required,
                            description = value.props?.description,
                            dataSource = listOf(
                                DataSourceItem("true", true),
                                DataSourceItem("false", false)
                            )
                        )
                    )

                    VariablePropType.TIME_PICKER -> builder.setProp(
                        TimePropData(
                            id = name,
                            type = FormDataType.STRING,
                            title = value.props?.label ?: name,
                            default = value.value,
                            required = value.props?.required,
                            description = value.props?.description
                        )
                    )

                    VariablePropType.COMPANY_STAFF_INPUT -> builder.setProp(
                        CompanyStaffPropData(
                            id = name,
                            title = value.props?.label ?: name,
                            default = value.value?.split(",")?.toSet() ?: emptySet<String>(),
                            required = value.props?.required,
                            description = value.props?.description
                        )
                    )

                    VariablePropType.TIPS -> builder.setProp(
                        TipPropData(
                            id = name,
                            title = value.props?.label ?: name,
                            default = value.value,
                            required = value.props?.required,
                            description = value.props?.description
                        )
                    )
                    // 默认按input, string类型算
                    else -> builder.setProp(
                        InputPropData(
                            id = name,
                            type = FormDataType.STRING,
                            title = value.props?.label ?: name,
                            default = value.value,
                            required = value.props?.required,
                            description = value.props?.description
                        )
                    )
                }
            }

            return builder.build()
        }

        /**
         * 子流水线插件使用
         */
        fun parseVariablesToStartUp(variables: Map<String, Variable>): List<DynamicParameterInfo> {
            // 去掉不能在前端页面展示的
            return variables.filter { it.value.allowModifyAtStartup == true }.map { (name, value) ->
                when (VariablePropType.findType(value.props?.type)) {
                    VariablePropType.SELECTOR ->
                        DynamicParameterInfo(
                            id = name,
                            paramModels = listOf(
                                DynamicParameterInfoParam(
                                    id = "key",
                                    type = "input",
                                    disabled = true,
                                    value = name
                                ),
                                DynamicParameterInfoParam(
                                    id = "value",
                                    type = "select",
                                    disabled = false,
                                    value = value.value ?: "",
                                    listType = "list",
                                    isMultiple = value.props?.multiple,
                                    list = value.props?.options?.map { option ->
                                        StartUpInfo(
                                            id = option.id.toString(),
                                            name = option.id.toString()
                                        )
                                    }
                                )
                            )
                        )

                    VariablePropType.CHECKBOX ->
                        DynamicParameterInfo(
                            id = name,
                            paramModels = listOf(
                                DynamicParameterInfoParam(
                                    id = "key",
                                    type = "input",
                                    disabled = true,
                                    value = name
                                ),
                                DynamicParameterInfoParam(
                                    id = "value",
                                    type = "select",
                                    disabled = false,
                                    value = value.value ?: "",
                                    listType = "list",
                                    isMultiple = value.props?.multiple,
                                    list = value.props?.options?.map { option ->
                                        StartUpInfo(
                                            id = option.id.toString(),
                                            name = option.id.toString()
                                        )
                                    }
                                )
                            )
                        )

                    VariablePropType.BOOLEAN ->
                        DynamicParameterInfo(
                            id = name,
                            paramModels = listOf(
                                DynamicParameterInfoParam(
                                    id = "key",
                                    type = "input",
                                    disabled = true,
                                    value = name
                                ),
                                DynamicParameterInfoParam(
                                    id = "value",
                                    type = "select",
                                    disabled = false,
                                    value = value.value ?: "",
                                    listType = "list",
                                    list = listOf(
                                        StartUpInfo(
                                            id = "true",
                                            name = "true"
                                        ),
                                        StartUpInfo(
                                            id = "false",
                                            name = "false"
                                        )
                                    )
                                )
                            )
                        )

                    VariablePropType.TIPS ->
                        DynamicParameterInfo(
                            id = name,
                            paramModels = listOf(
                                DynamicParameterInfoParam(
                                    id = "key",
                                    type = "input",
                                    disabled = true,
                                    value = name
                                ),
                                DynamicParameterInfoParam(
                                    id = "value",
                                    type = "input",
                                    disabled = true,
                                    value = value.value ?: ""
                                )
                            )
                        )
                    // 默认按input, string类型算
                    else -> DynamicParameterInfo(
                        id = name,
                        paramModels = listOf(
                            DynamicParameterInfoParam(
                                id = "key",
                                type = "input",
                                disabled = true,
                                value = name
                            ),
                            DynamicParameterInfoParam(
                                id = "value",
                                type = "input",
                                disabled = false,
                                value = value.value ?: ""
                            )
                        )
                    )
                }
            }
        }

        // 将string转为其他可能的类型，如double，int或bool
        fun String.stringToOther(): Any {
            if (this == "true" || this == "false") {
                return this.toBoolean()
            }
            // 有小数说明只有可能是double
            if (this.contains(".")) {
                return this.toDoubleOrNull() ?: this
            }
            return this.toLongOrNull() ?: this
        }

        fun parseInputs(inputs: Map<String, Any?>?): Map<String, String>? {
            if (inputs.isNullOrEmpty()) {
                return null
            }

            return mutableMapOf<String, String>().also { result ->
                inputs.forEach inputEach@{ (key, value) ->
                    if (value == null) {
                        return@inputEach
                    }

                    when (value) {
                        is Iterable<*> -> {
                            if (value.count() < 0) {
                                return@inputEach
                            }
                            result[key] = value.joinToString(",")
                        }

                        else -> result[key] = value.toString()
                    }
                }
            }
        }
    }
}

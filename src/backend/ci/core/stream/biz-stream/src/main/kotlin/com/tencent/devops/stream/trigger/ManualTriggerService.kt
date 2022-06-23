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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.TriggerBuildReq
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerSetting
import com.tencent.devops.stream.trigger.actions.streamActions.data.StreamManualEvent
import com.tencent.devops.stream.trigger.service.StreamEventService
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongParameterList", "ThrowsCount")
class ManualTriggerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val actionFactory: EventActionFactory,
    streamGitConfig: StreamGitConfig,
    streamEventService: StreamEventService,
    streamBasicSettingService: StreamBasicSettingService,
    streamYamlTrigger: StreamYamlTrigger,
    private val streamBasicSettingDao: StreamBasicSettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    gitPipelineResourceDao: GitPipelineResourceDao,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    streamYamlBuild: StreamYamlBuild,
    private val yamlTemplateService: YamlTemplateService
) : BaseManualTriggerService(
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

    fun parseManualVariables(
        userId: String,
        pipelineId: String,
        triggerBuildReq: TriggerBuildReq,
        yamlObject: PreTemplateScriptBuildYaml
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
            resourcePoolMapExt = null
        ).replace().variables
    }

    companion object {
        fun parseInputs(inputs: Map<String, Any?>?): Map<String, String>? {
            if (inputs == null) {
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
                            result[key] = JsonUtil.toJson(value)
                        }
                        else -> result[key] = value.toString()
                    }
                }
            }
        }
    }
}

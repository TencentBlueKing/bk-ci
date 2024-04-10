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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerMatcher
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.trigger.template.YamlTemplateService
import com.tencent.devops.stream.v1.components.V1YamlTrigger
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import com.tencent.devops.stream.v1.pojo.V1GitRequestEventForHandle
import com.tencent.devops.stream.v1.pojo.V1StreamTriggerContext
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Suppress("ComplexCondition", "LongParameterList")
@Primary
@Service
class TXStreamYamlTrigger @Autowired constructor(
    dslContext: DSLContext,
    client: Client,
    triggerMatcher: TriggerMatcher,
    yamlSchemaCheck: YamlSchemaCheck,
    yamlTemplateService: YamlTemplateService,
    streamBasicSettingService: StreamBasicSettingService,
    yamlBuild: StreamYamlBuild,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    streamYamlBaseBuild: StreamYamlBaseBuild,
    private val objectMapper: ObjectMapper,
    private val v1YamlTrigger: V1YamlTrigger,
    private val streamGitConfig: StreamGitConfig
) : StreamYamlTrigger(
    client = client,
    dslContext = dslContext,
    triggerMatcher = triggerMatcher,
    yamlSchemaCheck = yamlSchemaCheck,
    yamlTemplateService = yamlTemplateService,
    streamBasicSettingService = streamBasicSettingService,
    yamlBuild = yamlBuild,
    gitRequestEventBuildDao = gitRequestEventBuildDao,
    streamYamlBaseBuild = streamYamlBaseBuild,
    streamGitConfig = streamGitConfig
) {

    override fun trigger(action: BaseAction, triggerEvent: Pair<List<Any>?, TriggerResult>?) {
        // 检查yml版本，根据yml版本选择不同的实现
        val originYaml = action.data.context.originYaml!!
        val ymlVersion = ScriptYmlUtils.parseVersion(originYaml)

        if (ymlVersion?.version == "v2.0") {
            super.trigger(action, triggerEvent)
            return
        }

        val request = action.buildRequestEvent(objectMapper.writeValueAsString(action.data.event))!!
        request.id = action.data.context.requestEventId
        v1YamlTrigger.triggerBuild(
            V1StreamTriggerContext(
                gitEvent = action.data.event as GitEvent,
                gitRequestEventForHandle = gitCiTriggerChangeGitRequestEvent(request),
                streamSetting = action.data.setting,
                pipeline = action.data.context.pipeline!!,
                originYaml = originYaml,
                mrChangeSet = action.data.context.changeSet?.toSet()
            )
        )
    }

    private fun gitCiTriggerChangeGitRequestEvent(
        gitRequestEvent: GitRequestEvent
    ): V1GitRequestEventForHandle {
        return V1GitRequestEventForHandle(
            id = gitRequestEvent.id,
            gitProjectId = gitRequestEvent.gitProjectId,
            branch = gitRequestEvent.branch,
            userId = gitRequestEvent.userId,
            checkRepoTrigger = false,
            gitRequestEvent = V1GitRequestEvent(gitRequestEvent)
        )
    }
}

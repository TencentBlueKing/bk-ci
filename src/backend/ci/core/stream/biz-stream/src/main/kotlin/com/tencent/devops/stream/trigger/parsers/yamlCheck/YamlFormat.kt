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

package com.tencent.devops.stream.trigger.parsers.yamlCheck

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.exception.YamlFormatException
import com.tencent.devops.process.yaml.v2.models.PreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import io.jsonwebtoken.io.IOException
import org.slf4j.LoggerFactory

object YamlFormat {

    private val logger = LoggerFactory.getLogger(YamlFormat::class.java)

    @Throws(StreamTriggerBaseException::class, ErrorCodeException::class)
    fun formatYaml(
        action: BaseAction
    ): PreTemplateScriptBuildYaml {
        // to format
        return try {
            YamlUtil.getObjectMapper().readValue(
                ScriptYmlUtils.formatYaml(action.data.context.originYaml!!),
                PreTemplateScriptBuildYaml::class.java
            )
        } catch (e: Throwable) {
            logger.info("YamlFormat|formatYaml|git ci yaml is invalid|eventId|${action.data.context.requestEventId}", e)
            val (block, message, reason) = when (e) {
                is YamlFormatException, is CustomException -> {
                    Triple(action.metaData.isStreamMr(), e.message, TriggerReason.CI_YAML_INVALID)
                }
                is IOException, is TypeCastException -> {
                    Triple(action.metaData.isStreamMr(), e.message, TriggerReason.CI_YAML_INVALID)
                }
                // 指定异常直接扔出在外面统一处理
                is StreamTriggerBaseException, is ErrorCodeException -> {
                    throw e
                }
                else -> {
                    logger.warn("YamlFormat|requestEventId|${action.data.context.requestEventId}|error", e)
                    Triple(false, e.message, TriggerReason.UNKNOWN_ERROR)
                }
            }
            throw StreamTriggerException(
                action,
                reason,
                listOf(message ?: ""),
                commitCheck = CommitCheck(
                    block = block,
                    state = StreamCommitCheckState.FAILURE
                )
            )
        }
    }
}

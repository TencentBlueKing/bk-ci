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

package com.tencent.devops.stream.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamGitYamlString
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import com.tencent.devops.stream.v1.service.V1StreamYamlService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TXStreamYamlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamSettingDao: StreamBasicSettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val v1streamYamlService: V1StreamYamlService,
    private val yamlSchemaCheck: YamlSchemaCheck
) : StreamYamlService(
    dslContext, streamSettingDao, gitRequestEventBuildDao, yamlSchemaCheck
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXStreamYamlService::class.java)
    }

    override fun checkYaml(userId: String, yaml: StreamGitYamlString): Pair<Result<String>, Boolean> {
        // 检查yml版本，根据yml版本选择不同的实现
        val ymlVersion = ScriptYmlUtils.parseVersion(yaml.yaml)
        return when {
            ymlVersion == null -> {
                Pair(Result(1, "Invalid yaml version is null"), false)
            }
            ymlVersion.version != "v2.0" -> {
                try {
                    val yamlStr = CiYamlUtils.formatYaml(yaml.yaml)
                    logger.debug("yaml str : $yamlStr")

                    val (validate, message) = v1streamYamlService.validateCIBuildYaml(yamlStr)
                    if (!validate) {
                        logger.warn("TXStreamYamlService|checkYaml|error=$message")
                        Result(1, "Invalid yaml: $message", message)
                    }
                    v1streamYamlService.createCIBuildYaml(yaml.yaml)

                    Pair(Result("OK"), true)
                } catch (e: Throwable) {
                    logger.warn("TXStreamYamlService|checkYaml|error|${e.message}|yaml|$yaml")
                    Pair(Result(1, "Invalid yaml", e.message), false)
                }
            }
            else -> {
                return try {
                    yamlSchemaCheck.check(yaml.yaml, null, true)
                    Pair(Result("OK"), true)
                } catch (e: Exception) {
                    logger.warn("TXStreamYamlService|checkYaml|failed", e)
                    Pair(Result(1, "Invalid yaml: ${e.message}"), false)
                }
            }
        }
    }
}

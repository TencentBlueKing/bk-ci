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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.yaml.v2.enums.TemplateType
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.constant.StreamMessageCode.PROJECT_CANNOT_QUERIED
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.StreamBasicSettingDao
import com.tencent.devops.stream.pojo.StreamGitYamlString
import com.tencent.devops.stream.pojo.V2BuildYaml
import com.tencent.devops.stream.trigger.parsers.yamlCheck.YamlSchemaCheck
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class StreamYamlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamSettingDao: StreamBasicSettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val yamlSchemaCheck: YamlSchemaCheck
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamYamlService::class.java)
    }

    fun getYamlV2(gitProjectId: Long, buildId: String): V2BuildYaml? {
        logger.info("StreamYamlService|getYamlV2|buildId|$buildId|gitProjectId|$gitProjectId")
        streamSettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            I18nUtil.getCodeLanMessage(PROJECT_CANNOT_QUERIED)
        )
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        // 针对V2版本做替换
        val parsed = eventBuild.parsedYaml.replaceFirst("triggerOn:", "on:")
        return V2BuildYaml(parsedYaml = parsed, originYaml = eventBuild.originYaml)
    }

    fun checkYaml(userId: String, yaml: StreamGitYamlString): Pair<Result<String>, Boolean> {
        // 检查yml版本，根据yml版本选择不同的实现
        val ymlVersion = ScriptYmlUtils.parseVersion(yaml.yaml)
        return when {
            ymlVersion == null -> {
                Pair(Result(1, "Invalid yaml version is null"), false)
            }
            else -> {
                return try {
                    yamlSchemaCheck.check(yaml.yaml, null, true)
                    Pair(Result("OK"), true)
                } catch (e: Exception) {
                    logger.warn("StreamYamlService|checkYaml|failed", e)
                    Pair(Result(1, "Invalid yaml: ${e.message}"), false)
                }
            }
        }
    }

    fun checkYaml(originYaml: String, templateType: TemplateType?, isCiFile: Boolean): Result<String> {
        return try {
            yamlSchemaCheck.check(originYaml, templateType, isCiFile)
            Result("OK")
        } catch (e: Exception) {
            logger.error("Check yaml schema failed.", e)
            Result(1, "Invalid yaml: ${e.message}")
        }
    }
}

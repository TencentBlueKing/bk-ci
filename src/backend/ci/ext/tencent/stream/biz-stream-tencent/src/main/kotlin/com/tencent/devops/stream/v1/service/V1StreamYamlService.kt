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

package com.tencent.devops.stream.v1.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.stream.constant.StreamMessageCode.GIT_CI_NO_RECOR
import com.tencent.devops.stream.constant.StreamMessageCode.MIRROR_VERSION_NOT_AVAILABLE
import com.tencent.devops.stream.constant.StreamMessageCode.PROJECT_CANNOT_QUERIED
import com.tencent.devops.stream.v1.dao.V1GitCIServicesConfDao
import com.tencent.devops.stream.v1.dao.V1GitCISettingDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.dao.V1StreamBasicSettingDao
import com.tencent.devops.stream.v1.pojo.V1EnvironmentVariables
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.StringReader
import javax.ws.rs.core.Response

@Service
class V1StreamYamlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitServicesConfDao: V1GitCIServicesConfDao,
    private val gitCIV1SettingDao: V1GitCISettingDao,
    private val gitCISettingDao: V1StreamBasicSettingDao,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(V1StreamYamlService::class.java)
    }

    fun validateCIBuildYaml(yamlStr: String) = CiYamlUtils.validateYaml(yamlStr)

    fun getCIBuildYamlSchema() = CiYamlUtils.getCIBuildYamlSchema()

    fun createCIBuildYaml(yamlStr: String, gitProjectId: Long? = null): CIBuildYaml {
        logger.info("input yamlStr: $yamlStr")

        var yaml = CiYamlUtils.formatYaml(yamlStr)
        yaml = replaceEnv(yaml, gitProjectId)
        val yamlObject = YamlUtil.getObjectMapper().readValue(yaml, CIBuildYaml::class.java)

        // 检测services镜像
        if (yamlObject.services != null) {
            yamlObject.services!!.forEachIndexed { index, it ->
                // 判断镜像格式是否合法
                val (imageName, imageTag) = it.parseImage()
                val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                    ?: throw CustomException(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        MessageUtil.getMessageByLocale(
                            messageCode = GIT_CI_NO_RECOR,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ) + ". ${it.image}"
                    )
                if (!record.enable) {
                    throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, MessageUtil.getMessageByLocale(
                        messageCode = MIRROR_VERSION_NOT_AVAILABLE,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ) + ". ${it.image}")
                }
            }
        }

        return CiYamlUtils.normalizeGitCiYaml(yamlObject)
    }

    private fun replaceEnv(yaml: String, gitProjectId: Long?): String {
        if (gitProjectId == null) {
            return yaml
        }
        val gitProjectConf = gitCIV1SettingDao.getSetting(dslContext, gitProjectId) ?: return yaml
        logger.info("gitProjectConf: $gitProjectConf")
        if (null == gitProjectConf.env) {
            return yaml
        }

        val sb = StringBuilder()
        val br = BufferedReader(StringReader(yaml))
        val envRegex = Regex("\\\$env:\\w+")
        var line: String? = br.readLine()
        while (line != null) {
            val envMatches = envRegex.find(line)
            envMatches?.groupValues?.forEach {
                logger.info("envKeyPrefix: $it")
                val envKeyPrefix = it
                val envKey = envKeyPrefix.removePrefix("\$env:")
                val envValue = getEnvValue(gitProjectConf.env!!, envKey)
                logger.info("envKey: $envKey, envValue: $envValue")
                line = if (null != envValue) {
                    envRegex.replace(line!!, envValue)
                } else {
                    envRegex.replace(line!!, "null")
                }
                logger.info("line: $line")
            }

            sb.append(line).append("\n")
            line = br.readLine()
        }

        return sb.toString()
    }

    private fun getEnvValue(env: List<V1EnvironmentVariables>, key: String): String? {
        env.forEach {
            if (it.name == key) {
                return it.value
            }
        }
        return null
    }

    fun getYaml(gitProjectId: Long, buildId: String): String {
        logger.info("get yaml by buildId:($buildId), gitProjectId: $gitProjectId")
        gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(
            Response.Status.FORBIDDEN,
            MessageUtil.getMessageByLocale(
                messageCode = PROJECT_CANNOT_QUERIED,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        )
        val eventBuild = gitRequestEventBuildDao.getByBuildId(dslContext, buildId)
        return (eventBuild?.originYaml) ?: ""
    }
}

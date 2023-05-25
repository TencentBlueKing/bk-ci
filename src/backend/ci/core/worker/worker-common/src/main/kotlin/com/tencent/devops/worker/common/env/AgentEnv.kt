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

package com.tencent.devops.worker.common.env

import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.constant.LOCALE_LANGUAGE
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.PropertyUtil
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.common.service.env.Env
import com.tencent.devops.worker.common.exception.PropertyNotExistException
import com.tencent.devops.worker.common.utils.WorkspaceUtils.getLandun
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

@Suppress("ALL")
object AgentEnv {

    private val logger = LoggerFactory.getLogger(AgentEnv::class.java)

    const val PROJECT_ID = "devops.project.id"
    const val DOCKER_PROJECT_ID = "devops_project_id"
    const val AGENT_ID = "devops.agent.id"
    const val DOCKER_AGENT_ID = "devops_agent_id"
    const val AGENT_SECRET_KEY = "devops.agent.secret.key"
    const val DOCKER_AGENT_SECRET_KEY = "devops_agent_secret_key"
    const val AGENT_GATEWAY = "landun.gateway"
    const val DOCKER_GATEWAY = "devops_gateway"
    const val AGENT_FILE_GATEWAY = "DEVOPS_FILE_GATEWAY"
    const val AGENT_ENV = "landun.env"
    const val AGENT_LOG_SAVE_MODE = "devops_log_save_mode"
    const val AGENT_PROPERTIES_FILE_NAME = ".agent.properties"
    const val BK_TAG = "devops_bk_tag"

    private var projectId: String? = null
    private var agentId: String? = null
    private var secretKey: String? = null
    private var gateway: String? = null
    private var fileGateway: String? = null
    private var os: OSType? = null
    private var env: Env? = null
    private var logStorageMode: LogStorageMode? = null
    private var bkTag: String? = null

    private var property: Properties? = null

    private val propertyFile = File(getLandun(), AGENT_PROPERTIES_FILE_NAME)

    fun getProjectId(): String {

        if (projectId.isNullOrBlank()) {
            synchronized(this) {
                if (projectId.isNullOrBlank()) {
                    projectId = getProperty(DOCKER_PROJECT_ID)
                    if (projectId.isNullOrBlank()) {
                        projectId = getProperty(PROJECT_ID)
                    }
                    if (projectId.isNullOrBlank()) {
                        throw PropertyNotExistException("$PROJECT_ID|$DOCKER_PROJECT_ID", "Empty project Id")
                    }
                    logger.info("Get the project ID($projectId)")
                }
            }
        }
        return projectId!!
    }

    fun getAgentId(): String {
        if (agentId.isNullOrBlank()) {
            synchronized(this) {
                if (agentId.isNullOrBlank()) {
                    agentId = getProperty(DOCKER_AGENT_ID)
                    if (agentId.isNullOrBlank()) {
                        agentId = getProperty(AGENT_ID)
                    }
                    if (agentId.isNullOrBlank()) {
                        throw PropertyNotExistException("$AGENT_ID|$DOCKER_AGENT_ID", "Empty agent Id")
                    }
                    logger.info("Get the agent id($agentId)")
                }
            }
        }
        return agentId!!
    }

    fun getEnv(): Env {
        if (env == null) {
            synchronized(this) {
                if (env == null) {
                    val landunEnv = System.getProperty(AGENT_ENV)
                    env = if (!landunEnv.isNullOrEmpty()) {
                        Env.parse(landunEnv)
                    } else {
                        // Get it from .agent.property
                        try {
                            Env.parse(PropertyUtil.getPropertyValue(AGENT_ENV, "/$AGENT_PROPERTIES_FILE_NAME"))
                        } catch (t: Throwable) {
                            logger.warn("Fail to get the agent env, use prod as default", t)
                            Env.PROD
                        }
                    }
                }
            }
        }
        return env!!
    }

    @Suppress("UNUSED")
    fun isProd() = getEnv() == Env.PROD

    @Suppress("UNUSED")
    fun isTest() = getEnv() == Env.TEST

    @Suppress("UNUSED")
    fun isDev() = getEnv() == Env.DEV

    fun getAgentSecretKey(): String {
        if (secretKey.isNullOrBlank()) {
            synchronized(this) {
                if (secretKey.isNullOrBlank()) {
                    secretKey = getProperty(DOCKER_AGENT_SECRET_KEY)
                    if (secretKey.isNullOrBlank()) {
                        secretKey = getProperty(AGENT_SECRET_KEY)
                    }
                    if (secretKey.isNullOrBlank()) {
                        throw PropertyNotExistException("$AGENT_SECRET_KEY|$DOCKER_AGENT_SECRET_KEY", "Empty agent secret key")
                    }
                    logger.info("Get the agent secret key($secretKey)")
                }
            }
        }
        return secretKey!!
    }

    fun getGateway(): String {
        if (gateway.isNullOrBlank()) {
            synchronized(this) {
                if (gateway.isNullOrBlank()) {
                    try {
                        gateway = getProperty(DOCKER_GATEWAY)
                        if (gateway.isNullOrBlank()) {
                            gateway = getProperty(AGENT_GATEWAY)
                        }
                        if (gateway.isNullOrBlank()) {
                            throw PropertyNotExistException(AGENT_GATEWAY, "Empty agent gateway")
                        }
                    } catch (t: Throwable) {
                        gateway = System.getProperty("devops.gateway", "")
                    }
                    logger.info("gateway: $gateway")
                }
            }
        }
        return gateway!!
    }

    fun getFileGateway(): String? {
        if (fileGateway.isNullOrBlank()) {
            synchronized(this) {
                if (fileGateway.isNullOrBlank()) {
                    fileGateway = getEnvProp(AGENT_FILE_GATEWAY)
                    logger.info("file gateway: $fileGateway")
                }
            }
        }
        return fileGateway
    }

    fun getOS(): OSType {
        if (os == null) {
            synchronized(this) {
                if (os == null) {
                    val osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
                    logger.info("Get the os name - ($osName)")
                    os = if (osName.indexOf(string = "mac") >= 0 || osName.indexOf("darwin") >= 0) {
                        OSType.MAC_OS
                    } else if (osName.indexOf("win") >= 0) {
                        OSType.WINDOWS
                    } else if (osName.indexOf("nux") >= 0) {
                        OSType.LINUX
                    } else {
                        OSType.OTHER
                    }
                }
            }
        }
        return os!!
    }

    fun getBkTag(): String? {

        if (bkTag.isNullOrBlank()) {
            synchronized(this) {
                if (bkTag.isNullOrBlank()) {
                    bkTag = getProperty(BK_TAG)
                    logger.info("Get the bkTag($bkTag)")
                    return bkTag
                }
            }
        }
        return bkTag!!
    }

    @Suppress("UNUSED")
    fun is32BitSystem() = System.getProperty("sun.arch.data.model") == "32"

    private fun getProperty(prop: String): String? {
        val buildType = BuildEnv.getBuildType()
        if (buildType == BuildType.DOCKER || buildType == BuildType.MACOS || buildType == BuildType.MACOS_NEW) {
            logger.info("buildType is $buildType")
            return getEnvProp(prop)
        }

        if (property == null) {
            if (!propertyFile.exists()) {
                throw ParamBlankException("The property file(${propertyFile.absolutePath}) is not exist")
            }
            property = Properties()
            property!!.load(FileInputStream(propertyFile))
        }
        return property!!.getProperty(prop, null)
    }

    fun getEnvProp(prop: String): String? {
        var value = System.getenv(prop)
        if (value.isNullOrBlank()) {
            // Get from java properties
            value = System.getProperty(prop)
        }
        return value
    }

    fun isDockerEnv(): Boolean {
        return BuildEnv.getBuildType() == BuildType.DOCKER
    }

    fun getLogMode(): LogStorageMode {
        if (null == logStorageMode) {
            synchronized(this) {
                if (null == logStorageMode) {
                    logStorageMode = try {
                        LogStorageMode.valueOf(
                            System.getenv(AGENT_LOG_SAVE_MODE)
                                ?: throw PropertyNotExistException(AGENT_LOG_SAVE_MODE, "Empty log mode")
                        )
                    } catch (t: Throwable) {
                        logger.warn("not system variable named log mode!")
                        LogStorageMode.UPLOAD
                    }
                    logger.info("get the log mode $logStorageMode")
                }
            }
        }
        return logStorageMode!!
    }

    fun setLogMode(storageMode: LogStorageMode) {
        logStorageMode = storageMode
    }

    /**
     * 获取国际化语言信息
     * @return 国际化语言信息
     */
    fun getLocaleLanguage(): String {
        return System.getProperty(LOCALE_LANGUAGE) ?: System.getenv(LOCALE_LANGUAGE) ?: DEFAULT_LOCALE_LANGUAGE
    }
}

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.env

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.service.env.Env
import com.tencent.devops.worker.common.exception.PropertyNotExistException
import com.tencent.devops.worker.common.utils.WorkspaceUtils.getLandun
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

object AgentEnv {

    private val logger = LoggerFactory.getLogger(AgentEnv::class.java)

    private const val PROJECT_ID = "devops.project.id"
    private const val DOCKER_PROJECT_ID = "devops_project_id"
    private const val AGENT_ID = "devops.agent.id"
    private const val DOCKER_AGENT_ID = "devops_agent_id"
    private const val AGENT_SECRET_KEY = "devops.agent.secret.key"
    private const val DOCKER_AGENT_SECRET_KEY = "devops_agent_secret_key"
    private const val AGENT_GATEWAY = "landun.gateway"
    private const val DOCKER_GATEWAY = "devops.gateway"
    private const val AGENT_ENV = "landun.env"

    private var projectId: String? = null
    private var agentId: String? = null
    private var secretKey: String? = null
    private var gateway: String? = null
    private var os: OSType? = null
    private var env: Env? = null

    private var property: Properties? = null

    private val propertyFile = File(getLandun(), ".agent.properties")

    fun getProjectId(): String {

        if (projectId.isNullOrBlank()) {
            synchronized(this) {
                if (projectId.isNullOrBlank()) {
                    projectId = getProperty(if (isDockerEnv()) DOCKER_PROJECT_ID else PROJECT_ID)
                    if (projectId.isNullOrBlank()) {
                        throw PropertyNotExistException(PROJECT_ID, "Empty project Id")
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
                    agentId = getProperty(if (isDockerEnv()) DOCKER_AGENT_ID else AGENT_ID)
                    if (agentId.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_ID, "Empty agent Id")
                    }
                    logger.info("Get the agent id($agentId)")
                }
            }
        }
        return agentId!!
    }

    private fun getEnv(): Env {
        if (env == null) {
            synchronized(this) {
                if (env == null) {
                    val landunEnv = System.getProperty(AGENT_ENV)
                    env = if (!landunEnv.isNullOrEmpty()) {
                        Env.parse(landunEnv)
                    } else {
                        // Get it from .agent.property
                        try {
                            Env.parse(getProperty(AGENT_ENV) ?: "")
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

    fun isProd() = getEnv() == Env.PROD

    fun isTest() = getEnv() == Env.TEST

    fun isDev() = getEnv() == Env.DEV

    fun getAgentSecretKey(): String {
        if (secretKey.isNullOrBlank()) {
            synchronized(this) {
                if (secretKey.isNullOrBlank()) {
                    secretKey = getProperty(if (isDockerEnv()) DOCKER_AGENT_SECRET_KEY else AGENT_SECRET_KEY)
                    if (secretKey.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_SECRET_KEY, "Empty agent secret key")
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
                        gateway = getProperty(if (isDockerEnv()) DOCKER_GATEWAY else AGENT_GATEWAY)
                        if (gateway.isNullOrBlank()) {
                            throw PropertyNotExistException(AGENT_GATEWAY, "Empty agent gateway")
                        }
                    } catch (t: Throwable) {
                        gateway = System.getProperty("devops.gateway", "")
                    }
                    logger.info("Get the gateway($gateway)")
                }
            }
        }
        return gateway!!
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

    fun is32BitSystem() = System.getProperty("sun.arch.data.model") == "32"

    private fun getProperty(prop: String): String? {
        val buildType = BuildEnv.getBuildType()
        if (buildType == BuildType.DOCKER) {
            logger.info("buildType is $buildType")
            return getEnv(prop)
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

    private fun getEnv(prop: String): String? {
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
}

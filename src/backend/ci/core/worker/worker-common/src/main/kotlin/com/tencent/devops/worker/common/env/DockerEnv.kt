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

import com.tencent.devops.worker.common.exception.PropertyNotExistException
import org.slf4j.LoggerFactory

object DockerEnv {
    private val logger = LoggerFactory.getLogger(DockerEnv::class.java)

    private const val PROJECT_ID = "devops_project_id"
    private const val BUILD_ID = "devops_build_id"
    private const val AGENT_ID = "devops_agent_id"
    private const val AGENT_SECRET_KEY = "devops_agent_secret_key"
    private const val AGENT_GATEWAY = "devops_gateway"
    private const val JOB_POOL = "JOB_POOL"
    private const val DOCKER_HOST_IP = "docker_host_ip"
    private const val DOCKER_HOST_PORT = "docker_host_port"
    private const val HOSTNAME = "HOSTNAME"

    private var projectId: String? = null
    private var buildId: String? = null
    private var agentId: String? = null
    private var secretKey: String? = null
    private var gateway: String? = null
    private var jobPool: String? = null
    private var dockerHostIp: String? = null
    private var dockerHostPort: String? = null
    private var hostname: String? = null

    fun setProjectId(projectId: String) {
        System.setProperty(PROJECT_ID, projectId)
    }

    fun getProjectId(): String {
        if (projectId.isNullOrBlank()) {
            synchronized(this) {
                if (projectId.isNullOrBlank()) {
                    projectId = getProperty(PROJECT_ID)
                    if (projectId.isNullOrBlank()) {
                        throw PropertyNotExistException(PROJECT_ID, "Empty project Id")
                    }
                    logger.info("Get the project ID($projectId)")
                }
            }
        }
        return projectId!!
    }

    fun setBuildId(buildId: String) {
        System.setProperty(BUILD_ID, buildId)
    }

    fun getBuildId(): String {
        if (buildId.isNullOrBlank()) {
            synchronized(this) {
                if (buildId.isNullOrBlank()) {
                    buildId = getProperty(BUILD_ID)
                    if (buildId.isNullOrBlank()) {
                        throw PropertyNotExistException(BUILD_ID, "Empty build Id")
                    }
                    logger.info("Get the build ID($buildId)")
                }
            }
        }
        return buildId!!
    }

    fun setAgentId(agentId: String) {
        System.setProperty(AGENT_ID, agentId)
    }

    fun getAgentId(): String {
        if (agentId.isNullOrBlank()) {
            synchronized(this) {
                if (agentId.isNullOrBlank()) {
                    agentId = getProperty(AGENT_ID)
                    if (agentId.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_ID, "Empty agent Id")
                    }
                    logger.info("Get the agent id($agentId)")
                }
            }
        }
        return agentId!!
    }

    fun setAgentSecretKey(secretKey: String) {
        System.setProperty(AGENT_SECRET_KEY, secretKey)
    }

    fun getAgentSecretKey(): String {
        if (secretKey.isNullOrBlank()) {
            synchronized(this) {
                if (secretKey.isNullOrBlank()) {
                    secretKey = getProperty(AGENT_SECRET_KEY)
                    if (secretKey.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_SECRET_KEY, "Empty agent secret key")
                    }
                    logger.info("Get the agent secret key($secretKey)")
                }
            }
        }
        return secretKey!!
    }

    fun getGatway(): String {
        if (gateway.isNullOrBlank()) {
            synchronized(this) {
                if (gateway.isNullOrBlank()) {
                    gateway = getProperty(AGENT_GATEWAY)
                    if (gateway.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_GATEWAY, "Empty agent gateway")
                    }
                    logger.info("Get the gateway($gateway)")
                }
            }
        }
        return gateway!!
    }

    fun getJobPool(): String? {
        if (jobPool.isNullOrBlank()) {
            synchronized(this) {
                if (jobPool.isNullOrBlank()) {
                    jobPool = try {
                        getProperty(JOB_POOL)
                    } catch (e: Exception) {
                        null
                    }
                    logger.info("Get the jobPool($jobPool)")
                }
            }
        }
        return jobPool
    }

    fun getDockerHostIp(): String {
        if (dockerHostIp.isNullOrBlank()) {
            synchronized(this) {
                if (dockerHostIp.isNullOrBlank()) {
                    dockerHostIp = getProperty(DOCKER_HOST_IP)
                    if (dockerHostIp.isNullOrBlank()) {
                        throw PropertyNotExistException(DOCKER_HOST_IP, "Empty dokerHostIp")
                    }
                    logger.info("Get the dokerHostIp($dockerHostIp)")
                }
            }
        }
        return dockerHostIp!!
    }

    fun getDockerHostPort(): String {
        if (dockerHostPort.isNullOrBlank()) {
            synchronized(this) {
                if (dockerHostPort.isNullOrBlank()) {
                    dockerHostPort = getProperty(DOCKER_HOST_PORT)
                    if (dockerHostPort.isNullOrBlank()) {
                        throw PropertyNotExistException(DOCKER_HOST_PORT, "Empty dockerHostPort")
                    }
                    logger.info("Get the dockerHostPort($dockerHostPort)")
                }
            }
        }
        return dockerHostPort!!
    }

    fun getHostname(): String {
        if (hostname.isNullOrBlank()) {
            synchronized(this) {
                if (hostname.isNullOrBlank()) {
                    hostname = getProperty(HOSTNAME)
                    if (hostname.isNullOrBlank()) {
                        throw PropertyNotExistException(HOSTNAME, "Empty hostname")
                    }
                    logger.info("Get the hostname($hostname)")
                }
            }
        }
        return hostname!!
    }

    private fun getProperty(prop: String): String {
        var value = System.getenv(prop)
        if (value.isNullOrBlank()) {
            // Get from java properties
            value = System.getProperty(prop)
        }
        return value
    }
}

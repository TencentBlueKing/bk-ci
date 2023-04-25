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

package com.tencent.devops.prebuild.resources

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.prebuild.api.UserPreBuildResource
import com.tencent.devops.prebuild.pojo.GitYamlString
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.enums.PreBuildPluginType
import com.tencent.devops.prebuild.service.PreBuildService
import com.tencent.devops.prebuild.v2.service.PreBuildV2Service
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPreBuildResourceImpl @Autowired constructor(
    private val preBuildService: PreBuildService,
    private val preBuildV2Service: PreBuildV2Service
) : UserPreBuildResource {
    override fun getUserProject(userId: String, accessToken: String): Result<UserProject> {
        return Result(preBuildService.getOrCreateUserProject(userId, accessToken))
    }

    override fun getOrCreateAgent(
        userId: String,
        os: OS,
        ip: String,
        hostName: String
    ): Result<ThirdPartyAgentStaticInfo> {
        return Result(preBuildService.getOrCreatePreAgent(userId, os, ip, hostName))
    }

    override fun getAgentStatus(
        userId: String,
        os: OS,
        ip: String,
        hostName: String
    ): Result<AgentStatus> {
        return Result(preBuildService.getAgentStatus(userId, os, ip, hostName))
    }

    override fun listPreProject(userId: String): Result<List<PreProject>> {
        return Result(preBuildService.listPreProject(userId))
    }

    override fun preProjectNameExist(userId: String, preProjectId: String): Result<Boolean> {
        return Result(preBuildService.projectNameExist(userId, preProjectId))
    }

    override fun manualStartup(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq
    ): Result<BuildId> {
        return try {
            val agentInfo =
                preBuildService.getAgent(userId, startUpReq.os, startUpReq.ip, startUpReq.hostname)
            if (null == agentInfo) {
                logger.error("Agent not install")
                return Result(2, "Agent未安装，请安装Agent.")
            }

            val ymlVersion = ScriptYmlUtils.parseVersion(startUpReq.yaml)
            val buildId = when (ymlVersion?.version == "v2.0") {
                true -> {
                    preBuildV2Service.startBuild(userId, preProjectId, startUpReq, agentInfo)
                }

                false -> {
                    val yaml = try {
                        val yamlStr = CiYamlUtils.formatYaml(startUpReq.yaml)
                        val yamlObject =
                            YamlUtil.getObjectMapper().readValue(yamlStr, CIBuildYaml::class.java)
                        CiYamlUtils.normalizePrebuildYaml(yamlObject)
                    } catch (e: Throwable) {
                        logger.error("Invalid yml, error message: ", e)
                        return Result(1, "YAML非法: ${e.message}")
                    }
                    preBuildService.startBuild(userId, preProjectId, startUpReq, yaml, agentInfo)
                }
            }

            Result(buildId)
        } catch (e: Throwable) {
            logger.error("startBuild failed, exception: ", e)
            Result(3, "error message: ${e.message}")
        }
    }

    override fun manualShutdown(
        userId: String,
        accessToken: String,
        preProjectId: String,
        buildId: String
    ): Result<Boolean> {
        return try {
            Result(preBuildService.shutDown(userId, accessToken, preProjectId, buildId))
        } catch (e: Throwable) {
            logger.error("shutDown failed, exception: ", e)
            Result(1, "error message: ${e.message}")
        }
    }

    override fun getBuildDetail(
        userId: String,
        preProjectId: String,
        buildId: String
    ): Result<ModelDetail> {
        return preBuildService.getBuildDetail(userId, preProjectId, buildId)
    }

    override fun getBuildLogs(
        userId: String,
        preProjectId: String,
        buildId: String,
        debugLog: Boolean?
    ): Result<QueryLogs> {
        return Result(preBuildService.getInitLogs(userId, preProjectId, buildId, debugLog))
    }

    override fun getAfterLogs(
        userId: String,
        preProjectId: String,
        buildId: String,
        start: Long,
        debugLog: Boolean?
    ): Result<QueryLogs> {
        return Result(preBuildService.getAfterLogs(userId, preProjectId, buildId, start, debugLog))
    }

    override fun getReport(userId: String, buildId: String): Result<CodeccCallback?> {
        logger.error("Resource api deprecated: $buildId")

        return Result(1, "Resource api deprecated")
    }

    override fun getHistory(
        userId: String,
        preProjectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<HistoryResponse>> {
        return Result(preBuildService.getHistory(userId, preProjectId, page, pageSize))
    }

    override fun getBuildLink(
        userId: String,
        preProjectId: String,
        buildId: String
    ): Result<String> {
        return Result(preBuildService.getBuildLink(userId, preProjectId, buildId))
    }

    override fun checkYaml(userId: String, yaml: GitYamlString): Result<String> {
        try {
            val ymlVersion = ScriptYmlUtils.parseVersion(yaml.yaml)
            when (ymlVersion?.version == "v2.0") {
                true -> {
                    return preBuildV2Service.checkYamlSchema(yaml.yaml)
                }

                false -> {
                    val yamlStr = CiYamlUtils.formatYaml(yaml.yaml)
                    val (validate, message) = preBuildService.validateCIBuildYaml(yamlStr)
                    if (!validate) {
                        logger.error("Validate yaml failed, message: $message")
                        return Result(1, "Invalid yaml: $message", message)
                    }
                    preBuildService.checkYml(yamlStr)
                }
            }
            return Result("OK")
        } catch (e: Throwable) {
            logger.error("check yaml failed, error: ${e.message}, yaml: $yaml")
            return Result(1, "Invalid yaml", e.message)
        }
    }

    override fun getPluginVersion(
        userId: String,
        pluginType: PreBuildPluginType
    ): Result<PrePluginVersion?> {
        return Result(preBuildService.getPluginVersion(userId, pluginType))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserPreBuildResourceImpl::class.java)
    }
}

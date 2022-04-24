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

package com.tencent.devops.prebuild.v2.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.CreateStagesRequest
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.service.CommonPreBuildService
import com.tencent.devops.prebuild.v2.component.PipelineLayout
import com.tencent.devops.prebuild.v2.component.PreCIYAMLValidator
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import javax.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PreBuildV2Service @Autowired constructor(
    private val preCIYAMLValidator: PreCIYAMLValidator,
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao
) : CommonPreBuildService(client, dslContext, prebuildProjectDao) {
    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildV2Service::class.java)
    }

    /**
     * 检验yaml合法性
     *
     * @param originYaml 源yaml
     * @return web api result
     */
    fun checkYamlSchema(originYaml: String): Result<String> {
        val (isPassed, ignore, errorMsg) = preCIYAMLValidator.validate(originYaml)

        return if (isPassed) {
            Result("OK")
        } else {
            Result(1, "Invalid yaml: $errorMsg")
        }
    }

    /**
     * 生成流水线并执行
     *
     * @param userId 用户Id
     * @param pipelineName 流水线名称
     * @param startUpReq 启动构建相关参数
     * @param agentInfo agent相关信息
     * @return 构建Id
     */
    fun startBuild(
        userId: String,
        pipelineName: String,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo
    ): BuildId {
        // 1.校验yaml合法性
        val (isPassed, preYamlObject, errorMsg) = preCIYAMLValidator.validate(startUpReq.yaml)
        if (!isPassed) {
            throw CustomException(Response.Status.BAD_REQUEST, "Invalid yaml: $errorMsg")
        }

        // 2.标准化处理
        val scriptBuildYaml = ScriptYmlUtils.normalizePreCiYaml(preYamlObject!!)

        // 3.生成流水线编排
        val createStagesRequest = CreateStagesRequest(userId, startUpReq, scriptBuildYaml, agentInfo, channelCode)
        val pipelineModel = PipelineLayout.Builder()
            .pipelineName(pipelineName)
            .description("From PreCI YAML 2.0")
            .creator(userId)
            .labels(emptyList())
            .stages(createStagesRequest)
            .build()

        // 4.存储upsert
        val pipelineId = createOrUpdatePipeline(userId, pipelineName, startUpReq, pipelineModel)
        val projectId = getUserProjectId(userId)

        // 5.启动流水线
        val startupResp =
            client.get(ServiceBuildResource::class).manualStartup(userId, projectId, pipelineId, mapOf(), channelCode)

        if (startupResp.isNotOk() || startupResp.data == null) {
            logger.error("Failed to start pipeline: $pipelineId, remote message: ${startupResp.message}")
            throw RemoteServiceException("Failed to start pipeline")
        }

        return BuildId(startupResp.data!!.id)
    }
}

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

package com.tencent.devops.process.engine.atom.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.v2.StreamDispatchInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.matrix.DispatchInfo
import com.tencent.devops.common.pipeline.matrix.SampleDispatchInfo
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.idc.IDCDispatchType
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.util.CommonCredentialUtils
import com.tencent.devops.process.util.StreamDispatchUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * @Description
 * @Date 2019/11/17
 * @Version 1.0
 */
@Component
@Primary
class DispatchTypeParserTxImpl @Autowired constructor(
    private val client: Client,
    private val objectMapper: ObjectMapper,
    @Qualifier(value = "commonDispatchTypeParser")
    private val commonDispatchTypeParser: DispatchTypeParser,
    private val buildVariableService: BuildVariableService
) : DispatchTypeParser {

    private val logger = LoggerFactory.getLogger(DispatchTypeParserTxImpl::class.java)

    override fun parse(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        dispatchType: DispatchType
    ) {
        if (dispatchType is StoreDispatchType) {
            if (dispatchType.imageType == ImageType.BKSTORE) {
                // 一般性处理
                commonDispatchTypeParser.parse(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    dispatchType = dispatchType
                )
                // 腾讯内部版专有处理
                if (dispatchType.imageType == ImageType.BKDEVOPS) {
                    if (dispatchType is DockerDispatchType) {
                        dispatchType.dockerBuildVersion = dispatchType.value.removePrefix("paas/")
                    } else if (dispatchType is PublicDevCloudDispathcType) {
                        // 在商店发布的蓝盾源镜像，无需凭证
                        val pool = Pool(dispatchType.value.removePrefix("/"), null, null, false, dispatchType.performanceConfigId)
                        dispatchType.image = JsonUtil.toJson(pool)
                    } else if (dispatchType is IDCDispatchType) {
                        dispatchType.image = dispatchType.value.removePrefix("paas/")
                    }
                } else {
                    // 第三方镜像
                    if (dispatchType is PublicDevCloudDispathcType) {
                        // 在商店发布的第三方源镜像，带凭证
                        genThirdDevCloudDispatchMessage(dispatchType, projectId, buildId)
                    } else if (dispatchType is IDCDispatchType) {
                        dispatchType.image = dispatchType.value
                    } else {
                        dispatchType.dockerBuildVersion = dispatchType.value
                    }
                }
            } else if (dispatchType.imageType == ImageType.BKDEVOPS) {
                // 针对非商店的旧数据处理
                if (dispatchType.value != DockerVersion.TLINUX1_2.value && dispatchType.value != DockerVersion.TLINUX2_2.value) {
                    dispatchType.dockerBuildVersion = "bkdevops/" + dispatchType.value
                    dispatchType.value = "bkdevops/" + dispatchType.value
                } else {
                    // TLINUX1.2/2.2需要后续做特殊映射
                }
                // DevCloud镜像历史数据特殊处理
                if (dispatchType is PublicDevCloudDispathcType) {
                    if (dispatchType.image != null) {
                        val pool = Pool("devcloud/" + dispatchType.image!!.removePrefix("/"), null, null, false, dispatchType.performanceConfigId)
                        dispatchType.image = JsonUtil.toJson(pool)
                    } else {
                        logger.error("dispatchType.image==null,buildId=$buildId,dispatchType=${JsonUtil.toJson(dispatchType)}")
                    }
                }
            } else {
                // 第三方镜像 DevCloud
                if (dispatchType is PublicDevCloudDispathcType) {
                    genThirdDevCloudDispatchMessage(dispatchType, projectId, buildId)
                }
            }
            logger.info("DispatchTypeParserTxImpl:AfterTransfer:dispatchType=(${JsonUtil.toJson(dispatchType)})")
        } else {
            logger.info("DispatchTypeParserTxImpl:not StoreDispatchType, no transfer")
        }
    }

    override fun parseInfo(customInfo: DispatchInfo, context: Map<String, String>): SampleDispatchInfo? {
        // 此处可以支持多种解析
        return when (customInfo) {
            is StreamDispatchInfo -> SampleDispatchInfo(
                name = customInfo.name,
                dispatchType = StreamDispatchUtils.getDispatchType(
                    client = client,
                    objectMapper = objectMapper,
                    job = customInfo.job,
                    projectCode = customInfo.projectCode,
                    defaultImage = customInfo.defaultImage,
                    resources = customInfo.resources,
                    context = context,
                    containsMatrix = true
                ),
                baseOS = StreamDispatchUtils.getBaseOs(customInfo.job, context),
                buildEnv = StreamDispatchUtils.getBuildEnv(customInfo.job, context)
            )
            else -> null
        }
    }

    private fun genThirdDevCloudDispatchMessage(
        dispatchType: PublicDevCloudDispathcType,
        projectId: String,
        buildId: String
    ) {
        var user = ""
        var password = ""
        var credentialProject = projectId
        if (!dispatchType.credentialProject.isNullOrBlank()) {
            credentialProject = dispatchType.credentialProject!!
        }
        // 通过凭证获取账号密码
        if (!dispatchType.credentialId.isNullOrBlank()) {
            val realCredentialId = EnvUtils.parseEnv(
                command = dispatchType.credentialId!!,
                data = buildVariableService.getAllVariable(projectId, buildId))
            if (realCredentialId.isNotEmpty()) {
                val ticketsMap = CommonCredentialUtils.getCredential(
                    client = client,
                    projectId = credentialProject,
                    credentialId = realCredentialId,
                    type = CredentialType.USERNAME_PASSWORD
                )
                user = ticketsMap["v1"] as String
                password = ticketsMap["v2"] as String
            }
        }
        val credential = Credential(user, password)
        val pool = Pool(dispatchType.value, credential, null, true, dispatchType.performanceConfigId)
        dispatchType.image = JsonUtil.toJson(pool)
    }
}

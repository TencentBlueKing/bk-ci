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

package com.tencent.devops.store.service.container.impl

import com.tencent.devops.common.api.constant.EXCEPTION
import com.tencent.devops.common.api.constant.NORMAL
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.store.pojo.app.ContainerResourceItem
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResourceValue
import com.tencent.devops.store.pojo.container.agent.AgentResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SampleContainerServiceImpl @Autowired constructor() : ContainerServiceImpl() {

    private val logger = LoggerFactory.getLogger(SampleContainerServiceImpl::class.java)

    override fun buildTypeEnable(buildType: BuildType, projectCode: String): Boolean {
        return true
    }

    override fun clickable(buildType: BuildType, projectCode: String, enableFlag: Boolean?): Boolean {
        return enableFlag ?: true
    }

    @Suppress("ALL")
    override fun getResource(
        userId: String,
        projectCode: String,
        containerId: String?,
        containerOS: OS,
        buildType: BuildType
    ): Pair<ContainerResource, ContainerResourceValue> {
        logger.info("getResource params:[$userId|$projectCode|$containerId|$containerOS|$buildType]")
        val containerResourceValue: List<String>?
        val resource = when (buildType) {
            BuildType.THIRD_PARTY_AGENT_ENV -> {
                val envNodeList =
                    client.get(ServiceEnvironmentResource::class).listBuildEnvs(userId, projectCode, containerOS)
                        .data // 第三方环境节点
                logger.info("the envNodeList is :$envNodeList")
                containerResourceValue = envNodeList?.map {
                    it.name
                }?.toList()
                val normalName = I18nUtil.getCodeLanMessage(NORMAL, language = I18nUtil.getLanguage(userId))
                val exceptionName = I18nUtil.getCodeLanMessage(EXCEPTION, language = I18nUtil.getLanguage(userId))
                envNodeList?.map {
                    AgentResponse(
                        id = it.envHashId,
                        name = it.name,
                        label = BuildType.THIRD_PARTY_AGENT_ENV.getI18n(I18nUtil.getRequestUserLanguage()) +
                                "（$normalName: ${it.normalNodeCount}，$exceptionName:${it.abnormalNodeCount}）",
                        sharedProjectId = it.sharedProjectId,
                        sharedUserId = it.sharedUserId
                    )
                }?.toList()
            }
            BuildType.THIRD_PARTY_AGENT_ID -> {
                val agentNodeList =
                    client.get(ServiceThirdPartyAgentResource::class).listAgents(userId, projectCode, containerOS)
                        .data // 第三方构建机
                logger.info("the agentNodeList is :$agentNodeList")
                containerResourceValue = agentNodeList?.map {
                    it.displayName
                }?.toList()
                agentNodeList?.map {
                    AgentResponse(
                        it.agentId,
                        it.displayName,
                        "/${it.ip}（${NodeStatus.getStatusName(it.status)}）"
                    )
                }
            }
            BuildType.KUBERNETES -> {
                val buildResourceRecord = buildResourceDao.getBuildResourceByContainerId(dslContext, containerId, null)
                var containerResourceList: Set<ContainerResourceItem>? = null
                if (buildResourceRecord != null && buildResourceRecord.size > 0) {
                    containerResourceList = HashSet()
                    for (buildResourceItem in buildResourceRecord) {
                        val buildResourceCode = buildResourceItem["buildResourceCode"] as String
                        containerResourceList.add(
                            ContainerResourceItem(
                                id = buildResourceCode,
                                name = buildResourceCode
                            )
                        )
                    }
                }
                val dockerList = mutableListOf<ContainerResourceItem>()
                if (null != containerResourceList) {
                    dockerList.addAll(containerResourceList.sortedBy { it.name })
                }
                val dockerBuildImageList =
                    client.get(ServiceImageResource::class).listDockerBuildImages(userId, projectCode)
                        .data // linux环境第三方镜像
                logger.info("the dockerBuildImageList is :$dockerBuildImageList")
                dockerBuildImageList?.forEach {
                    val image = it.image
                    if (null != image) {
                        val array = image.split("/paas/bkdevops/")
                        dockerList.add(ContainerResourceItem(id = array[1], name = array[1]))
                    }
                }
                containerResourceValue = dockerList.map {
                    it.name
                }.toList()
                dockerList
            }
            else -> {
                containerResourceValue = emptyList()
                emptyList<String>()
            }
        }
        return ContainerResource(resource) to ContainerResourceValue(containerResourceValue)
    }
}

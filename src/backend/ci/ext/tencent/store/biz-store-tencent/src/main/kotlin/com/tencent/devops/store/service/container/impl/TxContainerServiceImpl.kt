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
import com.tencent.devops.common.api.constant.NUM_UNIT
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.image.api.ServiceDevCloudImageResource
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.store.pojo.app.ContainerResourceItem
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResourceValue
import com.tencent.devops.store.pojo.container.agent.AgentResponse
import com.tencent.devops.store.pojo.container.macos.MacOSNode
import com.tencent.devops.store.pojo.container.pcg.PCGDockerImageResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class TxContainerServiceImpl @Autowired constructor() : ContainerServiceImpl() {

    private val logger = LoggerFactory.getLogger(TxContainerServiceImpl::class.java)

    @Autowired
    private lateinit var pcgImageServiceImpl: PCGImageServiceImpl

    override fun buildTypeEnable(buildType: BuildType, projectCode: String): Boolean {
        return when (buildType) {
            BuildType.THIRD_PARTY_PCG -> {
                pcgImageServiceImpl.projectEnable(projectCode)
            }
//            BuildType.MACOS -> {
//                macosServiceImpl.isEnable(projectCode)
//            }
            else -> true
        }
    }

    override fun clickable(buildType: BuildType, projectCode: String, enableFlag: Boolean?): Boolean {
        return enableFlag ?: buildType.clickable
    }

    override fun getResource(
        userId: String,
        projectCode: String,
        containerId: String?,
        containerOS: OS,
        buildType: BuildType
    ): Pair<ContainerResource, ContainerResourceValue> {
        logger.info("getResource params:[$userId|$projectCode|$containerId|$containerOS|$buildType")
        val containerResourceValue: List<String>?
        val resource = when (buildType) {
            BuildType.THIRD_PARTY_DEVCLOUD -> {
                val agentNodeList = client.get(ServiceNodeResource::class).listNodeByType(
                    userId = userId,
                    projectId = projectCode,
                    type = NodeType.DEVCLOUD.name
                ).data
                logger.info("the agentNodeList is :$agentNodeList")
                containerResourceValue = agentNodeList?.filter { it.nodeStatus == NodeStatus.NORMAL.name }?.map {
                    it.displayName!!
                }!!.toList()
                agentNodeList.map {
                    AgentResponse(
                        it.nodeHashId,
                        it.displayName!!,
                        "/${it.ip}（${NodeStatus.getStatusName(it.nodeStatus)}）"
                    )
                }
            }
            BuildType.THIRD_PARTY_AGENT_ENV -> {
                val envNodeList =
                    client.get(ServiceEnvironmentResource::class).listBuildEnvs(userId, projectCode, containerOS)
                        .data // 第三方环境节点
                logger.info("the envNodeList is :$envNodeList")

                containerResourceValue = envNodeList?.map {
                    it.name
                }?.toList()
                val normalName = MessageCodeUtil.getCodeLanMessage(NORMAL)
                val exceptionName = MessageCodeUtil.getCodeLanMessage(EXCEPTION)
                val numUnit = MessageCodeUtil.getCodeLanMessage(NUM_UNIT)
                envNodeList?.map {
                    AgentResponse(
                        id = it.envHashId,
                        name = it.name,
                        label = "（$normalName: ${it.normalNodeCount}$numUnit，$exceptionName:" +
                            " ${it.abnormalNodeCount}$numUnit）",
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
                        "/${it.ip}（${it.status}）"
                    )
                }
            }
            BuildType.ESXi -> {
                val macPublicBuildResourceName = MessageCodeUtil.getCodeLanMessage("macPublicBuildResourceName")
                val l = listOf(MacOSNode(macPublicBuildResourceName, macPublicBuildResourceName))
                containerResourceValue = l.map {
                    it.name
                }.toList()
                l
            }
            BuildType.DOCKER -> {
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
            BuildType.THIRD_PARTY_PCG -> {
                val l = if (pcgImageServiceImpl.projectEnable(projectCode)) {
                    pcgImageServiceImpl.getPCGImages().map {
                        val name = "${it.img_name}:${it.img_ver}:${it.os}:${it.language}"
                        PCGDockerImageResponse(name, name)
                    }.toList()
                } else {
                    emptyList()
                }
                containerResourceValue = l.map {
                    it.name
                }.toList()
                l
            }
            BuildType.PUBLIC_DEVCLOUD, BuildType.GIT_CI -> {
                val publicImageList = client.get(ServiceDevCloudImageResource::class)
                    .listDevCloudImages(userId = userId, projectId = projectCode, public = true).data // 公共镜像
                logger.info("the publicImageList is :$publicImageList")
                val projectImageList = client.get(ServiceDevCloudImageResource::class)
                    .listDevCloudImages(userId = userId, projectId = projectCode, public = false).data // 项目镜像
                logger.info("the projectImageList is :$projectImageList")

                val dockerList = mutableListOf<ContainerResourceItem>()
                publicImageList?.forEach {
                    val image = it.image
                    if (null != image) {
                        val array = image.split("/devcloud/")
                        dockerList.add(ContainerResourceItem(id = array[1], name = array[1]))
                    }
                }
                projectImageList?.forEach {
                    val image = it.image
                    if (null != image) {
                        val array = image.split("/devcloud/")
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

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

package com.tencent.devops.store.service.container.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.model.store.tables.records.TContainerRecord
import com.tencent.devops.store.dao.container.BuildResourceDao
import com.tencent.devops.store.dao.container.ContainerDao
import com.tencent.devops.store.dao.container.ContainerResourceRelDao
import com.tencent.devops.store.pojo.app.ContainerAppWithVersion
import com.tencent.devops.store.pojo.container.Container
import com.tencent.devops.store.pojo.container.ContainerBuildType
import com.tencent.devops.store.pojo.container.ContainerRequest
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResourceValue
import com.tencent.devops.store.pojo.container.ContainerResp
import com.tencent.devops.store.pojo.container.agent.AgentResponse
import com.tencent.devops.store.pojo.container.enums.ContainerRequiredEnum
import com.tencent.devops.store.service.container.ContainerAppService
import com.tencent.devops.store.service.container.ContainerService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils

/**
 * 构建容器逻辑类
 *
 * since: 2018-12-20
 */
@Service
class ContainerServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val containerDao: ContainerDao,
    private val containerResourceRelDao: ContainerResourceRelDao,
    private val buildResourceDao: BuildResourceDao,
    private val containerAppService: ContainerAppService,
    private val client: Client
) : ContainerService {

    private val logger = LoggerFactory.getLogger(ContainerServiceImpl::class.java)

    private val OSDefaultBuildType = mapOf(
        OS.WINDOWS to BuildType.THIRD_PARTY_AGENT_ID,
        OS.LINUX to BuildType.THIRD_PARTY_AGENT_ID,
        OS.MACOS to BuildType.THIRD_PARTY_AGENT_ID
    )

    /**
     * 获取所有构建容器信息
     */
    @Suppress("UNCHECKED_CAST")
    override fun getAllPipelineContainer(): Result<List<Container>> {
        val pipelineContainerList = mutableListOf<Container>()
        val pipelineContainers = containerDao.getAllPipelineContainer(dslContext, null, null)
        pipelineContainers?.forEach {
            pipelineContainerList.add(
                convertPipelineContainer(it)
            )
        }
        return Result(pipelineContainerList)
    }

    /**
     * 获取构建容器信息
     */
    override fun getAllContainerInfos(
        userId: String,
        projectCode: String,
        type: String?,
        os: OS?
    ): Result<List<ContainerResp>> {
        logger.info("the get userId is :$userId,projectCode is :$projectCode, type is :$type,os is :$os")
        val dataList = mutableListOf<ContainerResp>()
        val pipelineContainers = containerDao.getAllPipelineContainer(dslContext, type, os?.name)
        pipelineContainers?.forEach {
            val containerId = it.id
            val defaultBuildResourceRecord =
                buildResourceDao.getBuildResourceByContainerId(dslContext, containerId, true)
            val defaultBuildResourceObject =
                if (null != defaultBuildResourceRecord && defaultBuildResourceRecord.size > 0) defaultBuildResourceRecord[0] else null
            val defaultPublicBuildResource: String? = defaultBuildResourceObject?.get("buildResourceCode") as? String
            var appList: List<ContainerAppWithVersion>? = null
            val resources = HashMap<BuildType, ContainerResource>()
            val containerOS = if (!"NONE".equals(it.os, true)) {
                appList = containerAppService.listAppsWithVersion(it.os)
                logger.info("the appList is :$appList")
                if (it.os.isNullOrBlank()) {
                    null
                } else {
                    OS.valueOf(it.os)
                }
            } else {
                null
            }

            logger.info("Get the os - (${it.os})")
            val typeList = mutableListOf<ContainerBuildType>()
            BuildType.values().forEach { type ->
                if (containerOS == null || type.osList.contains(containerOS)) {
                    if (buildTypeEnable(type, projectCode)) {
                        typeList.add(ContainerBuildType(type.name, type.value, type.enableApp))
                    }
                }
                if (containerOS != null) {
                    val resource = try {
                        getResource(userId, projectCode, containerId, containerOS, type).first
                    } catch (e: Exception) {
                        logger.warn(
                            "[$userId|$projectCode|$containerId|$containerOS|$type] Fail to get the container resource",
                            e
                        )
                        null
                    }
                    if (resource != null) {
                        resources[type] = resource
                    }
                }
            }
            val pipelineContainerResp = ContainerResp(
                id = it.id,
                name = it.name,
                type = it.type,
                baseOS = it.os,
                required = ContainerRequiredEnum.getContainerRequired(it.required.toInt()),
                maxQueueMinutes = it.maxQueueMinutes,
                maxRunningMinutes = it.maxRunningMinutes,
                defaultPublicBuildResource = defaultPublicBuildResource,
                typeList = typeList,
                props = convertString(it.props),
                apps = appList,
                defaultBuildType = if (containerOS == null) null else OSDefaultBuildType[containerOS],
                resources = resources
            )
            logger.info("pipelineContainerResp is: $pipelineContainerResp")
            dataList.add(pipelineContainerResp)
        }
        return Result(dataList)
    }

    /**
     * For the pcg image, only for the enable projects
     */
    private fun buildTypeEnable(buildType: BuildType, projectId: String): Boolean {
        logger.info("buildTypeEnable buildType is :$buildType,projectId is :$projectId")
        return true
    }

    private fun getResource(
        userId: String,
        projectCode: String,
        containerId: String?,
        containerOS: OS,
        buildType: BuildType
    ): Pair<ContainerResource, ContainerResourceValue> {
        logger.info("getResource userId is :$userId,projectCode is :$projectCode,containerId is :$containerId")
        logger.info("getResource containerOS is :$containerOS,buildType is :$buildType")
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

                envNodeList?.map {
                    AgentResponse(
                        it.envHashId,
                        it.name,
                        "（正常: ${it.normalNodeCount}个，异常: ${it.abnormalNodeCount}个）"
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
            else -> {
                containerResourceValue = emptyList()
                emptyList<String>()
            }
        }
        return ContainerResource(resource) to ContainerResourceValue(containerResourceValue)
    }

    /**
     * 获取容器构建资源信息
     */
    override fun getContainerResource(
        userId: String,
        projectCode: String,
        containerOS: OS,
        buildType: BuildType
    ): Result<ContainerResourceValue?> {
        logger.info("the userId is :$userId,projectCode is :$projectCode, os: $containerOS, buildType is :$buildType")
        return try {
            Result(getResource(userId, projectCode, null, containerOS, buildType).second)
        } catch (e: Exception) {
            logger.info("get container resource error is :$e")
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    /**
     * 获取容器构建资源信息
     */
    override fun getContainerResource(
        userId: String,
        projectCode: String,
        containerId: String?,
        containerOS: OS,
        buildType: BuildType
    ): Result<ContainerResource?> {
        logger.info("the userId is :$userId,projectCode is :$projectCode, os: $containerOS, containerId is :$containerId, buildType is :$buildType")
        return try {
            Result(getResource(userId, projectCode, containerId, containerOS, buildType).first)
        } catch (e: Exception) {
            logger.info("get container resource error is :$e")
            MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
    }

    /**
     * 获取构建容器信息
     */
    override fun getPipelineContainer(id: String): Result<Container?> {
        logger.info("the get id is :{}", id)
        val pipelineContainerRecord = containerDao.getPipelineContainer(dslContext, id)
        logger.info("the pipelineContainerRecord is :{}", pipelineContainerRecord)
        return Result(
            if (pipelineContainerRecord == null) {
                null
            } else {
                convertPipelineContainer(pipelineContainerRecord)
            }
        )
    }

    /**
     * 保存构建容器信息
     */
    override fun savePipelineContainer(containerRequest: ContainerRequest): Result<Boolean> {
        logger.info("the save containerRequest is :{}", containerRequest)
        val name = containerRequest.name
        // 判断容器名称是否存在
        val count = containerDao.countByName(dslContext, name)
        if (count > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(name),
                false
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val id = UUIDUtil.generate()
            containerDao.savePipelineContainer(context, id, containerRequest)
            val resourceIdList = containerRequest.resourceIdList
            if (!CollectionUtils.isEmpty(resourceIdList)) containerResourceRelDao.batchAdd(
                context,
                id,
                resourceIdList!!
            )
        }
        return Result(true)
    }

    /**
     * 更新构建容器信息
     */
    override fun updatePipelineContainer(id: String, containerRequest: ContainerRequest): Result<Boolean> {
        logger.info("the update id is :{},the update containerRequest is :{}", id, containerRequest)
        val name = containerRequest.name
        // 判断容器名称是否存在
        val count = containerDao.countByName(dslContext, name)
        if (count > 0) {
            // 判断更新的容器名称是否属于自已
            val pipelineContainer = containerDao.getPipelineContainer(dslContext, id)
            if (null != pipelineContainer && name != pipelineContainer.name) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(name),
                    false
                )
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            containerDao.updatePipelineContainer(dslContext, id, containerRequest)
            // 先解除容器与构建资源的关联关系，然后再从新建立二者之间的关系
            containerResourceRelDao.deleteByContainerId(context, id)
            val resourceIdList = containerRequest.resourceIdList
            if (!CollectionUtils.isEmpty(resourceIdList)) containerResourceRelDao.batchAdd(
                context,
                id,
                resourceIdList!!
            )
        }
        return Result(true)
    }

    /**
     * 删除构建容器信息
     */
    override fun deletePipelineContainer(id: String): Result<Boolean> {
        logger.info("the delete id is :{}", id)
        containerDao.deletePipelineContainer(dslContext, id)
        return Result(true)
    }

    private fun convertPipelineContainer(tContainerRecord: TContainerRecord): Container {
        val resourceIdList = mutableListOf<String>()
        val containerResourceRels = containerResourceRelDao.listByContainerId(dslContext, tContainerRecord.id)
        containerResourceRels?.forEach { resourceIdList.add(it.resourceId) }
        return Container(
            id = tContainerRecord.id,
            name = tContainerRecord.name,
            type = tContainerRecord.type,
            os = tContainerRecord.os,
            required = tContainerRecord.required,
            maxQueueMinutes = tContainerRecord.maxQueueMinutes,
            maxRunningMinutes = tContainerRecord.maxRunningMinutes,
            resourceIdList = resourceIdList,
            props = convertString(tContainerRecord.props)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertString(str: String?): Map<String, Any> {
        return if (!StringUtils.isEmpty(str)) {
            JsonUtil.getObjectMapper().readValue(str, Map::class.java) as Map<String, Any>
        } else {
            mapOf()
        }
    }
}
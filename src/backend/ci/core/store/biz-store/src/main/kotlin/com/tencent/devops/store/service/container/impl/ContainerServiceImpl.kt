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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_BUILD_ENV_TYPE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TContainerRecord
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.container.BuildResourceDao
import com.tencent.devops.store.dao.container.ContainerDao
import com.tencent.devops.store.dao.container.ContainerResourceRelDao
import com.tencent.devops.store.pojo.app.ContainerAppWithVersion
import com.tencent.devops.store.pojo.common.BK_NORMAL
import com.tencent.devops.store.pojo.common.BK_TRIGGER
import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import com.tencent.devops.store.pojo.container.Container
import com.tencent.devops.store.pojo.container.ContainerBuildType
import com.tencent.devops.store.pojo.container.ContainerOsInfo
import com.tencent.devops.store.pojo.container.ContainerRequest
import com.tencent.devops.store.pojo.container.ContainerResource
import com.tencent.devops.store.pojo.container.ContainerResourceValue
import com.tencent.devops.store.pojo.container.ContainerResp
import com.tencent.devops.store.pojo.container.ContainerType
import com.tencent.devops.store.pojo.container.enums.ContainerRequiredEnum
import com.tencent.devops.store.service.container.BuildResourceService
import com.tencent.devops.store.service.container.ContainerAppService
import com.tencent.devops.store.service.container.ContainerService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.CollectionUtils

/**
 * 构建容器逻辑类
 *
 * since: 2018-12-20
 */
@Suppress("ALL")
abstract class ContainerServiceImpl @Autowired constructor() : ContainerService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var containerDao: ContainerDao
    @Autowired
    lateinit var containerResourceRelDao: ContainerResourceRelDao
    @Autowired
    lateinit var buildResourceDao: BuildResourceDao
    @Autowired
    lateinit var businessConfigDao: BusinessConfigDao
    @Autowired
    lateinit var containerAppService: ContainerAppService
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var buildResourceService: BuildResourceService

    private val logger = LoggerFactory.getLogger(ContainerServiceImpl::class.java)

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
     * 获取容器信息
     */
    override fun getAllContainers(): Result<List<ContainerType>?> {
        val containers = containerDao.getAllPipelineContainer(dslContext, null, null)
        val containerTypes = containers?.groupBy { it.type }?.map {
            val list = it.value.map {
                ContainerOsInfo(
                    os = it.os,
                    name = it.name
                )
            }
            ContainerType(
                type = it.key,
                osInfos = list
            )
        }
        return Result(containerTypes)
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
        logger.info("getAllContainerInfos params:[$userId|$projectCode|$type|$os]")
        val dataList = mutableListOf<ContainerResp>()
        val pipelineContainers = containerDao.getAllPipelineContainer(dslContext, type, os?.name)
        pipelineContainers?.forEach {
            val containerId = it.id
            val defaultBuildResourceRecord =
                buildResourceDao.getBuildResourceByContainerId(dslContext, containerId, true)
            val defaultBuildResourceObject =
                if (null != defaultBuildResourceRecord && defaultBuildResourceRecord.size > 0) {
                    defaultBuildResourceRecord[0]
                } else null
            val defaultPublicBuildResource: String? = defaultBuildResourceObject?.get("buildResourceCode") as? String
            var appList: List<ContainerAppWithVersion>? = null
            val resources = HashMap<BuildType, ContainerResource>()
            val containerOS = if (!"NONE".equals(it.os, true)) {
                appList = containerAppService.listAppsWithVersion(it.os)
                if (it.os.isNullOrBlank()) {
                    null
                } else {
                    OS.valueOf(it.os)
                }
            } else {
                null
            }

            logger.info("Get the os - (${it.os})")
            val buildTypeConfig = businessConfigDao.get(
                dslContext = dslContext,
                business = BusinessEnum.BUILD_TYPE.name,
                feature = "buildTypeConfig",
                businessValue = "config"
            )?.configValue
            val queryAllFlag = type == null && os == null // 是否查所有容器信息标识
            val typeList = mutableListOf<ContainerBuildType>()
            BuildType.values().filter { type -> type.visable == true }.forEach { type ->
                if ((containerOS == null || type.osList.contains(containerOS)) && buildTypeEnable(type, projectCode)) {
                    // 构建资源国际化转换
                    val i18nTypeName = I18nUtil.getCodeLanMessage(
                        messageCode = "buildType.${type.name}",
                        defaultMessage = type.value
                    )
                    var enableFlag: Boolean? = null
                    if (buildTypeConfig != null) {
                        val buildTypeConfigMap = JsonUtil.toMap(buildTypeConfig)
                        val dataConfigMap = buildTypeConfigMap[type.name] as? Map<*, *>
                        enableFlag = dataConfigMap?.get("enable") as? Boolean
                    }
                    typeList.add(ContainerBuildType(
                        type = type.name,
                        name = i18nTypeName,
                        enableApp = type.enableApp,
                        disabled = !clickable(buildType = type, projectCode = projectCode, enableFlag = enableFlag),
                        defaultBuildResource = buildResourceService.getDefaultBuildResource(type)
                    ))
                }
                if (!queryAllFlag && containerOS != null) {
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
            val defaultBuildType = if (containerOS == null) null else BuildType.valueOf(
                businessConfigDao.get(
                    dslContext = dslContext,
                    business = BusinessEnum.BUILD_TYPE.name,
                    feature = "defaultBuildType",
                    businessValue = containerOS.name
                )!!.configValue
            )
            val pipelineContainerResp = ContainerResp(
                id = it.id,
                name = getContainerI18nName(it.type, it.os) ?: it.name,
                type = it.type,
                baseOS = it.os,
                required = ContainerRequiredEnum.getContainerRequired(it.required.toInt()),
                maxQueueMinutes = it.maxQueueMinutes,
                maxRunningMinutes = it.maxRunningMinutes,
                defaultPublicBuildResource = defaultPublicBuildResource,
                typeList = typeList,
                props = convertString(it.props),
                apps = appList,
                defaultBuildType = defaultBuildType,
                resources = resources
            )
            logger.info("pipelineContainerResp is: $pipelineContainerResp")
            dataList.add(pipelineContainerResp)
        }
        return Result(dataList)
    }

    fun getContainerI18nName(type: String, os: String): String? {
        return when (type) {
            "normal" -> {
                I18nUtil.getCodeLanMessage(BK_NORMAL)
            }
            "vmBuild" -> {
                I18nUtil.getCodeLanMessage(BK_BUILD_ENV_TYPE + os)
            }
            "trigger" -> {
                I18nUtil.getCodeLanMessage(BK_TRIGGER)
            }
            else -> null
        }
    }

    abstract fun buildTypeEnable(buildType: BuildType, projectCode: String): Boolean

    abstract fun clickable(buildType: BuildType, projectCode: String, enableFlag: Boolean?): Boolean

    abstract fun getResource(
        userId: String,
        projectCode: String,
        containerId: String?,
        containerOS: OS,
        buildType: BuildType
    ): Pair<ContainerResource, ContainerResourceValue>

    /**
     * 获取容器构建资源信息
     */
    override fun getContainerResource(
        userId: String,
        projectCode: String,
        containerOS: OS,
        buildType: BuildType
    ): Result<ContainerResourceValue?> {
        logger.info("getContainerResource params:[$userId|$projectCode|$containerOS|$buildType]")
        return try {
            Result(getResource(userId, projectCode, null, containerOS, buildType).second)
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|getContainerResource|$projectCode|$containerOS|" +
                "$buildType|error=${ignored.message}", ignored)
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
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
        logger.info("getContainerResource params:[$userId|$projectCode|$containerId|$containerOS|$buildType]")
        return try {
            Result(getResource(userId, projectCode, containerId, containerOS, buildType).first)
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|getContainerResource|$projectCode|$containerOS|$containerId|" +
                "$buildType|error=${ignored.message}", ignored)
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        }
    }

    /**
     * 获取构建容器信息
     */
    override fun getPipelineContainer(id: String): Result<Container?> {
        val pipelineContainerRecord = containerDao.getPipelineContainer(dslContext, id)
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
        logger.info("savePipelineContainer containerRequest:$containerRequest")
        val name = containerRequest.name
        // 判断容器名称是否存在
        val count = containerDao.countByName(dslContext, name)
        if (count > 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(name),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val id = UUIDUtil.generate()
            containerDao.savePipelineContainer(context, id, containerRequest)
            val resourceIdList = containerRequest.resourceIdList
            if (!CollectionUtils.isEmpty(resourceIdList)) containerResourceRelDao.batchAdd(
                dslContext = context,
                containerId = id,
                resourceIdList = resourceIdList!!
            )
        }
        return Result(true)
    }

    /**
     * 更新构建容器信息
     */
    override fun updatePipelineContainer(id: String, containerRequest: ContainerRequest): Result<Boolean> {
        logger.info("updatePipelineContainer id:$id,containerRequest:$containerRequest")
        val name = containerRequest.name
        // 判断容器名称是否存在
        val count = containerDao.countByName(dslContext, name)
        if (count > 0) {
            // 判断更新的容器名称是否属于自已
            val pipelineContainer = containerDao.getPipelineContainer(dslContext, id)
            if (null != pipelineContainer && name != pipelineContainer.name) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(name),
                    data = false
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
                dslContext = context,
                containerId = id,
                resourceIdList = resourceIdList!!
            )
        }
        return Result(true)
    }

    /**
     * 删除构建容器信息
     */
    override fun deletePipelineContainer(id: String): Result<Boolean> {
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
        return if (!str.isNullOrBlank()) {
            JsonUtil.getObjectMapper().readValue(str, Map::class.java) as Map<String, Any>
        } else {
            mapOf()
        }
    }
}

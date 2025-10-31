/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.BusinessConfigDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.StorePipelineService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.UpdateStorePipelineModelRequest
import com.tencent.devops.store.pojo.common.config.BusinessConfigRequest
import com.tencent.devops.store.pojo.common.enums.ScopeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.UpdateStoreBaseDataPO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Suppress("ALL")
@Service
class StorePipelineServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val client: Client,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val businessConfigDao: BusinessConfigDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig
) : StorePipelineService {

    private final val pageSize = 50

    private final val featureName = "initBuildPipeline"

    private val executorService by lazy {
        Executors.newFixedThreadPool(1).apply {
            Runtime.getRuntime().addShutdownHook(Thread { shutdown() })
        }
    }

    private val logger = LoggerFactory.getLogger(StorePipelineServiceImpl::class.java)

    override fun updatePipelineModel(
        userId: String,
        updateStorePipelineModelRequest: UpdateStorePipelineModelRequest
    ): Result<Boolean> {
        val scopeType = updateStorePipelineModelRequest.scopeType
        val storeType = updateStorePipelineModelRequest.storeType
        val storeCodeList = updateStorePipelineModelRequest.storeCodeList
        val updatePipelineModel = updateStorePipelineModelRequest.pipelineModel
        val pipelineModel: String
        if (updatePipelineModel.isNullOrBlank()) {
            val pipelineModelConfig =
                businessConfigDao.get(dslContext, storeType, featureName, "PIPELINE_MODEL")
                    ?: return I18nUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.SYSTEM_ERROR,
                        language = I18nUtil.getLanguage(userId)
                    )
            pipelineModel = pipelineModelConfig.configValue
        } else {
            pipelineModel = updatePipelineModel
        }
        val innerPipelineUser = storeInnerPipelineConfig.innerPipelineUser
        when (scopeType) {
            ScopeTypeEnum.ALL.name -> {
                handleStorePublicPipelineModel(
                    storeType = storeType,
                    storeCode = null,
                    userId = innerPipelineUser,
                    pipelineModel = pipelineModel
                )
            }
            ScopeTypeEnum.SPEC.name -> {
                if (storeCodeList == null) {
                    return I18nUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("storeCodeList"),
                        language = I18nUtil.getLanguage(userId)
                    )
                }
                storeCodeList.forEach {
                    handleStorePublicPipelineModel(
                        storeType = storeType,
                        storeCode = it,
                        userId = storeInnerPipelineConfig.innerPipelineUser,
                        pipelineModel = pipelineModel
                    )
                }
            }
        }
        return Result(true)
    }

    override fun runPipeline(storeRunPipelineParam: StoreRunPipelineParam): Boolean {
        val userId = storeRunPipelineParam.userId
        val storeId = storeRunPipelineParam.storeId
        val storeBaseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: return false
        val storeCode = storeBaseRecord.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(storeBaseRecord.storeType.toInt())
        val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(dslContext, storeCode, storeType)
        val storeReleaseSpecBusService = SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
        val innerPipelineProject = storeInnerPipelineConfig.innerPipelineProject
        val innerPipelineUser = storeInnerPipelineConfig.innerPipelineUser
        val pipelineId: String
        // 生成流水线启动参数
        val startParams = storeReleaseSpecBusService.getStoreRunPipelineStartParams(storeRunPipelineParam)
        if (null == storePipelineRelRecord) {
            pipelineId = creatStorePipelineByStoreCode(storeType = storeType.name)
            storePipelineRelDao.add(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                pipelineId = pipelineId,
                projectCode = innerPipelineProject
            )
        } else {
            pipelineId = storePipelineRelRecord.pipelineId
            val buildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, storeId)
            // 判断插件版本最近一次的构建是否完成
            val buildResult = if (buildInfoRecord != null) {
                client.get(ServiceBuildResource::class).getBuildStatus(
                    userId = innerPipelineUser,
                    projectId = innerPipelineProject,
                    pipelineId = buildInfoRecord.pipelineId,
                    buildId = buildInfoRecord.buildId,
                    channelCode = ChannelCode.AM
                ).data
            } else {
                null
            }
            if (buildResult != null) {
                val buildStatus = BuildStatus.parse(buildResult.status)
                if (!buildStatus.isFinish()) {
                    // 最近一次构建还未完全结束，给出错误提示
                    throw ErrorCodeException(
                        errorCode = StoreMessageCode.STORE_VERSION_IS_NOT_FINISH,
                        params = arrayOf(storeBaseRecord.name, storeBaseRecord.version)
                    )
                }
            }
        }
        // 触发执行流水线
        val buildIdObj = client.get(ServiceBuildResource::class).manualStartupNew(
            userId = innerPipelineUser,
            projectId = innerPipelineProject,
            pipelineId = pipelineId,
            values = startParams,
            channelCode = ChannelCode.AM,
            startType = StartType.SERVICE
        ).data
        var buildId: String? = null
        if (null != buildIdObj) {
            buildId = buildIdObj.id
            storePipelineBuildRelDao.add(
                dslContext = dslContext,
                storeId = storeId,
                pipelineId = pipelineId,
                buildId = buildId
            )
        }
        val storeStatus = storeReleaseSpecBusService.getStoreRunPipelineStatus(buildId)
        storeStatus?.let {
            storeBaseManageDao.updateStoreBaseInfo(
                dslContext = dslContext,
                updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                    id = storeId,
                    status = storeStatus,
                    modifier = userId
                )
            )
        }
        return true
    }

    override fun deleteStoreInnerPipeline(
        userId: String,
        storeType: StoreTypeEnum?,
        storeCode: String?,
        excludeProjectCode: String?
    ): Boolean {
        executorService.execute {
            logger.info("begin deleteStoreInnerPipeline!!")
            try {
                doDeleteStoreInnerPipelineBus(
                    storeType = storeType,
                    storeCode = storeCode,
                    excludeProjectCode = excludeProjectCode,
                    userId = userId
                )
            } catch (ignored: Throwable) {
                logger.error("deleteStoreInnerPipeline error | storeType=$storeType | storeCode=$storeCode", ignored)
            }
            logger.info("end deleteStoreInnerPipeline!!")
        }
        return true
    }

    private fun doDeleteStoreInnerPipelineBus(
        storeType: StoreTypeEnum?,
        storeCode: String?,
        excludeProjectCode: String?,
        userId: String
    ) {
        var offset = 0
        do {
            // 查询组件内置流水线信息记录
            val storePipelineRelRecords = storePipelineRelDao.getStorePipelineRelRecords(
                dslContext = dslContext,
                offset = offset,
                limit = pageSize,
                storeType = storeType,
                storeCode = storeCode
            )
            storePipelineRelRecords?.forEach { storePipelineRelRecord ->
                var initProjectCode = storePipelineRelRecord.projectCode
                if (excludeProjectCode == initProjectCode) {
                    // 如果内置流水线的项目属于要排除的项目，则不删除该内置流水线
                    return@forEach
                }
                storePipelineBuildRelDao.deleteStorePipelineBuildRelByPipelineId(
                    dslContext,
                    storePipelineRelRecord.pipelineId
                )
                storePipelineRelDao.deleteStorePipelineRelById(dslContext, storePipelineRelRecord.id)
                if (initProjectCode.isNullOrBlank()) {
                    initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                        dslContext = dslContext,
                        storeCode = storePipelineRelRecord.storeCode,
                        storeType = storePipelineRelRecord.storeType
                    )
                }
                // 调接口删除内置流水线
                client.get(ServicePipelineResource::class).delete(
                    userId = userId,
                    pipelineId = storePipelineRelRecord.pipelineId,
                    channelCode = ChannelCode.AM,
                    projectId = initProjectCode,
                    checkFlag = false
                )
            }
            offset += pageSize
        } while (storePipelineRelRecords?.size == pageSize)
    }

    private fun handleStorePublicPipelineModel(
        storeType: String,
        userId: String,
        pipelineModel: String,
        storeCode: String? = null
    ) {
        val str = "#{$KEY_PIPELINE_NAME}"
        var model = pipelineModel
        val suffix = storeCode ?: "PUBLIC"
        val pipelineName = getPublicPipelineName(storeType, suffix)
        val projectCode = storeInnerPipelineConfig.innerPipelineProject
        if (pipelineModel.contains(str)) {
            model = pipelineModel.replace(
                "#{$KEY_PIPELINE_NAME}",
                pipelineName
            )
        }
        var publicPipelineId = redisOperation.get(getPublicPipelineName(storeType, "PUBLIC"))
        if (publicPipelineId.isNullOrBlank()) {
            publicPipelineId = creatStorePipelineByStoreCode(storeType = storeType)
        }
        var pipelineId = if (storeCode != null) {
            storePipelineRelDao.getStorePipelineRelByStoreCode(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = StoreTypeEnum.valueOf(storeType)
            )?.pipelineId
        } else {
            publicPipelineId
        }
        pipelineId ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
            params = arrayOf(storeCode ?: getPublicPipelineName(storeType, "PUBLIC")
            )
        )
        // 对已托管给公共项目的指定组件刷新内置流水线model时则给组件在公共项目下创建单独的流水线
        if (storeCode != null && pipelineId == publicPipelineId) {
            pipelineId = creatStorePipelineByStoreCode(
                storeCode = storeCode,
                storeType = storeType
            )

            val pipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = StoreTypeEnum.valueOf(storeType)
            )
            if (pipelineRelRecord == null) {
                storePipelineRelDao.add(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = StoreTypeEnum.valueOf(storeType),
                    pipelineId = pipelineId,
                    projectCode = storeInnerPipelineConfig.innerPipelineProject
                )
            } else {
                storePipelineRelDao.updateStorePipelineProject(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = StoreTypeEnum.valueOf(storeType),
                    projectCode = storeInnerPipelineConfig.innerPipelineProject,
                    pipelineId = pipelineId
                )
            }
        }
        val flag = client.get(ServicePipelineSettingResource::class).getPipelineSetting(
            projectId = projectCode,
            pipelineId = pipelineId,
            channelCode = ChannelCode.AM
        ).data != null
        // 公共项目直接更新
        if (flag) {
            client.get(ServicePipelineSettingResource::class)
                .updatePipelineModel(
                    userId = userId,
                    updatePipelineModelRequest = UpdatePipelineModelRequest(
                        pipelineModelVersionList = listOf(
                            PipelineModelVersion(
                                projectId = projectCode,
                                pipelineId = pipelineId,
                                creator = storeInnerPipelineConfig.innerPipelineUser,
                                model = model,
                                channelCode = ChannelCode.AM
                            )
                        )
                    )
                )
        }
    }

    private fun getPublicPipelineName(storeType: String, suffix: String): String {
        return "$storeType-PIPELINE-BUILD:$suffix"
    }

    override fun creatStorePipelineByStoreCode(
        storeCode: String?,
        storeType: String
    ): String {
        val suffix = storeCode ?: "PUBLIC"
        val pipelineName = getPublicPipelineName(storeType, suffix)
        var key = "CREAT-$pipelineName"
        storeCode?.let { key += "-$storeCode" }
        val lock = RedisLock(redisOperation, key, 60L)
        try {
            lock.lock()
            val innerPipelineUser = storeInnerPipelineConfig.innerPipelineUser
            val innerPipelineProject = storeInnerPipelineConfig.innerPipelineProject
            // 获取已创建的公共流水线
            if (storeCode.isNullOrBlank()) {
                val pipelineIdConfig = businessConfigDao.get(
                    dslContext = dslContext,
                    business = StoreTypeEnum.valueOf(storeType).name,
                    feature = "initBuildPipeline",
                    businessValue = "PIPELINE_ID"
                )
                pipelineIdConfig?.let {
                    return it.configValue
                }
            }
            val pipelineModelConfig = businessConfigDao.get(
                dslContext = dslContext,
                business = StoreTypeEnum.valueOf(storeType).name,
                feature = "initBuildPipeline",
                businessValue = "PIPELINE_MODEL"
            )
            val pipelineModel = pipelineModelConfig!!.configValue.replace(
                "#{$KEY_PIPELINE_NAME}",
                pipelineName
            )
            val model = JsonUtil.to(pipelineModel, Model::class.java)
            val pipelineId = client.get(ServicePipelineResource::class).create(
                userId = innerPipelineUser,
                projectId = innerPipelineProject,
                pipeline = model,
                channelCode = ChannelCode.AM
            ).data!!.id
            if (storeCode == null) {
                redisOperation.set(
                    key = pipelineName,
                    value = pipelineId,
                    expired = false
                )
                // 持久化公共组件内置流水线
                businessConfigDao.add(
                    dslContext = dslContext,
                    request = BusinessConfigRequest(
                        business = StoreTypeEnum.valueOf(storeType).name,
                        feature = "initBuildPipeline",
                        businessValue = "PIPELINE_ID",
                        configValue = pipelineId,
                        description = "${StoreTypeEnum.valueOf(storeType).name} init build pipeline id"
                    )
                )
            }
            return pipelineId
        } finally {
            lock.unlock()
        }
    }
}

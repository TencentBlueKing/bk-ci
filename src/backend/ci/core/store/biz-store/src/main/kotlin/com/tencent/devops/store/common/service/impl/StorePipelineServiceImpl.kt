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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_BRANCH
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_PATH
import com.tencent.devops.common.api.constant.KEY_SCRIPT
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.StoreInitPipelineReq
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineInitResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.AbstractStoreCommonDao
import com.tencent.devops.store.common.dao.BusinessConfigDao
import com.tencent.devops.store.common.dao.OperationLogDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreBuildInfoDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.StorePipelineService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_PROJECT_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.OperationLogCreateRequest
import com.tencent.devops.store.pojo.common.UpdateStorePipelineModelRequest
import com.tencent.devops.store.pojo.common.enums.ScopeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.UpdateStoreBaseDataPO
import org.apache.commons.lang3.StringEscapeUtils
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
    private val gray: Gray,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val businessConfigDao: BusinessConfigDao,
    private val storeBuildInfoDao: StoreBuildInfoDao,
    private val operationLogDao: OperationLogDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig
) : StorePipelineService {

    private final val pageSize = 50

    private final val handlePageKeyPrefix = "updatePipelineModel:handlePage"

    private final val featureName = "initBuildPipeline"

    private val executorService = Executors.newSingleThreadScheduledExecutor()

    private val logger = LoggerFactory.getLogger(StorePipelineServiceImpl::class.java)

    override fun updatePipelineModel(
        userId: String,
        updateStorePipelineModelRequest: UpdateStorePipelineModelRequest
    ): Result<Boolean> {
        val taskId = UUIDUtil.generate()
        val scopeType = updateStorePipelineModelRequest.scopeType
        val storeType = updateStorePipelineModelRequest.storeType
        val storeCodeList = updateStorePipelineModelRequest.storeCodeList
        val updatePipelineModel = updateStorePipelineModelRequest.pipelineModel
        val pipelineModel: String
        val grayPipelineModel: String
        if (updatePipelineModel.isNullOrBlank()) {
            val pipelineModelConfig =
                businessConfigDao.get(dslContext, storeType, featureName, "PIPELINE_MODEL")
                    ?: return I18nUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.SYSTEM_ERROR,
                        language = I18nUtil.getLanguage(userId)
                    )
            val grayPipelineModelConfig =
                businessConfigDao.get(dslContext, storeType, featureName, "GRAY_PIPELINE_MODEL")
                    ?: return I18nUtil.generateResponseDataObject(
                        messageCode = CommonMessageCode.SYSTEM_ERROR,
                        language = I18nUtil.getLanguage(userId)
                    )
            pipelineModel = pipelineModelConfig.configValue
            grayPipelineModel = grayPipelineModelConfig.configValue
        } else {
            pipelineModel = updatePipelineModel
            grayPipelineModel = updatePipelineModel
        }
        when (scopeType) {
            ScopeTypeEnum.ALL.name -> {
                handleStorePipelineModel(
                    storeType = storeType,
                    taskId = taskId,
                    userId = userId,
                    pipelineModel = grayPipelineModel,
                    grayFlag = true
                )
                handleStorePipelineModel(
                    storeType = storeType,
                    taskId = taskId,
                    userId = userId,
                    pipelineModel = pipelineModel,
                    grayFlag = false
                )
            }
            ScopeTypeEnum.GRAY.name -> {
                handleStorePipelineModel(
                    storeType = storeType,
                    taskId = taskId,
                    userId = userId,
                    pipelineModel = grayPipelineModel,
                    grayFlag = true
                )
            }
            ScopeTypeEnum.NO_GRAY.name -> {
                handleStorePipelineModel(
                    storeType = storeType,
                    taskId = taskId,
                    userId = userId,
                    pipelineModel = pipelineModel,
                    grayFlag = false
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
                updatePipelineModel(
                    storeType = storeType,
                    storeCodeList = storeCodeList,
                    userId = userId,
                    taskId = taskId,
                    defaultPipelineModel = pipelineModel,
                    checkGrayFlag = true,
                    grayPipelineModel = grayPipelineModel
                )
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
        // 生成流水线启动参数
        val startParams = storeReleaseSpecBusService.getStoreRunPipelineStartParams(storeRunPipelineParam)
        if (null == storePipelineRelRecord) {
            val pipelineModelConfig = businessConfigDao.get(
                dslContext = dslContext,
                business = storeType.name,
                feature = "initBuildPipeline",
                businessValue = "PIPELINE_MODEL"
            )
            var pipelineModel = pipelineModelConfig!!.configValue
            val pipelineName = "am-$storeType-$storeCode-${UUIDUtil.generate()}"
            val paramMap = mapOf(
                KEY_PIPELINE_NAME to pipelineName
            )
            // 将流水线模型中的变量替换成具体的值
            paramMap.forEach { (key, value) ->
                pipelineModel = pipelineModel.replace("#{$key}", value)
            }
            val storeInitPipelineReq = StoreInitPipelineReq(
                pipelineModel = pipelineModel,
                startParams = startParams
            )
            val storeInitPipelineResp = client.get(ServicePipelineInitResource::class)
                .initStorePipeline(innerPipelineUser, innerPipelineProject, storeInitPipelineReq).data
            logger.info("runStorePipeline storeInitPipelineResp is:$storeInitPipelineResp")
            if (null != storeInitPipelineResp) {
                val pipelineId = storeInitPipelineResp.pipelineId
                storePipelineRelDao.add(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType,
                    pipelineId = pipelineId,
                    projectCode = innerPipelineProject
                )
                val buildId = storeInitPipelineResp.buildId
                if (!buildId.isNullOrBlank()) {
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
            }
        } else {
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
            // 触发执行流水线
            val pipelineId = storePipelineRelRecord.pipelineId
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
        }
        return true
    }

    override fun deleteStoreInnerPipeline(
        userId: String,
        storeType: StoreTypeEnum?,
        storeCode: String?,
        excludeProjectCode: String?
    ): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin deleteStoreInnerPipeline!!")
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
            logger.info("end deleteStoreInnerPipeline!!")
        }
        return true
    }

    private fun handleStorePipelineModel(
        storeType: String,
        taskId: String,
        userId: String,
        pipelineModel: String,
        grayFlag: Boolean? = null
    ) {
        val grayProjectSet = gray.grayProjectSet(redisOperation)
        executorService.submit<Unit> {
            loop@ while (true) {
                val projectRelCount = storeProjectRelDao.getStoreInitProjectCount(
                    dslContext = dslContext,
                    storeType = StoreTypeEnum.valueOf(storeType).type.toByte(),
                    descFlag = false,
                    grayFlag = grayFlag,
                    grayProjectCodeList = if (grayFlag != null) grayProjectSet else null
                )
                // 获取实际处理的页数
                val handlePage = redisOperation.get("$handlePageKeyPrefix:$taskId")?.toInt() ?: 0
                // 计算当前实际的页数
                val actPage = PageUtil.calTotalPage(pageSize, projectRelCount)
                if (projectRelCount > 0 && actPage > handlePage) {
                    val page = handlePage + 1
                    val projectRelRecords = storeProjectRelDao.getStoreInitProjects(
                        dslContext = dslContext,
                        storeType = StoreTypeEnum.valueOf(storeType).type.toByte(),
                        descFlag = false,
                        grayFlag = grayFlag,
                        grayProjectCodeList = if (grayFlag != null) grayProjectSet else null,
                        page = page,
                        pageSize = pageSize
                    )
                    val grayStoreCodeList =
                        projectRelRecords?.getValues(TStoreProjectRel.T_STORE_PROJECT_REL.STORE_CODE)
                    if (!grayStoreCodeList.isNullOrEmpty()) {
                        updatePipelineModel(
                            storeType = storeType,
                            storeCodeList = grayStoreCodeList,
                            userId = userId,
                            taskId = taskId,
                            defaultPipelineModel = pipelineModel
                        )
                    }
                    // 把当前任务处理的页数放入redis缓存
                    redisOperation.set("$handlePageKeyPrefix:$taskId", page.toString())
                } else {
                    redisOperation.delete("$handlePageKeyPrefix:$taskId")
                    break@loop
                }
            }
        }
    }

    private fun updatePipelineModel(
        storeType: String,
        storeCodeList: List<String>,
        userId: String,
        taskId: String,
        defaultPipelineModel: String,
        checkGrayFlag: Boolean = false,
        grayPipelineModel: String? = null
    ) {
        if (checkGrayFlag && (grayPipelineModel == null)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                params = arrayOf("grayPipelineModel")
            )
        }
        // 获取研发商店组件信息
        var storeInfoRecords = storeBaseQueryDao.getLatestStoreInfoListByCodes(
            dslContext = dslContext,
            storeType = StoreTypeEnum.valueOf(storeType),
            storeCodeList = storeCodeList
        )
        if (storeInfoRecords.isNullOrEmpty()) {
            val storeCommonDao =
                SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
            storeInfoRecords = storeCommonDao.getLatestStoreInfoListByCodes(dslContext, storeCodeList)
        }
        val pipelineModelVersionList = mutableListOf<PipelineModelVersion>()
        storeInfoRecords?.forEach { storeInfoRecord ->
            val storeInfoMap = storeInfoRecord.intoMap()
            val projectCode =
                storeInfoMap[KEY_PROJECT_CODE]?.toString() ?: storeInnerPipelineConfig.innerPipelineProject
            val storeCode = storeInfoMap[KEY_STORE_CODE] as String
            val pipelineName = "am-$storeCode-${UUIDUtil.generate()}"
            val paramMap = mutableMapOf(
                KEY_PIPELINE_NAME to pipelineName,
                KEY_STORE_CODE to storeCode,
                KEY_VERSION to storeInfoMap[KEY_VERSION]
            )
            val language = storeInfoMap[KEY_LANGUAGE]?.toString()
            language?.let {
                paramMap[KEY_LANGUAGE] = language
                val storeBuildInfoRecord = storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext = dslContext,
                    language = language,
                    storeType = StoreTypeEnum.valueOf(storeType)
                )
                storeBuildInfoRecord?.let {
                    paramMap[KEY_SCRIPT] = StringEscapeUtils.escapeJava(storeBuildInfoRecord.script)
                    paramMap[KEY_REPOSITORY_PATH] = storeBuildInfoRecord.repositoryPath ?: ""
                }
            }
            val repositoryHashId = storeInfoMap[KEY_REPOSITORY_HASH_ID]?.toString()
            repositoryHashId?.let {
                paramMap[KEY_REPOSITORY_HASH_ID] = repositoryHashId
            }
            val branch = storeInfoMap[KEY_BRANCH]?.toString()
            branch?.let {
                paramMap[KEY_BRANCH] = branch
            }
            // 将流水线模型中的变量替换成具体的值
            var convertModel = if (checkGrayFlag) {
                val grayProjectSet = gray.grayProjectSet(redisOperation)
                if (grayProjectSet.contains(projectCode)) grayPipelineModel!! else defaultPipelineModel
            } else {
                defaultPipelineModel
            }
            paramMap.forEach { (key, value) ->
                if (value != null) {
                    convertModel = convertModel.replace("#{$key}", value.toString())
                }
            }
            pipelineModelVersionList.add(
                PipelineModelVersion(
                    projectId = projectCode,
                    pipelineId = storeInfoMap[KEY_PIPELINE_ID] as String,
                    creator = storeInfoMap[KEY_CREATOR] as String,
                    model = convertModel
                )
            )
        }
        try {
            val updatePipelineModelResult = client.get(ServicePipelineSettingResource::class)
                .updatePipelineModel(
                    userId = userId,
                    updatePipelineModelRequest = UpdatePipelineModelRequest(
                        pipelineModelVersionList = pipelineModelVersionList
                    )
                )
            if (updatePipelineModelResult.isNotOk()) {
                batchAddOperateLogs(storeCodeList, storeType, userId, taskId)
            }
        } catch (ignored: Throwable) {
            logger.error("BKSystemErrorMonitor|updatePipelineModel|error=${ignored.message}", ignored)
            // 将刷新失败的组件信息入库
            batchAddOperateLogs(storeCodeList, storeType, userId, taskId)
        }
    }

    private fun batchAddOperateLogs(
        storeCodeList: List<String>,
        storeType: String,
        userId: String,
        taskId: String
    ) {
        val operationLogList = mutableListOf<OperationLogCreateRequest>()
        storeCodeList.forEach {
            operationLogList.add(
                OperationLogCreateRequest(
                    storeCode = it,
                    storeType = StoreTypeEnum.valueOf(storeType).type.toByte(),
                    optType = StoreOperationTypeEnum.UPDATE_PIPELINE_MODEL.name,
                    optUser = userId,
                    optDesc = taskId
                )
            )
        }
        operationLogDao.batchAddLogs(dslContext, userId, operationLogList)
    }
}

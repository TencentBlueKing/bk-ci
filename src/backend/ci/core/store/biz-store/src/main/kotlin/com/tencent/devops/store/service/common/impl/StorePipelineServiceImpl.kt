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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_PATH
import com.tencent.devops.common.api.constant.KEY_SCRIPT
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.OperationLogDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_PROJECT_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.OperationLogCreateRequest
import com.tencent.devops.store.pojo.common.UpdateStorePipelineModelRequest
import com.tencent.devops.store.pojo.common.enums.ScopeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StorePipelineService
import java.util.concurrent.Executors
import org.apache.commons.lang3.StringEscapeUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StorePipelineServiceImpl : StorePipelineService {

    @Autowired
    private lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    private lateinit var businessConfigDao: BusinessConfigDao

    @Autowired
    private lateinit var operationLogDao: OperationLogDao

    @Autowired
    private lateinit var redisOperation: RedisOperation

    @Autowired
    private lateinit var gray: Gray

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var client: Client

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
                    if (grayStoreCodeList != null && grayStoreCodeList.isNotEmpty()) {
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
        val storeCommonDao =
            SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
        // 获取研发商店组件信息
        val storeInfoRecords = storeCommonDao.getLatestStoreInfoListByCodes(dslContext, storeCodeList)
        val pipelineModelVersionList = mutableListOf<PipelineModelVersion>()
        storeInfoRecords?.forEach { storeInfo ->
            val projectCode = storeInfo[KEY_PROJECT_CODE] as String
            val storeCode = storeInfo[KEY_STORE_CODE] as String
            var pipelineName = "am-$projectCode-$storeCode-${System.currentTimeMillis()}"
            if (pipelineName.toCharArray().size > 128) {
                pipelineName = "am-$storeCode-${UUIDUtil.generate()}"
            }
            val paramMap = mapOf(
                KEY_PIPELINE_NAME to pipelineName,
                KEY_STORE_CODE to storeCode,
                KEY_VERSION to storeInfo[KEY_VERSION],
                KEY_LANGUAGE to storeInfo[KEY_LANGUAGE],
                KEY_SCRIPT to StringEscapeUtils.escapeJava(storeInfo[KEY_SCRIPT] as String),
                KEY_REPOSITORY_HASH_ID to storeInfo[KEY_REPOSITORY_HASH_ID],
                KEY_REPOSITORY_PATH to (storeInfo[KEY_REPOSITORY_PATH] ?: "")
            )
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
                    pipelineId = storeInfo[KEY_PIPELINE_ID] as String,
                    creator = storeInfo[KEY_CREATOR] as String,
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
            logger.info("updatePipelineModelResult:$updatePipelineModelResult")
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

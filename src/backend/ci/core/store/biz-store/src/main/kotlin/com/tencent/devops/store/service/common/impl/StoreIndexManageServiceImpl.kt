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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TStoreIndexBaseInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexLevelInfoRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.store.dao.common.StoreIndexBaseInfoDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.StoreIndexBaseInfo
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StorePipelineBusTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.index.StoreIndexCreateRequest
import com.tencent.devops.store.pojo.common.index.StoreIndexPipelineInitRequest
import com.tencent.devops.store.service.common.StoreIndexManageService
import com.tencent.devops.store.service.common.StoreIndexPipelineService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreIndexManageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeIndexPipelineService: StoreIndexPipelineService,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storeIndexBaseInfoDao: StoreIndexBaseInfoDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val client: Client,
    private val redisOperation: RedisOperation
) : StoreIndexManageService {

    override fun add(userId: String, storeIndexCreateRequest: StoreIndexCreateRequest): Result<Boolean> {
        //管理员权限校验

        val indexCode = storeIndexCreateRequest.indexCode
        val validateResult = validateAddStoreIndexCreateReq(storeIndexCreateRequest)
        if (validateResult != null) {
            logger.info("the validateResult is :$validateResult")
            return validateResult
        }
        val storeIndexBaseInfoId = UUIDUtil.generate()
        val tStoreIndexBaseInfoRecord = TStoreIndexBaseInfoRecord()
        tStoreIndexBaseInfoRecord.id = storeIndexBaseInfoId
        tStoreIndexBaseInfoRecord.storeType = storeIndexCreateRequest.storeType.type.toByte()
        tStoreIndexBaseInfoRecord.indexCode = indexCode
        tStoreIndexBaseInfoRecord.indexName = storeIndexCreateRequest.indexName
        tStoreIndexBaseInfoRecord.iconUrl = storeIndexCreateRequest.iconUrl
        tStoreIndexBaseInfoRecord.iconTips = storeIndexCreateRequest.iconTips
        tStoreIndexBaseInfoRecord.description = storeIndexCreateRequest.description
        tStoreIndexBaseInfoRecord.operationType = storeIndexCreateRequest.operationType.name
        tStoreIndexBaseInfoRecord.atomCode = storeIndexCreateRequest.atomCode
        tStoreIndexBaseInfoRecord.executeTimeType = storeIndexCreateRequest.executeTimeType.name
        tStoreIndexBaseInfoRecord.creator = userId
        tStoreIndexBaseInfoRecord.modifier = userId
        tStoreIndexBaseInfoRecord.createTime = LocalDateTime.now()
        tStoreIndexBaseInfoRecord.updateTime = LocalDateTime.now()

        val indexLevelInfoRecords = storeIndexCreateRequest.levelInfos.map {
            val tStoreIndexLevelInfo = TStoreIndexLevelInfoRecord()
            tStoreIndexLevelInfo.id = UUIDUtil.generate()
            tStoreIndexLevelInfo.levelName = it.levelName
            tStoreIndexLevelInfo.iconCssVaule = it.iconCssValue
            tStoreIndexLevelInfo.indexId = storeIndexBaseInfoId
            tStoreIndexLevelInfo.creator = userId
            tStoreIndexLevelInfo.modifier = userId
            tStoreIndexLevelInfo.createTime = LocalDateTime.now()
            tStoreIndexLevelInfo.updateTime = LocalDateTime.now()
            tStoreIndexLevelInfo
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeIndexBaseInfoDao.createStoreIndexBaseInfo(context, tStoreIndexBaseInfoRecord)
            storeIndexBaseInfoDao.batchCreateStoreIndexLevelInfo(context, indexLevelInfoRecords)
        }
        if (storeIndexCreateRequest.executeTimeType == IndexExecuteTimeTypeEnum.INDEX_CHANGE) {
            storeIndexPipelineService.initStoreIndexPipeline(
                userId = userId,
                storeIndexPipelineInitRequest = StoreIndexPipelineInitRequest(
                    indexCode = indexCode,
                    atomCode = storeIndexCreateRequest.atomCode!!,
                    executeTimeType = storeIndexCreateRequest.executeTimeType,
                    storeType = storeIndexCreateRequest.storeType
                )
            )
        }
        return Result(true)
    }

    override fun delete(userId: String, indexId: String): Result<Boolean> {
        //管理员权限校验

        val indexBaseInfo = storeIndexBaseInfoDao.getStoreIndexBaseInfoById(dslContext, indexId) ?: return Result(false)
        val atomCode = indexBaseInfo.atomCode
        val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM,
            busType = StorePipelineBusTypeEnum.INDEX
        )
        if (storePipelineRelRecord != null) {
            val pipelineId = storePipelineRelRecord.pipelineId
            // 查询插件对应的初始化项目
            val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext = dslContext,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )!!
            val pipelineBuildInfo = client.get(ServiceBuildResource::class).getPipelineLatestBuildByIds(
                initProjectCode,
                listOf(pipelineId)
            ).data?.get(storePipelineRelRecord.pipelineId)
            pipelineBuildInfo?.let {
                if (it.status == BuildStatus.PREPARE_ENV.statusName || it.status == BuildStatus.RUNNING.statusName) {
                    client.get(ServiceBuildResource::class).manualShutdown(
                        userId = userId,
                        projectId = initProjectCode,
                        pipelineId = pipelineId,
                        buildId = it.buildId,
                        channelCode = ChannelCode.AM
                    )
                }
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeIndexBaseInfoDao.deleteTStoreIndexLevelInfo(context, indexId)
            storeIndexBaseInfoDao.deleteTStoreIndexBaseInfo(context, indexId)
        }
        // 考虑到数据量的问题，使用定时任务处理存量数据
        redisOperation.sadd("deleteStoreIndexResultKey", indexId)
        return Result(true)
    }

    /**
     * 执行删除组件指标存量数据
     */
    @Scheduled(cron = "0 * * * * ?") // 每小时执行一次
    fun deleteStoreIndexResul() {
        val lock = RedisLock(redisOperation, "deleteStoreIndexResul", 60L)
        try {
            lock.lock()
            redisOperation.getSetMembers("deleteStoreIndexResultKey")?.forEach {
                logger.info("expired indexId is: {}", it)
                storeIndexBaseInfoDao.deleteStoreIndexResulById(dslContext, it)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to offline atom: {}", ignored)
        } finally {
            lock.unlock()
        }
    }

    override fun list(userId: String, keyWords: String?, page: Int, pageSize: Int): Page<StoreIndexBaseInfo> {
        //管理员权限校验


        val count = storeIndexBaseInfoDao.count(dslContext, keyWords)
        val records = storeIndexBaseInfoDao.list(dslContext, keyWords, page, pageSize)
        // 计算任务插件通过Redis实时上报计算进度
        records.forEach {
            val totalTaskNum = redisOperation.get("${it.indexCode}_totalTaskNum")
            val finishTaskNum = redisOperation.get("${it.indexCode}_finishTaskNum")
            if (totalTaskNum != null && finishTaskNum != null) {
                it.totalTaskNum = totalTaskNum.toInt()
                it.finishTaskNum = finishTaskNum.toInt()
            }
        }
        return Page(
            count = count,
            page = page,
            pageSize = pageSize,
            records = records
        )
    }

    private fun validateAddStoreIndexCreateReq(
        storeIndexCreateRequest: StoreIndexCreateRequest
    ): Result<Boolean>? {
        val indexCode = storeIndexCreateRequest.indexCode
        // 判断插件代码是否存在
        val codeCount =
            storeIndexBaseInfoDao.getStoreIndexBaseInfoByCode(dslContext, storeIndexCreateRequest.storeType, indexCode)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(indexCode)
            )
        }
        val indexName = storeIndexCreateRequest.indexName
        // 判断插件名称是否存在
        val nameCount =
            storeIndexBaseInfoDao.getStoreIndexBaseInfoByName(dslContext, storeIndexCreateRequest.storeType, indexName)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(indexName)
            )
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StoreIndexManageServiceImpl::class.java)
    }
}
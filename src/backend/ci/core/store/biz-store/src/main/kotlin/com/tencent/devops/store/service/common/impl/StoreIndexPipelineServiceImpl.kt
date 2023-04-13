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
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_INDEX_CODE
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StorePipelineBusTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.index.StoreIndexPipelineInitRequest
import com.tencent.devops.store.service.common.StoreIndexPipelineService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreIndexPipelineServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val businessConfigDao: BusinessConfigDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val client: Client
) : StoreIndexPipelineService {

    private val logger = LoggerFactory.getLogger(StoreIndexPipelineServiceImpl::class.java)

    /**
     * 初始化研发商店指标流水线
     * @param userId 用户ID
     * @param storeIndexPipelineInitRequest 初始化研发商店指标流水线请求报文
     * @return 布尔值
     */
    override fun initStoreIndexPipeline(
        userId: String,
        storeIndexPipelineInitRequest: StoreIndexPipelineInitRequest
    ): Boolean {
        logger.info("initStoreIndexPipeline params:[$userId|$storeIndexPipelineInitRequest]")
        val storeType = storeIndexPipelineInitRequest.storeType
        val pipelineModelConfig = businessConfigDao.get(
            dslContext = dslContext,
            business = storeType.name,
            feature = "initStoreIndexPipeline",
            businessValue = "PIPELINE_MODEL"
        )
        var pipelineModelStr = pipelineModelConfig!!.configValue
        val atomCode = storeIndexPipelineInitRequest.atomCode
        val pipelineName = "am-$atomCode-${UUIDUtil.generate()}"
        val paramMap = mapOf(
            KEY_PIPELINE_NAME to pipelineName,
            KEY_ATOM_CODE to atomCode,
            KEY_VERSION to storeIndexPipelineInitRequest.atomVersion
        )
        // 将流水线模型中的变量替换成具体的值
        paramMap.forEach { (key, value) ->
            pipelineModelStr = pipelineModelStr.replace("#{$key}", value)
        }
        val pipelineModel = JsonUtil.to(pipelineModelStr, Model::class.java)
        val executeTimeType = storeIndexPipelineInitRequest.executeTimeType
        if (executeTimeType == IndexExecuteTimeTypeEnum.CRON) {
            // 如果插件的执行时机选的是定时触发方式，那么则为流水线加上定时触发插件
            addTimerTriggerElement(pipelineModel)
        }
        // 判断插件的指标计算流水线是否创建
        val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM,
            busType = StorePipelineBusTypeEnum.INDEX
        )
        var pipelineId = storePipelineRelRecord?.pipelineId
        if (storePipelineRelRecord == null) {
            // 查询插件对应的初始化项目
            val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext = dslContext,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
            // 创建指标计算流水线
            pipelineId = client.get(ServicePipelineResource::class).create(
                userId = userId,
                projectId = initProjectCode,
                pipeline = pipelineModel,
                channelCode = ChannelCode.AM
            ).data?.id ?: throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
            // 保存插件与流水线的关联关系
            storePipelineRelDao.add(
                dslContext = dslContext,
                storeCode = atomCode,
                storeType = storeType,
                pipelineId = pipelineId,
                busType = StorePipelineBusTypeEnum.INDEX
            )
        }
        if (executeTimeType == IndexExecuteTimeTypeEnum.INDEX_CHANGE) {
            // 如果插件的执行时机选的是新增指标触发方式，则启动构建
            startBuild(
                atomCode = atomCode,
                userId = userId,
                pipelineId = pipelineId!!,
                storeIndexPipelineInitRequest = storeIndexPipelineInitRequest
            )
        }
        return true
    }

    private fun addTimerTriggerElement(pipelineModel: Model) {
        val firstStage = pipelineModel.stages.first()
        val triggerContainer = firstStage.containers.first()
        val triggerContainerElements = triggerContainer.elements
        triggerContainer.elements = triggerContainerElements.plus(
            TimerTriggerElement(newExpression = listOf("0 0 3 ? * 7,1,2,3,4,5,6"))
        )
    }

    private fun startBuild(
        atomCode: String,
        userId: String,
        pipelineId: String,
        storeIndexPipelineInitRequest: StoreIndexPipelineInitRequest
    ) {
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        val buildId = client.get(ServiceBuildResource::class).manualStartupNew(
            userId = userId,
            projectId = initProjectCode,
            pipelineId = pipelineId,
            values = mapOf(KEY_INDEX_CODE to storeIndexPipelineInitRequest.indexCode),
            channelCode = ChannelCode.AM,
            startType = StartType.SERVICE
        )
        logger.info("manualStartupNew result is:$buildId")
    }
}

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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.model.store.tables.records.TStoreIndexBaseInfoRecord
import com.tencent.devops.model.store.tables.records.TStoreIndexLevelInfoRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.store.dao.common.StoreIndexManageDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.common.enums.IndexExecuteTimeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.index.StoreIndexCreateRequest
import com.tencent.devops.store.service.common.StoreIndexManageService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StoreIndexManageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storeIndexManageDao: StoreIndexManageDao,
    private val client: Client
) : StoreIndexManageService {

    @Value("\${store.computeMetricsProjectId}")
    val computeMetricsProjectId: String = ""

    override fun add(userId: String, storeIndexCreateRequest: StoreIndexCreateRequest): Boolean {
        //管理员权限校验

        val storeIndexBaseInfoId = UUIDUtil.generate()
        val tStoreIndexBaseInfoRecord = TStoreIndexBaseInfoRecord()
        tStoreIndexBaseInfoRecord.id = storeIndexBaseInfoId
        tStoreIndexBaseInfoRecord.storeType = storeIndexCreateRequest.storeType.type.toByte()
        tStoreIndexBaseInfoRecord.indexCode = storeIndexCreateRequest.indexCode
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
        storeIndexManageDao.createStoreIndexBaseInfo(dslContext, tStoreIndexBaseInfoRecord)
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
        storeIndexManageDao.batchCreateStoreIndexLevelInfo(dslContext, indexLevelInfoRecords)
        // 指标执行时机类型为指标变动则调用流水线进行指标计算
        if (storeIndexCreateRequest.executeTimeType == IndexExecuteTimeTypeEnum.INDEX_CHANGE
            && !storeIndexCreateRequest.atomCode.isNullOrBlank()) {
            val atomPipelineRelRecord = storePipelineRelDao.getStorePipelineRel(
                dslContext,
                storeIndexCreateRequest.atomCode!!
                , StoreTypeEnum.IMAGE
            )
            if (atomPipelineRelRecord != null) {
                val startParams = mapOf("plugin_code" to storeIndexCreateRequest.atomCode!!)
                client.get(ServiceBuildResource::class).manualStartupNew(
                    userId = userId,
                    projectId = computeMetricsProjectId,
                    pipelineId = atomPipelineRelRecord.pipelineId,
                    values = startParams,
                    channelCode = ChannelCode.AM,
                    startType = StartType.PIPELINE
                )
            }
        }
        return true
    }
}
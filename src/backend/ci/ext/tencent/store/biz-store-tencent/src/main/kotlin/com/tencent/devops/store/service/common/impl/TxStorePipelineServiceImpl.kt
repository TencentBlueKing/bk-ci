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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.service.ServicePipelineSettingResource
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.process.pojo.setting.UpdatePipelineModelRequest
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.UpdateStorePipelineModelRequest
import com.tencent.devops.store.pojo.common.enums.ScopeTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.TxStorePipelineService
import org.apache.commons.lang.StringEscapeUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxStorePipelineServiceImpl : TxStorePipelineService {

    @Autowired
    private lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    private lateinit var businessConfigDao: BusinessConfigDao

    @Autowired
    private lateinit var redisOperation: RedisOperation

    @Autowired
    private lateinit var gray: Gray

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var client: Client

    private val logger = LoggerFactory.getLogger(TxStorePipelineServiceImpl::class.java)

    override fun updatePipelineModel(
        userId: String,
        updateStorePipelineModelRequest: UpdateStorePipelineModelRequest
    ): Result<Boolean> {
        logger.info("updatePipelineModel userId:$userId,updateStorePipelineModelRequest:$updateStorePipelineModelRequest")
        val scopeType = updateStorePipelineModelRequest.scopeType
        val storeType = updateStorePipelineModelRequest.storeType
        val storeCodeList = updateStorePipelineModelRequest.storeCodeList
        val businessConfig =
            businessConfigDao.get(dslContext, StoreTypeEnum.ATOM.name, "initBuildPipeline", "PIPELINE_MODEL")
        when (scopeType) {
            ScopeTypeEnum.ALL.name -> {
            }
            ScopeTypeEnum.GRAY.name -> {
                val grayProjectSet = gray.grayProjectSet(redisOperation)
                val projectRelCount = storeProjectRelDao.getStoreInitProjectCount(
                    dslContext = dslContext,
                    storeType = StoreTypeEnum.valueOf(storeType).type.toByte(),
                    descFlag = false,
                    specProjectCodeList = storeCodeList,
                    grayFlag = null,
                    grayProjectCodeList = null
                )
                val projectRelRecords = storeProjectRelDao.getStoreInitProjects(
                    dslContext = dslContext,
                    storeType = StoreTypeEnum.valueOf(storeType).type.toByte(),
                    descFlag = false,
                    specProjectCodeList = null,
                    grayFlag = null,
                    grayProjectCodeList = grayProjectSet.toList(),
                    page = null,
                    pageSize = null
                )
            }
            ScopeTypeEnum.NO_GRAY.name -> {
                val grayProjectSet = gray.grayProjectSet(redisOperation)
            }
            ScopeTypeEnum.SPEC.name -> {
                val storeCommonDao =
                    SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
                if (storeCodeList == null) {
                    return MessageCodeUtil.generateResponseDataObject(
                        CommonMessageCode.PARAMETER_IS_NULL,
                        arrayOf("storeCodeList")
                    )
                }
                // 获取研发商店组件信息
                val storeInfoRecords = storeCommonDao.getLatestStoreInfoListByCodes(dslContext, storeCodeList)
                var pipelineModel = businessConfig!!.configValue
                val pipelineModelVersionList = mutableListOf<PipelineModelVersion>()
                storeInfoRecords?.forEach { storeInfo ->
                    val projectCode = storeInfo["projectCode"] as String
                    val storeCode = storeInfo["storeCode"] as String
                    val pipelineName = "am-$projectCode-$storeCode-${System.currentTimeMillis()}"
                    val paramMap = mapOf(
                        "pipelineName" to pipelineName,
                        "storeCode" to storeCode,
                        "version" to storeInfo["storeCode"],
                        "script" to StringEscapeUtils.escapeJava(storeInfo["script"] as String),
                        "repositoryHashId" to storeInfo["repositoryHashId"],
                        "repositoryPath" to storeInfo["repositoryPath"]
                    )
                    // 将流水线模型中的变量替换成具体的值
                    paramMap.forEach { (key, value) ->
                        pipelineModel = pipelineModel.replace("#{$key}", value.toString())
                    }
                    pipelineModelVersionList.add(
                        PipelineModelVersion(
                            projectId = projectCode,
                            pipelineId = storeInfo["pipelineId"] as String,
                            model = JsonUtil.to(pipelineModel, Model::class.java)
                        )
                    )
                }
                val updatePipelineModelResult = client.get(ServicePipelineSettingResource::class)
                    .updatePipelineModel(
                        userId = userId,
                        updatePipelineModelRequest = UpdatePipelineModelRequest(
                            pipelineModelVersionList = pipelineModelVersionList
                        )
                    )
                logger.info("updatePipelineModelResult:$updatePipelineModelResult")
            }
        }
        return Result(true)
    }
}

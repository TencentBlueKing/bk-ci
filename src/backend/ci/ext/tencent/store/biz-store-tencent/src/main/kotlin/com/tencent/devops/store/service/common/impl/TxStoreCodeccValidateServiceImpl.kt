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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.pojo.common.StoreCodeccValidateDetail
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.TxStoreCodeccValidateService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxStoreCodeccValidateServiceImpl @Autowired constructor(
    private val client: Client,
    private val businessConfigDao: BusinessConfigDao,
    private val dslContext: DSLContext
)  : TxStoreCodeccValidateService {

    private val logger = LoggerFactory.getLogger(TxStoreCodeccValidateServiceImpl::class.java)

    private final val toolNameEn = "tool_name_en"

    override fun validateCodeccResult(buildId: String, language: String): Result<Boolean> {
        logger.info("validateCodeccResult buildId:$buildId,language:$language")
        // 获取代码扫描校验模型
        val businessConfig = businessConfigDao.get(dslContext, StoreTypeEnum.ATOM.name, "${language}Codecc", "VALIDATE_MODEL")
        val validateModel = businessConfig!!.configValue
        val validateModelList = JsonUtil.to(validateModel, object : TypeReference<List<Map<String, Any>>>() {})
        logger.info("validateModelList is:$validateModelList")
        // 获取codecc扫描结果数据
        val codeccTaskResult = client.get(ServiceCodeccResource::class).getCodeccTaskResult(setOf(buildId))
        logger.info("codeccTaskResult is:$codeccTaskResult")
        val codeccTaskMap = codeccTaskResult.data
        if (codeccTaskResult.isNotOk() || codeccTaskMap == null) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        val codeccTask = codeccTaskMap[buildId]
        val toolSnapshotList = codeccTask!!.toolSnapshotList
        val validateMap = mutableMapOf<String, List<StoreCodeccValidateDetail>>()
        validateModelList.forEach validateItemEach@{ validateItemMap ->
            val validateToolNameEn = validateItemMap[toolNameEn]
            val validateItemList = mutableListOf<StoreCodeccValidateDetail>()
            toolSnapshotList.forEach { codeccItemMap ->
                val codeccToolNameEn = codeccItemMap[toolNameEn]
                if (validateToolNameEn == codeccToolNameEn) {
                    validateItemMap.forEach { (key, value) ->
                        if (key != validateToolNameEn) {
                            validateItemList.add(
                                StoreCodeccValidateDetail(
                                    validateKey = key,
                                    actValue = if (codeccItemMap[key] == null) "" else codeccItemMap[key].toString(),
                                    expectedValue = value.toString()
                                )
                            )
                        }
                    }
                    validateMap[validateToolNameEn.toString()] = validateItemList
                    return@validateItemEach
                }
            }
        }
        logger.info("buildId[$buildId] validateMap is:$validateMap")
        return Result(true)
    }
}

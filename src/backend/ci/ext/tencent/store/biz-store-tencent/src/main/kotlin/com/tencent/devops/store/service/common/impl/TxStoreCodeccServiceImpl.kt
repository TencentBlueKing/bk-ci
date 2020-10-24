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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccMeasureInfo
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.TxStoreCodeccService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxStoreCodeccServiceImpl @Autowired constructor(
    private val client: Client,
    private val storeMemberDao: StoreMemberDao,
    private val businessConfigDao: BusinessConfigDao,
    private val storeCommonService: StoreCommonService,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext
) : TxStoreCodeccService {

    private val logger = LoggerFactory.getLogger(TxStoreCodeccServiceImpl::class.java)

    override fun getCodeccMeasureInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeId: String?
    ): Result<CodeccMeasureInfo?> {
        logger.info("getCodeccMeasureInfo userId:$userId,storeType:$storeType,storeCode:$storeCode,storeId:$storeId")
        validatePermission(userId, storeCode, storeType)
        var commitId: String? = null
        if (storeId != null) {
            // 如果组件ID不为空则会去redis中获取当时构建拉代码存的commitId
            commitId = redisOperation.get("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$storeId")
        }
        logger.info("getCodeccMeasureInfo commitId:$commitId")
        val mameSpaceName = storeCommonService.getStoreRepoNameSpaceName(StoreTypeEnum.valueOf(storeType))
        val codeccMeasureInfoResult = client.get(ServiceCodeccResource::class).getCodeccMeasureInfo(
            repoId = "$mameSpaceName/$storeCode",
            commitId = commitId
        )
        val codeccMeasureInfo = codeccMeasureInfoResult.data
        if (codeccMeasureInfo != null) {
            val codeStyleScore = codeccMeasureInfo.codeStyleScore
            val codeSecurityScore = codeccMeasureInfo.codeSecurityScore
            val codeMeasureScore = codeccMeasureInfo.codeMeasureScore
            if (codeStyleScore != null && codeSecurityScore != null && codeMeasureScore != null) {
                val codeStyleQualifiedScore = getQualifiedScore(storeType, "codeStyle")
                val codeSecurityQualifiedScore = getQualifiedScore(storeType, "codeSecurity")
                val codeMeasureQualifiedScore = getQualifiedScore(storeType, "codeMeasure")
                // 判断插件代码库的扫描分数是否合格
                codeccMeasureInfo.qualifiedFlag =
                    codeStyleScore > codeStyleQualifiedScore && codeSecurityScore > codeSecurityQualifiedScore && codeMeasureScore > codeMeasureQualifiedScore
            }
        }
        logger.info("getCodeccMeasureInfo codeccMeasureInfoResult:$codeccMeasureInfoResult")
        return codeccMeasureInfoResult
    }

    override fun startCodeccTask(
        userId: String,
        storeType: String,
        storeCode: String,
        storeId: String?
    ): Result<String?> {
        logger.info("startCodeccTask userId:$userId,storeType:$storeType,storeCode:$storeCode,storeId:$storeId")
        validatePermission(userId, storeCode, storeType)
        var commitId: String? = null
        if (storeId != null) {
            // 如果组件ID不为空则会去redis中获取当时构建拉代码存的commitId
            commitId = redisOperation.get("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$storeId")
        }
        logger.info("startCodeccTask commitId:$commitId")
        val mameSpaceName = storeCommonService.getStoreRepoNameSpaceName(StoreTypeEnum.valueOf(storeType))
        val repoId = "$mameSpaceName/$storeCode"
        val startCodeccTaskResult = client.get(ServiceCodeccResource::class).startCodeccTask(
            repoId = repoId,
            commitId = commitId
        )
        logger.info("startCodeccTask commitId:$commitId,startCodeccTaskResult:$startCodeccTaskResult")
        if (startCodeccTaskResult.status == 2300020) {
            // 如果没有创建扫描流水线则再补偿创建
            val createCodeccPipelineResult = client.get(ServiceCodeccResource::class).createCodeccPipeline(repoId)
            logger.info("createCodeccPipelineResult is :$createCodeccPipelineResult")
            val createFlag = createCodeccPipelineResult.data
            if (createCodeccPipelineResult.isNotOk() || createFlag != true) {
                throw ErrorCodeException(
                    errorCode = createCodeccPipelineResult.status.toString(),
                    defaultMessage = createCodeccPipelineResult.message
                )
            }
            return client.get(ServiceCodeccResource::class).startCodeccTask(
                repoId = repoId,
                commitId = commitId
            )
        } else {
            return startCodeccTaskResult
        }
    }

    override fun getQualifiedScore(storeType: String, scoreType: String): Double {
        val qualifiedScoreConfig = businessConfigDao.get(
            dslContext = dslContext,
            business = storeType,
            feature = "codeccQualifiedScore",
            businessValue = scoreType
        )
        return (qualifiedScoreConfig?.configValue ?: "90").toDouble()
    }

    private fun validatePermission(userId: String, storeCode: String, storeType: String) {
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = StoreTypeEnum.valueOf(storeType).type.toByte()
            )
        ) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PERMISSION_DENIED)
        }
    }
}

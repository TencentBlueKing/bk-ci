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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccMeasureInfo
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.STORE_REPO_CODECC_BUILD_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.TxStoreCodeccCommonService
import com.tencent.devops.store.service.common.TxStoreCodeccService
import com.tencent.devops.store.service.common.TxStoreRepoService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response

@Service
class TxStoreCodeccServiceImpl @Autowired constructor(
    private val client: Client,
    private val storeMemberDao: StoreMemberDao,
    private val businessConfigDao: BusinessConfigDao,
    private val txStoreRepoService: TxStoreRepoService,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext
) : TxStoreCodeccService {

    private val logger = LoggerFactory.getLogger(TxStoreCodeccServiceImpl::class.java)

    override fun getCodeccMeasureInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeId: String?,
        buildId: String?
    ): Result<CodeccMeasureInfo?> {
        logger.info("getCodeccMeasureInfo params:[$userId|$storeType|$storeCode|$storeId|$buildId]")
        validatePermission(userId, storeCode, storeType)
        var codeccBuildId: String? = buildId
        if (codeccBuildId == null && storeId != null) {
            // 如果组件ID不为空则会去redis中获取启动codecc任务存的buildId
            codeccBuildId = redisOperation.get("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode:$storeId")
            if (codeccBuildId == null) {
                // storeId和buildI沒有建立关联关系则说明启动codecc任务失败
                return Result(
                    CodeccMeasureInfo(
                        status = 1,
                        message = MessageUtil.getCodeLanMessage(messageCode = StoreMessageCode.USER_START_CODECC_TASK_FAIL,
                            language = I18nUtil.getLanguage(userId))
                    )
                )
            }
        } else if (codeccBuildId == null && storeId == null) {
            // 适配质量管理页面启动任务后刷新页面还能看到启动任务那次的代码扫描任务详情
            codeccBuildId = redisOperation.get("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode")
        }
        logger.info("getCodeccMeasureInfo codeccBuildId:$codeccBuildId")
        val mameSpaceName = txStoreRepoService.getStoreRepoNameSpaceName(StoreTypeEnum.valueOf(storeType))
        val codeccMeasureInfoResult = client.get(ServiceCodeccResource::class).getCodeccMeasureInfo(
            repoId = "$mameSpaceName/$storeCode",
            buildId = codeccBuildId
        )
        val codeccMeasureInfo = codeccMeasureInfoResult.data
        if (codeccMeasureInfo != null) {
            val codeStyleScore = codeccMeasureInfo.codeStyleScore
            val codeSecurityScore = codeccMeasureInfo.codeSecurityScore
            val codeMeasureScore = codeccMeasureInfo.codeMeasureScore
            val codeStyleQualifiedScore = getQualifiedScore(storeType, "codeStyle")
            val codeSecurityQualifiedScore = getQualifiedScore(storeType, "codeSecurity")
            val codeMeasureQualifiedScore = getQualifiedScore(storeType, "codeMeasure")
            val qualifiedFlag = getQualifiedFlag(
                storeType = storeType,
                codeStyleScore = codeStyleScore,
                codeSecurityScore = codeSecurityScore,
                codeMeasureScore = codeMeasureScore
            )
            codeccMeasureInfo.qualifiedFlag = qualifiedFlag
            codeccMeasureInfo.codeStyleQualifiedScore = codeStyleQualifiedScore
            codeccMeasureInfo.codeSecurityQualifiedScore = codeSecurityQualifiedScore
            codeccMeasureInfo.codeMeasureQualifiedScore = codeMeasureQualifiedScore
            if (codeccMeasureInfo.status != 3) {
                if (storeId != null) {
                    getStoreCodeccCommonService(storeType).doStoreCodeccOperation(
                        qualifiedFlag = qualifiedFlag,
                        storeId = storeId,
                        storeCode = storeCode,
                        userId = userId
                    )
                } else if (buildId == null) {
                    val atomBuildId = redisOperation.get("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode")
                    if (atomBuildId != null) {
                        redisOperation.delete("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode")
                    }
                }
            }
        }
        return codeccMeasureInfoResult
    }

    override fun getQualifiedFlag(
        storeType: String,
        codeStyleScore: Double?,
        codeSecurityScore: Double?,
        codeMeasureScore: Double?
    ): Boolean {
        val codeStyleQualifiedScore = getQualifiedScore(storeType, "codeStyle")
        val codeSecurityQualifiedScore = getQualifiedScore(storeType, "codeSecurity")
        val codeMeasureQualifiedScore = getQualifiedScore(storeType, "codeMeasure")
        var qualifiedFlag = false
        if (codeStyleScore != null && codeSecurityScore != null && codeMeasureScore != null) {
            // 判断插件代码库的扫描分数是否合格
            qualifiedFlag =
                codeStyleScore >= codeStyleQualifiedScore && codeSecurityScore >= codeSecurityQualifiedScore && codeMeasureScore >= codeMeasureQualifiedScore
        }
        return qualifiedFlag
    }

    private fun getStoreCodeccCommonService(storeType: String): TxStoreCodeccCommonService {
        return SpringContextUtil.getBean(TxStoreCodeccCommonService::class.java, "${storeType}_CODECC_COMMON_SERVICE")
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
            // 如果组件ID不为空则会去redis中获取当时构建时存的commitId
            commitId = redisOperation.get("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$storeCode:$storeId")
        }
        logger.info("startCodeccTask commitId:$commitId")
        val mameSpaceName = txStoreRepoService.getStoreRepoNameSpaceName(StoreTypeEnum.valueOf(storeType))
        val repoId = "$mameSpaceName/$storeCode"
        var startCodeccTaskResult = client.get(ServiceCodeccResource::class).startCodeccTask(
            repoId = repoId,
            commitId = commitId
        )
        logger.info("startCodeccTask commitId:$commitId,startCodeccTaskResult:$startCodeccTaskResult")
        if (startCodeccTaskResult.status == 2300020) {
            // 如果没有创建扫描流水线则再补偿创建
            val storeCommonDao = SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
            val codeccLanguages = mutableListOf<String>()
            val devLanguages = storeCommonDao.getStoreDevLanguages(dslContext, storeCode)
            devLanguages?.forEach {
                codeccLanguages.add(getCodeccLanguage(it))
            }
            val createCodeccPipelineResult = client.get(ServiceCodeccResource::class).createCodeccPipeline(repoId, codeccLanguages)
            logger.info("createCodeccPipelineResult is :$createCodeccPipelineResult")
            val createFlag = createCodeccPipelineResult.data
            if (createCodeccPipelineResult.isNotOk() || createFlag != true) {
                throw ErrorCodeException(
                    statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                    errorCode = createCodeccPipelineResult.status.toString(),
                    defaultMessage = createCodeccPipelineResult.message
                )
            }
            startCodeccTaskResult = client.get(ServiceCodeccResource::class).startCodeccTask(
                repoId = repoId,
                commitId = commitId
            )
        }
        if (startCodeccTaskResult.isOk()) {
            // 后置处理操作
            getStoreCodeccCommonService(storeType).doStartTaskAfterOperation(
                userId = userId,
                storeCode = storeCode,
                storeId = storeId
            )
            // 把代码扫描构建ID存入redis
            if (storeId != null) {
                redisOperation.set(
                    key = "$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode:$storeId",
                    value = startCodeccTaskResult.data!!,
                    expired = false
                )
            } else {
                redisOperation.set(
                    key = "$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$storeCode",
                    value = startCodeccTaskResult.data!!,
                    expiredInSecond = TimeUnit.DAYS.toSeconds(3)
                )
            }
        }
        return startCodeccTaskResult
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

    override fun getCodeccLanguage(language: String): String {
        val codeccLanguageMappingConfig = businessConfigDao.get(
            dslContext = dslContext,
            business = BusinessEnum.CODECC.name,
            feature = "codeccLanguageMapping",
            businessValue = language
        )
        return codeccLanguageMappingConfig?.configValue ?: language
    }

    override fun getCodeccFlag(storeType: String): Boolean? {
        val codeccFlagConfig = businessConfigDao.get(
            dslContext = dslContext,
            business = storeType,
            feature = "codeccFlag",
            businessValue = storeType
        )
        return codeccFlagConfig?.configValue?.toBoolean()
    }

    private fun validatePermission(userId: String, storeCode: String, storeType: String) {
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = StoreTypeEnum.valueOf(storeType).type.toByte()
            )
        ) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PERMISSION_DENIED, params = arrayOf(userId))
        }
    }
}

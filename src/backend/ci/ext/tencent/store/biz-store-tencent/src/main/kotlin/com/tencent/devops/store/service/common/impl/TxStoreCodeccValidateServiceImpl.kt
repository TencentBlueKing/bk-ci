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
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.TxStoreCodeccDao
import com.tencent.devops.store.pojo.common.STORE_CODECC_FAILED_TEMPLATE_SUFFIX
import com.tencent.devops.store.pojo.common.STORE_CODECC_QUALIFIED_TEMPLATE_SUFFIX
import com.tencent.devops.store.pojo.common.StoreCodeccInfo
import com.tencent.devops.store.pojo.common.StoreValidateCodeccResultRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreNotifyService
import com.tencent.devops.store.service.common.TxStoreCodeccValidateService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.time.LocalDateTime
import kotlin.math.pow

@Service
class TxStoreCodeccValidateServiceImpl @Autowired constructor(
    private val client: Client,
    private val businessConfigDao: BusinessConfigDao,
    private val storeCodeccDao: TxStoreCodeccDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val storeNotifyService: StoreNotifyService,
    private val storeCommonService: StoreCommonService,
    private val dslContext: DSLContext
) : TxStoreCodeccValidateService {

    private val logger = LoggerFactory.getLogger(TxStoreCodeccValidateServiceImpl::class.java)

    private final val toolNameEn = "tool_name_en"

    private final val totalNewSerious = "total_new_serious"

    private final val totalNewNormal = "total_new_normal"

    private final val codeCalScoreStyle = "codeCalScoreStyle"

    private final val scoreFormat = "%.2f"

    @Value("\${codecc.detailUrl}")
    private lateinit var codeccDetailUrl: String

    override fun validateCodeccResult(
        storeValidateCodeccResultRequest: StoreValidateCodeccResultRequest
    ): Result<Boolean> {
        logger.info("validateCodeccResult storeValidateCodeccResultRequest:$storeValidateCodeccResultRequest")
        val storeType = storeValidateCodeccResultRequest.storeType
        // 获取codecc扫描结果数据
        val buildId = storeValidateCodeccResultRequest.buildId
        val codeccCallback = getCodeccCallback(buildId)
        var totalLine = 0 // 总行数
        var totalCoveritySeriousWaringCount = 0
        var totalCoverityNormalWaringCount = 0
        var totalCcnExceedNum = 0.0
        var totalSeriousRiskCount = 0
        var totalNormalRiskCount = 0
        var totalSeriousWaringCount = 0
        var totalNormalWaringCount = 0
        val codeStyleToolsConfig = businessConfigDao.get(dslContext, storeType.name, "codecc", "codeStyleTools")
        val codeStyleToolNameEnList = codeStyleToolsConfig!!.configValue.split(",")
        codeccCallback.toolSnapshotList.forEach { codeccItemMap ->
            val codeccToolNameEn = codeccItemMap[toolNameEn]
            when {
                codeccToolNameEn == "COVERITY" -> {
                    totalCoveritySeriousWaringCount = codeccItemMap[totalNewSerious] as Int
                    totalCoverityNormalWaringCount = codeccItemMap[totalNewNormal] as Int
                }
                codeccToolNameEn == "CCN" -> totalCcnExceedNum = codeccItemMap["ccn_beyond_threshold"].toString().toDouble()
                codeccToolNameEn == "WOODPECKER_SENSITIVE" -> {
                    totalSeriousRiskCount = codeccItemMap[totalNewSerious] as Int
                    totalNormalRiskCount = codeccItemMap[totalNewNormal] as Int
                }
                codeccToolNameEn == "CLOC" -> totalLine = codeccItemMap["total_lines"] as Int
                codeStyleToolNameEnList.contains(codeccToolNameEn) -> {
                    totalSeriousWaringCount = codeccItemMap[totalNewSerious] as Int
                    totalNormalWaringCount = codeccItemMap[totalNewNormal] as Int
                }
            }
        }
        // 计算代码规范评分
        val codeStyleScore = calCodeStyleScore(
            storeType = storeType,
            totalSeriousWaringCount = totalSeriousWaringCount,
            totalNormalWaringCount = totalNormalWaringCount,
            totalLine = totalLine,
            language = storeValidateCodeccResultRequest.language
        )
        // 计算代码安全评分
        val codeSecurityScore = calCodeSecurityScore(totalSeriousRiskCount, totalNormalRiskCount)
        // 计算代码度量和检查评分
        val codeMeasureScore = calCodeMeasureScore(
            storeType = storeType,
            totalCoveritySeriousWaringCount = totalCoveritySeriousWaringCount,
            totalCoverityNormalWaringCount = totalCoverityNormalWaringCount,
            totalLine = totalLine,
            totalCcnExceedNum = totalCcnExceedNum
        )
        val buildRelRecord = storePipelineBuildRelDao.getStorePipelineBuildRelByBuildId(dslContext, buildId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(buildId)
            )
        // 将评分信息入库
        storeCodeccDao.addStoreCodeccScore(
            dslContext = dslContext,
            userId = storeValidateCodeccResultRequest.userId,
            storeCodeccInfo = StoreCodeccInfo(
                storeId = buildRelRecord.storeId,
                storeCode = storeValidateCodeccResultRequest.storeCode,
                storeType = storeValidateCodeccResultRequest.storeType,
                buildId = buildId,
                taskId = codeccCallback.taskId,
                codeStyleScore = codeStyleScore,
                codeSecurityScore = codeSecurityScore,
                codeMeasureScore = codeMeasureScore
            )
        )
        // 获取合格分数配置
        val qualifiedScoreConfig = businessConfigDao.get(dslContext, storeType.name, "codecc", "qualifiedScore")
        val qualifiedScore = (qualifiedScoreConfig?.configValue ?: "90").toDouble()
        val notifyTemplateCode = storeType.name + if (codeStyleScore >= qualifiedScore && codeSecurityScore >= qualifiedScore && codeMeasureScore >= qualifiedScore) {
            STORE_CODECC_QUALIFIED_TEMPLATE_SUFFIX
        } else {
            STORE_CODECC_FAILED_TEMPLATE_SUFFIX
        }
        // 发送邮件通知插件成员
        val receivers = mutableSetOf(storeValidateCodeccResultRequest.userId)
        val codeccDetailUrl = MessageFormat(codeccDetailUrl).format(
            arrayOf(
                storeValidateCodeccResultRequest.projectCode,
                codeccCallback.taskId
            )
        )
        val storeName = storeCommonService.getStoreNameById(buildRelRecord.storeId, storeType)
        val bodyParams = mapOf(
            "storeName" to storeName,
            "codeStyleScore" to codeStyleScore.toString(),
            "codeSecurityScore" to codeSecurityScore.toString(),
            "codeMeasureScore" to codeMeasureScore.toString(),
            "codeccDetailUrl" to codeccDetailUrl,
            "dataTime" to DateTimeUtil.toDateTime(LocalDateTime.now())
        )
        storeNotifyService.sendNotifyMessage(
            templateCode = notifyTemplateCode,
            sender = DEVOPS,
            receivers = receivers,
            bodyParams = bodyParams
        )
        return Result(true)
    }

    private fun getCodeccCallback(buildId: String): CodeccCallback {
        val codeccTaskResult = client.get(ServiceCodeccResource::class).getCodeccTaskResult(setOf(buildId))
        logger.info("codeccTaskResult is:$codeccTaskResult")
        val codeccTaskMap = codeccTaskResult.data
        val codeccCallback = codeccTaskMap?.get(buildId)
        if (codeccTaskResult.isNotOk() || codeccTaskMap == null || codeccCallback == null) {
            throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
        }
        return codeccCallback
    }

    /**
     * 计算代码规范评分
     * @param storeType 组件类型
     * @param totalSeriousWaringCount 总严重告警数
     * @param totalNormalWaringCount 总一般告警数
     * @param totalLine 代码总行数
     * @param language 开发语言
     */
    private fun calCodeStyleScore(
        storeType: StoreTypeEnum,
        totalSeriousWaringCount: Int,
        totalNormalWaringCount: Int,
        totalLine: Int,
        language: String
    ): Double {
        // 计算百行告警数，百行告警数=（严重告警数*1+一般告警数*0.5）/代码行数*100
        val hundredWaringCount = ((totalSeriousWaringCount * 1 + totalNormalWaringCount * 0.5) / totalLine) * 100
        // 从配置表中读取60分对应百行告警数配置
        val languageWaringConfig = businessConfigDao.get(dslContext, storeType.name, "codeStyleHundredWaring", language)
        val languageWaringConfigCount = languageWaringConfig?.configValue ?: "6"
        // 计算代码规范评分
        val score = 100 * ((0.6.pow(1.toDouble() / languageWaringConfigCount.toDouble())).pow(hundredWaringCount))
        return String.format(scoreFormat, score).toDouble()
    }

    /**
     * 计算代码安全评分
     * @param totalSeriousRiskCount 总严重风险数
     * @param totalNormalRiskCount 总一般风险数
     */
    private fun calCodeSecurityScore(totalSeriousRiskCount: Int, totalNormalRiskCount: Int): Double {
        val score = if (totalSeriousRiskCount > 0) {
            0
        } else if (totalSeriousRiskCount == 0 && totalNormalRiskCount > 0) {
            50
        } else if (totalSeriousRiskCount == 0 && totalNormalRiskCount == 0) {
            100
        } else {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
        }
        return String.format(scoreFormat, score).toDouble()
    }

    /**
     * 计算代码度量和检查评分
     * @param storeType 组件类型
     * @param totalCoveritySeriousWaringCount 总严重告警数
     * @param totalCoverityNormalWaringCount 总一般告警数
     * @param totalLine 代码总行数
     * @param totalCcnExceedNum 代码圈复杂度总超标数
     */
    private fun calCodeMeasureScore(
        storeType: StoreTypeEnum,
        totalCoveritySeriousWaringCount: Int,
        totalCoverityNormalWaringCount: Int,
        totalLine: Int,
        totalCcnExceedNum: Double
    ): Double {
        // 计算圈复杂度千行平均超标数
        val thousandCcnCount = 1000 * totalCcnExceedNum / totalLine
        // 从配置表中读取圈复杂度评分计算规则
        val ccnCodeCalScoreStyleConfig = businessConfigDao.get(dslContext, storeType.name, codeCalScoreStyle, "ccn")
        val ccnCodeCalScoreStyle = ccnCodeCalScoreStyleConfig!!.configValue
        // 计算圈复杂度得分
        val ccnScore = storeCodeccDao.calScore(dslContext, ccnCodeCalScoreStyle, thousandCcnCount)
        // 计算严重告警数得分
        val coveritySeriousWaringScore = if (totalCoveritySeriousWaringCount > 0) 0 else 100
        // 计算一般告警数千行均值
        val thousandCoverityNormalWaringCount = (1000 * totalCoverityNormalWaringCount / totalLine).toDouble()
        // 从配置表中读取coverity一般告警数评分计算规则
        val coverityCalScoreStyleConfig = businessConfigDao.get(dslContext, storeType.name, codeCalScoreStyle, "coverityNormalWaring")
        val coverityCalScoreStyle = coverityCalScoreStyleConfig!!.configValue
        // 计算一般告警数得分评分
        val coverityNormalWaringScore = storeCodeccDao.calScore(dslContext, coverityCalScoreStyle, thousandCoverityNormalWaringCount)
        val score = 0.9 * ccnScore + 0.08 * coveritySeriousWaringScore + 0.02 * coverityNormalWaringScore
        return String.format(scoreFormat, score).toDouble()
    }
}

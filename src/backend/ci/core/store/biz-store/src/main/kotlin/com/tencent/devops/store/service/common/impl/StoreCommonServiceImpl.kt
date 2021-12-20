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

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.configuration.StoreDetailUrlConfig
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.OperationLogDao
import com.tencent.devops.store.dao.common.ReasonRelDao
import com.tencent.devops.store.dao.common.SensitiveConfDao
import com.tencent.devops.store.dao.common.StoreApproveDao
import com.tencent.devops.store.dao.common.StoreCommentDao
import com.tencent.devops.store.dao.common.StoreCommentPraiseDao
import com.tencent.devops.store.dao.common.StoreCommentReplyDao
import com.tencent.devops.store.dao.common.StoreDeptRelDao
import com.tencent.devops.store.dao.common.StoreEnvVarDao
import com.tencent.devops.store.dao.common.StoreMediaInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.dao.common.StoreStatisticDailyDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.common.StoreStatisticTotalDao
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreBuildInfo
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.StoreShowVersionItem
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommonService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store公共
 * since: 2019-07-23
 */
@Suppress("ALL")
@Service
class StoreCommonServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val operationLogDao: OperationLogDao,
    private val sensitiveConfDao: SensitiveConfDao,
    private val reasonRelDao: ReasonRelDao,
    private val storeApproveDao: StoreApproveDao,
    private val storeCommentDao: StoreCommentDao,
    private val storeCommentPraiseDao: StoreCommentPraiseDao,
    private val storeCommentReplyDao: StoreCommentReplyDao,
    private val storeDeptRelDao: StoreDeptRelDao,
    private val storeEnvVarDao: StoreEnvVarDao,
    private val storeMediaInfoDao: StoreMediaInfoDao,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao,
    private val storeStatisticDailyDao: StoreStatisticDailyDao,
    private val storeDetailUrlConfig: StoreDetailUrlConfig
) : StoreCommonService {

    private val logger = LoggerFactory.getLogger(StoreCommonServiceImpl::class.java)

    override fun getStoreNameById(
        storeId: String,
        storeType: StoreTypeEnum
    ): String {
        logger.info("getStoreNameById: $storeId | $storeType")
        val storeCommonDao = getStoreCommonDao(storeType.name)
        return storeCommonDao.getStoreNameById(dslContext, storeId) ?: ""
    }

    override fun getStorePublicFlagByCode(storeCode: String, storeType: StoreTypeEnum): Boolean {
        return getStoreCommonDao(storeType.name).getStorePublicFlagByCode(dslContext, storeCode)
    }

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    /**
     * 获取正确的升级版本号
     */
    override fun getRequireVersion(
        dbVersion: String,
        releaseType: ReleaseTypeEnum,
        reqVersion: String?
    ): List<String> {
        var requireVersionList = listOf(INIT_VERSION)
        if (dbVersion.isBlank()) {
            return requireVersionList
        }
        val dbVersionParts = dbVersion.split(".")
        val firstVersionPart = dbVersionParts[0]
        val secondVersionPart = dbVersionParts[1]
        val thirdVersionPart = dbVersionParts[2]
        when (releaseType) {
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE -> {
                requireVersionList = listOf("${firstVersionPart.toInt() + 1}.0.0")
            }
            ReleaseTypeEnum.COMPATIBILITY_UPGRADE -> {
                requireVersionList = listOf("$firstVersionPart.${secondVersionPart.toInt() + 1}.0")
            }
            ReleaseTypeEnum.COMPATIBILITY_FIX -> {
                requireVersionList = listOf("$firstVersionPart.$secondVersionPart.${thirdVersionPart.toInt() + 1}")
            }
            ReleaseTypeEnum.CANCEL_RE_RELEASE -> {
                requireVersionList = listOf(dbVersion)
            }
            ReleaseTypeEnum.HIS_VERSION_UPGRADE -> {
                if (!reqVersion.isNullOrBlank()) {
                    val reqVersionParts = reqVersion.split(".")
                    requireVersionList = listOf(
                        "${reqVersionParts[0]}.${reqVersionParts[1]}.${reqVersionParts[2].toInt() + 1}",
                        "${reqVersionParts[0]}.${reqVersionParts[1].toInt() + 1}.0"
                    )
                } else {
                    throw ErrorCodeException(errorCode = StoreMessageCode.USER_HIS_VERSION_UPGRADE_INVALID)
                }
            }
            else -> {
            }
        }
        return requireVersionList
    }

    /**
     * 设置进度
     */
    override fun setProcessInfo(
        processInfo: List<ReleaseProcessItem>,
        totalStep: Int,
        currStep: Int,
        status: String
    ): Boolean {
        for (item in processInfo) {
            if (item.step < currStep) {
                item.status = SUCCESS
            } else if (item.step == currStep) {
                if (currStep == totalStep) {
                    item.status = SUCCESS
                } else {
                    item.status = status
                }
            }
        }
        return true
    }

    /**
     * 生成发布流程进度信息
     */
    override fun generateStoreProcessInfo(
        userId: String,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        creator: String,
        processInfo: List<ReleaseProcessItem>
    ): StoreProcessInfo {
        val opPermission = storeMemberDao.isStoreAdmin(
            dslContext,
            userId,
            storeCode,
            storeType.type.toByte()
        ) || creator == userId
        val storeProcessInfo = StoreProcessInfo(opPermission, null, processInfo)
        val storeBuildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, storeId)
        if (null != storeBuildInfoRecord) {
            storeProcessInfo.storeBuildInfo = StoreBuildInfo(
                storeId = storeBuildInfoRecord.storeId,
                pipelineId = storeBuildInfoRecord.pipelineId,
                buildId = storeBuildInfoRecord.buildId,
                projectCode = storeProjectRelDao.getUserStoreTestProjectCode(
                    dslContext,
                    userId,
                    storeCode,
                    storeType
                ) ?: ""
            )
        }
        return storeProcessInfo
    }

    /**
     * 获取store组件详情页地址
     */
    override fun getStoreDetailUrl(storeType: StoreTypeEnum, storeCode: String): String {
        logger.info("getStoreDetailUrl storeType is :$storeType, storeCode is :$storeCode")
        val url = when (storeType) {
            StoreTypeEnum.ATOM -> "${storeDetailUrlConfig.atomDetailBaseUrl}$storeCode"
            StoreTypeEnum.TEMPLATE -> "${storeDetailUrlConfig.templateDetailBaseUrl}$storeCode"
            StoreTypeEnum.IMAGE -> "${storeDetailUrlConfig.imageDetailBaseUrl}$storeCode"
            StoreTypeEnum.IDE_ATOM -> "${storeDetailUrlConfig.ideAtomDetailBaseUrl}$storeCode"
            StoreTypeEnum.SERVICE -> "${storeDetailUrlConfig.serviceDetailBaseUrl}$storeCode"
            else -> ""
        }
        logger.info("getStoreDetailUrl url is :$url")
        return url
    }

    override fun deleteStoreInfo(context: DSLContext, storeCode: String, storeType: Byte): Boolean {
        operationLogDao.deleteOperationLog(context, storeCode, storeType)
        sensitiveConfDao.deleteSensitiveConf(context, storeCode, storeType)
        reasonRelDao.deleteReasonRel(context, storeCode, storeType)
        storeApproveDao.deleteApproveInfo(context, storeCode, storeType)
        storeCommentDao.deleteStoreComment(context, storeCode, storeType)
        storeCommentPraiseDao.deleteStoreCommentPraise(context, storeCode, storeType)
        storeCommentReplyDao.deleteStoreCommentReply(context, storeCode, storeType)
        storeDeptRelDao.deleteByStoreCode(context, storeCode, storeType)
        storeEnvVarDao.deleteEnvVar(context, storeCode, storeType)
        storeMediaInfoDao.deleteByStoreCode(context, storeCode, storeType)
        storeMemberDao.deleteAll(context, storeCode, storeType)
        storePipelineBuildRelDao.deleteStorePipelineBuildRel(context, storeCode, storeType)
        storePipelineRelDao.deleteStorePipelineRel(context, storeCode, storeType)
        storeProjectRelDao.deleteAllRel(context, storeCode, storeType)
        storeReleaseDao.deleteStoreReleaseInfo(context, storeCode, storeType)
        storeStatisticDao.deleteStoreStatistic(context, storeCode, storeType)
        storeStatisticTotalDao.deleteStoreStatisticTotal(context, storeCode, storeType)
        storeStatisticDailyDao.deleteDailyStatisticData(context, storeCode, storeType)
        return true
    }

    override fun getStoreShowVersionInfo(
        cancelFlag: Boolean,
        releaseType: ReleaseTypeEnum?,
        version: String?
    ): StoreShowVersionInfo {
        val defaultShowReleaseType = when {
            cancelFlag -> {
                ReleaseTypeEnum.CANCEL_RE_RELEASE
            }
            releaseType?.isDefaultShow() == true -> {
                releaseType
            }
            releaseType == null -> {
                ReleaseTypeEnum.NEW
            }
            else -> {
                ReleaseTypeEnum.COMPATIBILITY_FIX
            }
        }
        val dbVersion = version ?: ""
        val defaultShowVersion = getRequireVersion(dbVersion, defaultShowReleaseType)[0]
        val showVersionList = mutableListOf<StoreShowVersionItem>()
        showVersionList.add(StoreShowVersionItem(defaultShowVersion, defaultShowReleaseType.name, true))
        if (dbVersion.isBlank()) {
            return StoreShowVersionInfo(showVersionList)
        }
        val tmpReleaseTypeList = listOf(
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE,
            ReleaseTypeEnum.COMPATIBILITY_UPGRADE,
            ReleaseTypeEnum.COMPATIBILITY_FIX
        )
        tmpReleaseTypeList.forEach { tmpReleaseType ->
            if (tmpReleaseType != defaultShowReleaseType) {
                val showVersion = getRequireVersion(dbVersion, tmpReleaseType)[0]
                showVersionList.add(StoreShowVersionItem(showVersion, tmpReleaseType.name))
            }
        }
        return StoreShowVersionInfo(showVersionList)
    }
}

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

import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.ING
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.configuration.StoreDetailUrlConfig
import com.tencent.devops.store.dao.common.AbstractStoreCommonDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.StoreBuildInfo
import com.tencent.devops.store.pojo.common.StoreProcessInfo
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
@Service
class StoreCommonServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val storeProjectRelDao: StoreProjectRelDao,
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

    private fun getStoreCommonDao(storeType: String): AbstractStoreCommonDao {
        return SpringContextUtil.getBean(AbstractStoreCommonDao::class.java, "${storeType}_COMMON_DAO")
    }

    /**
     * 获取正确的升级版本号
     */
    override fun getRequireVersion(
        dbVersion: String,
        releaseType: ReleaseTypeEnum
    ): String {
        var requireVersion = INIT_VERSION
        val dbVersionParts = dbVersion.split(".")
        when (releaseType) {
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE -> {
                requireVersion = "${dbVersionParts[0].toInt() + 1}.0.0"
            }
            ReleaseTypeEnum.COMPATIBILITY_UPGRADE -> {
                requireVersion = "${dbVersionParts[0]}.${dbVersionParts[1].toInt() + 1}.0"
            }
            ReleaseTypeEnum.COMPATIBILITY_FIX -> {
                requireVersion = "${dbVersionParts[0]}.${dbVersionParts[1]}.${dbVersionParts[2].toInt() + 1}"
            }
            ReleaseTypeEnum.CANCEL_RE_RELEASE -> {
                requireVersion = dbVersion
            }
            else -> {
            }
        }
        return requireVersion
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
                    item.name += if (status == DOING) MessageCodeUtil.getCodeLanMessage(ING) else MessageCodeUtil.getCodeLanMessage(
                        FAIL
                    )
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
        modifier: String,
        processInfo: List<ReleaseProcessItem>
    ): StoreProcessInfo {
        val opPermission = storeMemberDao.isStoreAdmin(
            dslContext,
            userId,
            storeCode,
            storeType.type.toByte()
        ) || modifier == userId
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
            else -> ""
        }
        logger.info("getStoreDetailUrl url is :$url")
        return url
    }
}

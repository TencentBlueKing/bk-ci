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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

abstract class StoreComponentManageServiceImpl(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureManageDao: StoreBaseFeatureManageDao,
    private val storeProjectService: StoreProjectService
) : StoreComponentManageService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentManageServiceImpl::class.java)
    }

    override fun updateComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun installComponent(
        userId: String,
        channelCode: ChannelCode,
        installStoreReq: InstallStoreReq
    ): Result<Boolean> {
        logger.info("installComponent params:[$userId|$installStoreReq]")
        val componentBaseInfoRecord = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(installStoreReq.storeCode)
        )
        val publicFlag = storeBaseFeatureManageDao.isPublic(
            dslContext = dslContext,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType
        )
        val storeBaseInfo = componentBaseInfoRecord.let {
            StoreBaseInfo(
                storeId = it.id,
                storeCode = it.storeCode,
                storeName = it.name,
                storeType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt()),
                version = it.name,
                publicFlag = publicFlag,
                status = it.status,
                logoUrl = it.logoUrl,
                publisher = it.publisher,
                classifyId = it.classifyId
            )
        }
        // 校验组件安装权限
        validateInstall(
            userId = userId,
            storeBaseInfo = storeBaseInfo,
            projectCodeList = installStoreReq.projectCodes
        )
        // 安装操作逻辑扩展
        val installComponentExtResult = installComponentExt(
            userId = userId,
            projectCodeList = installStoreReq.projectCodes,
            storeBaseInfo = storeBaseInfo
        )
        if (installComponentExtResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_INSTALL_VALIDATE_FAIL,
                params = arrayOf(installStoreReq.storeCode, installComponentExtResult.message ?: "")
            )
        }
        return storeProjectService.installStoreComponent(
            userId = userId,
            projectCodeList = installStoreReq.projectCodes,
            storeId = storeBaseInfo.storeId,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType,
            publicFlag = publicFlag,
            channelCode = channelCode
        )

    }


    /**
     * 组件安装校验
     */
    abstract fun validateInstall(
        userId: String,
        storeBaseInfo: StoreBaseInfo,
        projectCodeList: ArrayList<String>
    ): Result<Boolean>

    /**
     * 组件安装校逻辑扩展
     */
    abstract fun installComponentExt(
        userId: String,
        projectCodeList: ArrayList<String>,
        storeBaseInfo: StoreBaseInfo
    ): Result<Boolean>

    override fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun deleteComponent(userId: String, storeType: String, storeCode: String): Result<Boolean> {
        TODO("Not yet implemented")
    }
}
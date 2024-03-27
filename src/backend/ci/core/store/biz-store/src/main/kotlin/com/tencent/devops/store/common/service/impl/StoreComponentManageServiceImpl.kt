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
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreLogicExtendService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class StoreComponentManageServiceImpl(
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
        val storeBaseDataPO = componentBaseInfoRecord.let {
            StoreBaseDataPO(
                id = it.id,
                storeCode = it.storeCode,
                storeType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt()),
                name = it.name,
                version = it.version,
                status = StoreStatusEnum.valueOf(it.status),
                logoUrl = it.logoUrl,
                latestFlag = it.latestFlag,
                publisher = it.publisher,
                classifyId = it.classifyId,
                creator = it.creator,
                modifier = it.modifier
            )
        }
        val componentFeatureInfo = storeBaseFeatureManageDao.getComponentFeatureDataByCode(
            dslContext = dslContext,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType
        )
        val publicFlag = componentFeatureInfo?.publicFlag ?: true
        validateInstall(
            userId = userId,
            publicFlag = publicFlag,
            installStoreReq = installStoreReq,
            channelCode = channelCode
        )
        val installComponentExtResult = getStoreLogicExtendService(installStoreReq.storeType).installComponentExt(
            userId = userId,
            projectCodeList = installStoreReq.projectCodes,
            storeBaseDataPO = storeBaseDataPO,
            storeBaseFeatureDataPO = componentFeatureInfo?.let {
                StoreBaseFeatureDataPO(
                    id = componentFeatureInfo.id,
                    storeCode = componentFeatureInfo.storeCode,
                    storeType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt()),
                    type = componentFeatureInfo.type,
                    rdType = componentFeatureInfo.rdType,
                    creator = componentFeatureInfo.creator,
                    modifier = componentFeatureInfo.modifier
                )
            }
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
            storeId = storeBaseDataPO.id,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType,
            publicFlag = publicFlag,
            channelCode = channelCode
        )

    }

    private fun getStoreLogicExtendService(storeType: StoreTypeEnum): StoreLogicExtendService {
        return SpringContextUtil.getBean(StoreLogicExtendService::class.java, getLogicExtendServiceBeanName(storeType))
    }

    private fun getLogicExtendServiceBeanName(storeType: StoreTypeEnum): String {
        return "${storeType}_LOGIC_EXTEND_SERVICE"
    }

    private fun validateInstall(
        userId: String,
        publicFlag: Boolean,
        channelCode: ChannelCode,
        installStoreReq: InstallStoreReq
    ): Result<Boolean> {
        val storeCode = installStoreReq.storeCode
        val storeType = installStoreReq.storeType
        val validateInstallExtResult = getStoreLogicExtendService(installStoreReq.storeType).validateInstallExt(
            userId = userId,
            storeCode = storeCode,
            projectCodeList = installStoreReq.projectCodes
        )
        if (validateInstallExtResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_INSTALL_VALIDATE_FAIL,
                params = arrayOf(storeCode, "${validateInstallExtResult.message}")
            )
        }
        val validateInstallResult = storeProjectService.validateInstallPermission(
            userId = userId,
            projectCodeList = installStoreReq.projectCodes,
            storeCode = storeCode,
            storeType = storeType,
            publicFlag = publicFlag,
            channelCode = channelCode
        )
        if (validateInstallResult.isNotOk()) {
            return validateInstallResult
        }
        return Result(true)
    }

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
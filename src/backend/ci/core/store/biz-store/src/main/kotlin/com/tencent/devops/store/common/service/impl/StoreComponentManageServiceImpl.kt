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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.ReasonRelDao
import com.tencent.devops.store.common.dao.StoreBaseExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreLabelRelDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.handler.StoreDeleteCheckHandler
import com.tencent.devops.store.common.handler.StoreDeleteDataPersistHandler
import com.tencent.devops.store.common.handler.StoreDeleteHandlerChain
import com.tencent.devops.store.common.handler.StoreDeleteRepoFileHandler
import com.tencent.devops.store.common.service.StoreBaseInstallService
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.utils.StoreReleaseUtils
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentManageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeProjectService: StoreProjectService,
    private val classifyDao: ClassifyDao,
    private val storeLabelRelDao: StoreLabelRelDao,
    private val storeBaseExtManageDao: StoreBaseExtManageDao,
    private val storeDeleteCheckHandler: StoreDeleteCheckHandler,
    private val storeDeleteRepoFileHandler: StoreDeleteRepoFileHandler,
    private val storeDeleteDataPersistHandler: StoreDeleteDataPersistHandler,
    private val storeMemberDao: StoreMemberDao,
    private val reasonRelDao: ReasonRelDao
) : StoreComponentManageService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentManageServiceImpl::class.java)
    }

    private fun getStoreManagementExtraService(storeType: StoreTypeEnum): StoreManagementExtraService {
        return SpringContextUtil.getBean(
            StoreManagementExtraService::class.java,
            "${storeType}_MANAGEMENT_EXTRA_SERVICE"
        )
    }

    override fun updateComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest,
        checkPermissionFlag: Boolean
    ): Result<Boolean> {
        logger.info("updateComponentBaseInfo params:[$userId|$storeCode|$storeType|$storeBaseInfoUpdateRequest]")
        // 校验当前用户是否拥有更新组件基本信息权限
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = StoreTypeEnum.valueOf(storeType).type.toByte()
            )
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.GET_INFO_NO_PERMISSION,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(storeCode)
            )
        }
        // 查询组件的最新记录
        val componentBaseInfoRecord = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType)
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        val editFlag = StoreUtils.checkEditCondition(componentBaseInfoRecord.status)
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_VERSION_IS_NOT_FINISH,
                params = arrayOf(componentBaseInfoRecord.name, componentBaseInfoRecord.version)
            )
        }
        val storeIds =  mutableListOf(componentBaseInfoRecord.id)
        val latestComponent =
            storeBaseQueryDao.getLatestComponentByCode(dslContext, storeCode, StoreTypeEnum.valueOf(storeType))
        if (latestComponent != null && componentBaseInfoRecord.id != latestComponent.id) {
            storeIds.add(latestComponent.id)
        }
        val storeBaseExtDataPOs = StoreReleaseUtils.generateStoreBaseExtDataPO(
            extBaseInfo = storeBaseInfoUpdateRequest.extBaseInfo,
            storeId = componentBaseInfoRecord.id,
            storeCode = storeCode,
            storeType = StoreTypeEnum.valueOf(storeType),
            userId = userId
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val classifyId = storeBaseInfoUpdateRequest.classifyCode?.let {
                classifyDao.getClassifyByCode(
                    dslContext = context,
                    classifyCode = it,
                    type = StoreTypeEnum.valueOf(storeType)
                )?.id
            }
            storeBaseManageDao.updateComponentBaseInfo(
                dslContext = context,
                userId = userId,
                storeIds = storeIds,
                classifyId = classifyId,
                storeBaseInfoUpdateRequest = storeBaseInfoUpdateRequest
            )
            storeBaseInfoUpdateRequest.labelIdList?.let {
                storeIds.forEach { storeId ->
                    storeLabelRelDao.deleteByStoreId(context, storeId)
                    storeLabelRelDao.batchAdd(
                        dslContext = context,
                        userId = userId,
                        storeId = storeId,
                        labelIdList = it
                    )
                }
            }
            if (!storeBaseExtDataPOs.isNullOrEmpty()) {
                storeBaseExtManageDao.batchSave(context, storeBaseExtDataPOs)
            }
        }
        return Result(true)
    }

    override fun installComponent(
        userId: String,
        channelCode: ChannelCode,
        installStoreReq: InstallStoreReq
    ): Result<Boolean> {
        logger.info("installComponent params:[$userId|$installStoreReq]")
        val storeBaseInstallService = getStoreBaseInstallService(installStoreReq.storeType)
        val storeBaseInfo = storeBaseInstallService.installComponentParamCheck(
            userId = userId,
            channelCode = channelCode,
            installStoreReq = installStoreReq
        ).data!!
        val installComponentPrepareResult = storeBaseInstallService.installComponentPrepare(
            userId = userId,
            projectCodes = installStoreReq.projectCodes,
            storeBaseInfo = storeBaseInfo,
            channelCode = channelCode
        )
        if (installComponentPrepareResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_INSTALL_VALIDATE_FAIL,
                params = arrayOf(installStoreReq.storeCode, installComponentPrepareResult.message ?: "")
            )
        }
        return storeProjectService.installStoreComponent(
            userId = userId,
            installStoreReq = installStoreReq,
            storeId = storeBaseInfo.storeId,
            publicFlag = storeBaseInfo.publicFlag,
            channelCode = channelCode
        )
    }

    override fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {

        logger.info("uninstallComponent|userId:$userId|projectCode:$projectCode|storeCode:$storeCode")
        getStoreManagementExtraService(StoreTypeEnum.valueOf(storeType)).uninstallComponentParamCheck(
            userId = userId,
            projectCode = projectCode,
            storeType = storeType,
            storeCode = storeCode
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 卸载
            storeProjectService.uninstall(StoreTypeEnum.valueOf(storeType), storeCode, projectCode)
            // 入库卸载原因
            unInstallReq.reasonList.forEach {
                if (it?.reasonId != null) {
                    reasonRelDao.add(
                        dslContext = context,
                        id = UUIDUtil.generate(),
                        userId = userId,
                        storeCode = storeCode,
                        storeType = StoreTypeEnum.valueOf(storeType).type.toByte(),
                        reasonId = it.reasonId,
                        note = it.note,
                        type = ReasonTypeEnum.UNINSTALLATOM.type
                    )
                }
            }
        }
        return Result(true)
    }

    override fun deleteComponent(userId: String, handlerRequest: StoreDeleteRequest): Result<Boolean> {
        logger.info("deleteComponent|userId:$userId|handlerRequest:$handlerRequest")
        val handlerList = mutableListOf(
            storeDeleteCheckHandler,
            storeDeleteRepoFileHandler,
            storeDeleteDataPersistHandler
        )
        val bkStoreContext = handlerRequest.bkStoreContext
        bkStoreContext[AUTH_HEADER_USER_ID] = userId
        StoreDeleteHandlerChain(handlerList).handleRequest(handlerRequest)
        return Result(true)
    }



    private fun getStoreBaseInstallService(storeType: StoreTypeEnum): StoreBaseInstallService {
        val beanName = when(storeType) {
            StoreTypeEnum.TEMPLATE -> {
                "TEMPLATE_BASE_INSTALL_SERVICE"
            }
            else -> "STORE_BASE_INSTALL_SERVICE"
        }
        return SpringContextUtil.getBean(
            StoreBaseInstallService::class.java,
            beanName
        )
    }
}
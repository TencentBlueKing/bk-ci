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

package com.tencent.devops.store.common.service

import com.tencent.devops.artifactory.api.service.StoreArchiveComponentPkgResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreLabelDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.utils.StoreReleaseUtils
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.STORE_COMPONENT_REPO_FILE_DELETE_FAIL
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import org.checkerframework.checker.units.qual.s
import org.eclipse.jgit.lib.ObjectChecker.type
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory

abstract class StoreComponentManageService(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseFeatureManageDao: StoreBaseFeatureManageDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    private val storeProjectService: StoreProjectService,
    private val classifyDao: ClassifyDao,
    private val storeLabelDao: StoreLabelDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeBaseExtManageDao: StoreBaseExtManageDao,
    private val storeCommonService: StoreCommonService,
    private val storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao,
    private val storeBaseEnvManageDao: StoreBaseEnvManageDao,
    private val storeBaseFeatureExtManageDao: StoreBaseFeatureExtManageDao,
    private val storeVersionLogDao: StoreVersionLogDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentManageService::class.java)

    }

    fun updateComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ): Result<Boolean> {
        logger.info("updateComponentBaseInfo params:[$userId|$storeCode|$storeType|$storeBaseInfoUpdateRequest]")
        // 判断当前用户是否拥有更新组件基本信息权限
        if (!validateUserUpdateActionPermission()) {
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
        val editFlag = checkEditCondition(componentBaseInfoRecord.status)
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_VERSION_IS_NOT_FINISH,
                params = arrayOf(componentBaseInfoRecord.name, componentBaseInfoRecord.version)
            )
        }
        val storeIds =  mutableListOf(componentBaseInfoRecord.id)
        val latestComponent = storeBaseQueryDao.getLatestComponentByCode(dslContext, storeCode)
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
                    storeLabelDao.deleteByStoreId(context, storeId)
                    storeLabelDao.batchAdd(
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

    abstract fun updateComponentExtBaseInfo(
        context: DSLContext,
        storeIds: List<String>,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    )

    private fun checkEditCondition(status: String): Boolean {
        val componentFinalStatusList = listOf(
            StoreStatusEnum.AUDIT_REJECT.name,
            StoreStatusEnum.RELEASED.name,
            StoreStatusEnum.GROUNDING_SUSPENSION.name,
            StoreStatusEnum.UNDERCARRIAGED.name,
            StoreStatusEnum.INIT.name
        )
        // 判断最近一个插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息
        return componentFinalStatusList.contains(status)
    }

    fun installComponent(
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
        val publicFlag = storeBaseFeatureQueryDao.isPublic(
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
     * 组件安装校逻辑扩展
     */
    abstract fun installComponentExt(
        userId: String,
        projectCodeList: ArrayList<String>,
        storeBaseInfo: StoreBaseInfo
    ): Result<Boolean>

    abstract fun validateUserUpdateActionPermission(): Boolean

    fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
    }

    fun deleteComponent(userId: String, storeType: String, storeCode: String): Result<Boolean> {

        return handleDeleteComponent(userId, storeType, storeCode)
    }

    fun deleteComponentCheck(handlerRequest: StoreDeleteRequest) {
        val bkStoreContext = handlerRequest.bkStoreContext
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        logger.info("delete component ,params:[$userId, $storeCode, $storeType]")
        val isOwner = storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())
        if (!isOwner) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        val releasedCount = storeBaseQueryDao.countReleaseStoreByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeTepe = storeType
        )
        if (releasedCount > 0) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_COMPONENT_RELEASED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(storeCode)
            )
        }
        getStoreSpecBusService(storeType).doComponentDeleteCheck()
    }

    fun deleteComponentRepoFile(handlerRequest: StoreDeleteRequest) {
        val bkStoreContext = handlerRequest.bkStoreContext
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        val deleteStorePkgResult = getStoreSpecBusService(storeType).deleteComponentRepoFile(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        if (deleteStorePkgResult.isNotOk()) {
            throw ErrorCodeException(errorCode = STORE_COMPONENT_REPO_FILE_DELETE_FAIL)
        }
    }

    fun doStoreDeleteDataPersistent(handlerRequest: StoreDeleteRequest) {
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val storeIds = storeBaseQueryDao.getComponentIds(context, storeCode, storeType)
            storeCommonService.deleteStoreInfo(context, storeCode, storeType.type.toByte())
            storeBaseEnvExtManageDao.batchDeleteStoreEnvExtInfo(context, storeIds)
            storeBaseEnvManageDao.batchDeleteStoreEnvInfo(context, storeIds)
            storeBaseFeatureManageDao.deleteStoreBaseFeature(context, storeCode, storeType.type.toByte())
            storeBaseFeatureExtManageDao.deleteStoreBaseFeatureExtInfo(context, storeCode, storeType)
            storeLabelDao.batchDeleteByStoreId(context, storeIds)
            storeVersionLogDao.deleteByStoreCode(context, storeCode, storeType)
            storeBaseExtManageDao.batchDeleteStoreBaseExtInfo(context, storeIds)
            storeBaseManageDao.deleteByComponentCode(context, storeCode, storeType)
        }
    }

    private fun getStoreSpecBusService(storeType: StoreTypeEnum): StoreSpecBusService {
        return SpringContextUtil.getBean(
            StoreSpecBusService::class.java,
            StoreUtils.getSpecBusServiceBeanName(storeType)
        )
    }
}
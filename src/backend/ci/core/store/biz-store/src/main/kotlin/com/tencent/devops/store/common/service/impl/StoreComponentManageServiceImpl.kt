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
import com.tencent.devops.common.api.constant.KEY_FILE_SHA_CONTENT
import com.tencent.devops.common.api.constant.MESSAGE
import com.tencent.devops.common.api.constant.STATUS
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.ReasonRelDao
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreLabelRelDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.handler.StoreDeleteCheckHandler
import com.tencent.devops.store.common.handler.StoreDeleteCodeRepositoryHandler
import com.tencent.devops.store.common.handler.StoreDeleteDataPersistHandler
import com.tencent.devops.store.common.handler.StoreDeleteHandlerChain
import com.tencent.devops.store.common.handler.StoreDeleteRepoFileHandler
import com.tencent.devops.store.common.lock.StoreCodeLock
import com.tencent.devops.store.common.service.StoreBaseInstallService
import com.tencent.devops.store.common.service.StoreComponentManageService
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.utils.StoreReleaseUtils
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.InstalledPkgFileShaContentRequest
import com.tencent.devops.store.pojo.common.KEY_REPOSITORY_AUTHORIZER
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreComponentManageServiceImpl : StoreComponentManageService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeBaseQueryDao: StoreBaseQueryDao

    @Autowired
    lateinit var storeBaseManageDao: StoreBaseManageDao

    @Autowired
    lateinit var storeProjectService: StoreProjectService

    @Autowired
    lateinit var classifyDao: ClassifyDao

    @Autowired
    lateinit var storeLabelRelDao: StoreLabelRelDao

    @Autowired
    lateinit var storeBaseExtManageDao: StoreBaseExtManageDao

    @Autowired
    lateinit var storeDeleteCheckHandler: StoreDeleteCheckHandler

    @Autowired
    lateinit var storeDeleteRepoFileHandler: StoreDeleteRepoFileHandler

    @Autowired
    lateinit var storeDeleteCodeRepositoryHandler: StoreDeleteCodeRepositoryHandler

    @Autowired
    lateinit var storeDeleteDataPersistHandler: StoreDeleteDataPersistHandler

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var reasonRelDao: ReasonRelDao

    @Autowired
    lateinit var storeBaseFeatureManageDao: StoreBaseFeatureManageDao

    @Autowired
    lateinit var storeBaseFeatureExtManageDao: StoreBaseFeatureExtManageDao

    @Autowired
    lateinit var storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao

    @Autowired
    lateinit var storeBaseEnvQueryDao: StoreBaseEnvQueryDao

    @Autowired
    lateinit var storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var redisOperation: RedisOperation

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
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        // 查询组件的最新记录
        val newestComponentRecord = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeTypeEnum
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        updateComponentCheck(
            userId = userId,
            storeTypeEnum = storeTypeEnum,
            checkPermissionFlag = checkPermissionFlag,
            componentBaseInfoRecord = newestComponentRecord
        )
        updateComponentPersistent(
            userId = userId,
            storeTypeEnum = storeTypeEnum,
            newestComponentRecord = newestComponentRecord,
            storeBaseInfoUpdateRequest = storeBaseInfoUpdateRequest
        )
        return Result(true)
    }

    private fun updateComponentPersistent(
        userId: String,
        storeTypeEnum: StoreTypeEnum,
        newestComponentRecord: TStoreBaseRecord,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest
    ) {
        val storeCode = newestComponentRecord.storeCode
        val storeIds = mutableListOf(newestComponentRecord.id)
        val latestComponent = storeBaseQueryDao.getLatestComponentByCode(dslContext, storeCode, storeTypeEnum)
        if (latestComponent != null && newestComponentRecord.id != latestComponent.id) {
            storeIds.add(latestComponent.id)
        }
        val (storeBaseFeatureDataPO, storeBaseFeatureExtDataPOs) = StoreReleaseUtils.generateStoreBaseFeaturePO(
            baseFeatureInfo = storeBaseInfoUpdateRequest.baseFeatureInfo,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            userId = userId
        )
        val storeBaseExtDataPOs = StoreReleaseUtils.generateStoreBaseExtDataPO(
            extBaseInfo = storeBaseInfoUpdateRequest.extBaseInfo,
            storeId = newestComponentRecord.id,
            storeCode = storeCode,
            storeType = storeTypeEnum,
            userId = userId
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val classifyId = storeBaseInfoUpdateRequest.classifyCode?.let {
                classifyDao.getClassifyByCode(dslContext = context, classifyCode = it, type = storeTypeEnum)?.id
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
            storeBaseFeatureDataPO?.let {
                storeBaseFeatureManageDao.saveStoreBaseFeatureData(context, it)
            }
            if (!storeBaseFeatureExtDataPOs.isNullOrEmpty()) {
                storeBaseFeatureExtManageDao.deleteStoreBaseFeatureExtInfo(
                    dslContext = context,
                    storeCode = storeCode,
                    storeType = storeTypeEnum.type.toByte()
                )
                storeBaseFeatureExtManageDao.batchSave(context, storeBaseFeatureExtDataPOs)
            }
        }
    }

    private fun updateComponentCheck(
        userId: String,
        storeTypeEnum: StoreTypeEnum,
        checkPermissionFlag: Boolean,
        componentBaseInfoRecord: TStoreBaseRecord
    ) {
        // 校验当前用户是否拥有更新组件基本信息权限
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = componentBaseInfoRecord.storeCode,
                storeType = storeTypeEnum.type.toByte()
            )
        ) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION,
                params = arrayOf(componentBaseInfoRecord.storeCode)
            )
        }

        val editFlag = StoreUtils.checkEditCondition(componentBaseInfoRecord.status)
        if (!editFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.STORE_VERSION_IS_NOT_FINISH,
                params = arrayOf(componentBaseInfoRecord.name, componentBaseInfoRecord.version)
            )
        }
    }

    override fun installComponent(
        userId: String,
        channelCode: ChannelCode,
        installStoreReq: InstallStoreReq
    ): Result<Boolean> {
        logger.info("installComponent params:[$userId|$installStoreReq]")
        // 检查安装组件请求合法性
        val storeBaseInstallService = getStoreBaseInstallService(installStoreReq.storeType)
        val storeBaseInfo = storeBaseInstallService.installComponentCheck(
            userId = userId,
            channelCode = channelCode,
            installStoreReq = installStoreReq
        ).data!!
        // 安装前准备
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
        // 卸载请求检查
        getStoreManagementExtraService(StoreTypeEnum.valueOf(storeType)).uninstallComponentCheck(
            userId = userId,
            projectCode = projectCode,
            storeType = storeType,
            storeCode = storeCode
        )
        val storeTypeEnum = StoreTypeEnum.valueOf(storeType)
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 卸载
            storeProjectService.uninstall(
                storeType = storeTypeEnum,
                storeCode = storeCode,
                projectCode = projectCode,
                instanceIdList = unInstallReq.instanceIdList
            )
            // 入库卸载原因
            unInstallReq.reasonList.forEach {
                if (it?.reasonId != null) {
                    reasonRelDao.add(
                        dslContext = context,
                        id = UUIDUtil.generate(),
                        userId = userId,
                        storeCode = storeCode,
                        storeType = storeTypeEnum.type.toByte(),
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
            storeDeleteCodeRepositoryHandler,
            storeDeleteDataPersistHandler
        )
        val bkStoreContext = handlerRequest.bkStoreContext
        bkStoreContext[AUTH_HEADER_USER_ID] = userId
        StoreCodeLock(redisOperation, handlerRequest.storeType, handlerRequest.storeCode).use { lock ->
            if (lock.tryLock()) {
                StoreDeleteHandlerChain(handlerList).handleRequest(handlerRequest)
            } else {
                throw ErrorCodeException(errorCode = CommonMessageCode.LOCK_FAIL)
            }
        }
        return Result(
            status = bkStoreContext[STATUS]?.toString()?.toInt() ?: 0,
            data = true,
            message = bkStoreContext[MESSAGE]?.toString()
        )
    }

    override fun validateComponentDownloadPermission(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        projectCode: String,
        userId: String,
        instanceId: String?
    ): Result<StoreBaseInfo?> {
        // 检查组件的状态是否符合下载条件
        val baseRecord = storeBaseQueryDao.getComponent(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version,
            storeType = storeType
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("$storeCode:$version")
        )
        val inValidStatusList = listOf(
            StoreStatusEnum.INIT.name,
            StoreStatusEnum.COMMITTING.name,
            StoreStatusEnum.BUILDING.name,
            StoreStatusEnum.BUILD_FAIL.name,
            StoreStatusEnum.CHECKING.name,
            StoreStatusEnum.CHECK_FAIL.name
        )
        if (baseRecord.status in inValidStatusList) {
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID)
        }
        val storeBaseInfo = StoreBaseInfo(
            storeId = baseRecord.id,
            storeCode = baseRecord.storeCode,
            storeName = baseRecord.name,
            storeType = StoreTypeEnum.getStoreTypeObj(baseRecord.storeType.toInt()),
            version = baseRecord.version,
            status = baseRecord.status,
            logoUrl = baseRecord.logoUrl,
            publisher = baseRecord.publisher,
            classifyId = baseRecord.classifyId
        )
        val storePublicFlagKey = StoreUtils.getStorePublicFlagKey(storeType.name)
        if (redisOperation.isMember(storePublicFlagKey, storeCode)) {
            // 如果从缓存中查出该组件是公共组件则无需权限校验
            storeBaseInfo.publicFlag = true
            return Result(storeBaseInfo)
        }
        val publicFlag = storeBaseFeatureQueryDao.getBaseFeatureByCode(dslContext, storeCode, storeType)?.publicFlag
        val checkFlag = publicFlag == true || (storeMemberDao.isStoreMember(
            dslContext = dslContext, userId = userId, storeCode = storeCode, storeType = storeType.type.toByte()
        ) || storeProjectService.isInstalledByProject(
            projectCode = projectCode,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            instanceId = instanceId
        ))
        if (!checkFlag) {
            if (projectCode.isNotBlank()) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.STORE_PROJECT_COMPONENT_NO_PERMISSION,
                    params = arrayOf(projectCode, storeCode)
                )
            } else {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION,
                    params = arrayOf(storeCode)
                )
            }
        }
        storeBaseInfo.publicFlag = publicFlag ?: false
        return Result(storeBaseInfo)
    }

    override fun updateComponentInstalledPkgShaContent(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        installedPkgFileShaContentRequest: InstalledPkgFileShaContentRequest
    ): Result<Boolean> {
        val storeId = storeBaseQueryDao.getComponentId(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version,
            storeType = storeType
        ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
        val baseEnvRecord = storeBaseEnvQueryDao.getBaseEnvsByStoreId(
            dslContext = dslContext,
            storeId = storeId,
            osName = installedPkgFileShaContentRequest.osName,
            osArch = installedPkgFileShaContentRequest.osArch
        )?.get(0) ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
        val storeBaseEnvExtDataPO = StoreBaseEnvExtDataPO(
            id = UUIDUtil.generate(),
            envId = baseEnvRecord.id,
            storeId = storeId,
            fieldName = "${KEY_FILE_SHA_CONTENT}_${installedPkgFileShaContentRequest.signFileName}",
            fieldValue = installedPkgFileShaContentRequest.fileShaContent,
            creator = userId,
            modifier = userId
        )
        storeBaseEnvExtManageDao.batchSave(dslContext, listOf(storeBaseEnvExtDataPO))
        return Result(true)
    }

    override fun updateStoreRepositoryAuthorizer(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<Boolean> {
        // 判断用户是否是管理员，只有管理员才能重置授权
        if (!storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )
        ) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(userId)
            )
        }
        val baseFeatureRecord =
            storeBaseFeatureQueryDao.getBaseFeatureByCode(dslContext, storeCode, storeType) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeCode)
            )
        val storeBaseFeatureExtDataPO = StoreBaseFeatureExtDataPO(
            id = UUIDUtil.generate(),
            featureId = baseFeatureRecord.id,
            storeCode = storeCode,
            storeType = storeType,
            fieldName = KEY_REPOSITORY_AUTHORIZER,
            fieldValue = userId,
            creator = userId,
            modifier = userId
        )
        storeBaseFeatureExtManageDao.batchSave(dslContext, listOf(storeBaseFeatureExtDataPO))
        return Result(true)
    }

    private fun getStoreBaseInstallService(storeType: StoreTypeEnum): StoreBaseInstallService {
        val beanName = when (storeType) {
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

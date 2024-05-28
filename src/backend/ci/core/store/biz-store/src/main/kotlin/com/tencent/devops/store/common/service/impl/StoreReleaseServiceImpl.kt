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
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StoreReleaseDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.handler.StoreCreateDataPersistHandler
import com.tencent.devops.store.common.handler.StoreCreateHandlerChain
import com.tencent.devops.store.common.handler.StoreCreateParamCheckHandler
import com.tencent.devops.store.common.handler.StoreUpdateDataPersistHandler
import com.tencent.devops.store.common.handler.StoreUpdateHandlerChain
import com.tencent.devops.store.common.handler.StoreUpdateParamCheckHandler
import com.tencent.devops.store.common.handler.StoreUpdateParamI18nConvertHandler
import com.tencent.devops.store.common.handler.StoreUpdateRunPipelineHandler
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreNotifyService
import com.tencent.devops.store.common.service.StorePipelineService
import com.tencent.devops.store.common.service.StoreReleaseService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.CLOSE
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreCreateResponse
import com.tencent.devops.store.pojo.common.publication.StoreOfflineRequest
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.publication.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreReleaseRequest
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateResponse
import com.tencent.devops.store.pojo.common.publication.UpdateStoreBaseDataPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Suppress("LongParameterList", "TooManyFunctions")
class StoreReleaseServiceImpl @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeMemberDao: StoreMemberDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao,
    private val storeVersionLogDao: StoreVersionLogDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val storeCommonService: StoreCommonService,
    private val storeNotifyService: StoreNotifyService,
    private val storePipelineService: StorePipelineService,
    private val storeCreateParamCheckHandler: StoreCreateParamCheckHandler,
    private val storeCreateDataPersistHandler: StoreCreateDataPersistHandler,
    private val storeUpdateParamI18nConvertHandler: StoreUpdateParamI18nConvertHandler,
    private val storeUpdateParamCheckHandler: StoreUpdateParamCheckHandler,
    private val storeUpdateDataPersistHandler: StoreUpdateDataPersistHandler,
    private val storeUpdateRunPipelineHandler: StoreUpdateRunPipelineHandler,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig
) : StoreReleaseService {

    private val logger = LoggerFactory.getLogger(StoreReleaseServiceImpl::class.java)

    @Value("\${store.storeApproveSwitch:close}")
    private var storeApproveSwitch: String = CLOSE

    override fun createComponent(userId: String, storeCreateRequest: StoreCreateRequest): StoreCreateResponse? {
        logger.info("createComponent userId:$userId|storeCreateRequest:$storeCreateRequest")
        val handlerList = mutableListOf(
            storeCreateParamCheckHandler, // 参数检查处理
            storeCreateDataPersistHandler // 数据持久化处理
        )
        val bkStoreContext = storeCreateRequest.bkStoreContext
        bkStoreContext[AUTH_HEADER_USER_ID] = userId
        StoreCreateHandlerChain(handlerList).handleRequest(storeCreateRequest)
        val storeId = bkStoreContext[KEY_STORE_ID]?.toString()
        return if (!storeId.isNullOrBlank()) {
            StoreCreateResponse(storeId = storeId)
        } else {
            null
        }
    }

    override fun updateComponent(userId: String, storeUpdateRequest: StoreUpdateRequest): StoreUpdateResponse? {
        logger.info("updateComponent userId:$userId|storeUpdateRequest:$storeUpdateRequest")
        val handlerList = mutableListOf(
            storeUpdateParamI18nConvertHandler, // 参数国际化处理
            storeUpdateParamCheckHandler, // 参数检查处理
            storeUpdateDataPersistHandler, // 数据持久化处理
            storeUpdateRunPipelineHandler // 运行内置流水线
        )
        val bkStoreContext = storeUpdateRequest.bkStoreContext
        bkStoreContext[AUTH_HEADER_USER_ID] = userId
        StoreUpdateHandlerChain(handlerList).handleRequest(storeUpdateRequest)
        val storeId = bkStoreContext[KEY_STORE_ID]?.toString()
        return if (!storeId.isNullOrBlank()) {
            StoreUpdateResponse(storeId = storeId)
        } else {
            null
        }
    }

    override fun getProcessInfo(userId: String, storeId: String): StoreProcessInfo {
        val record = storeBaseQueryDao.getComponentById(dslContext, storeId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        val storeCode = record.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(record.storeType.toInt())
        // 判断用户是否有查询权限
        val queryFlag = storeMemberDao.isStoreMember(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        )
        if (!queryFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        val status = StoreStatusEnum.valueOf(record.status)
        // 查看当前版本之前的版本是否有已发布的，如果有已发布的版本则只是普通的升级操作而不需要审核
        val isNormalUpgrade = storeCommonService.getNormalUpgradeFlag(
            storeCode = storeCode,
            storeType = storeType,
            status = status
        )
        val storeReleaseSpecBusService = SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
        val processInfo = storeReleaseSpecBusService.getReleaseProcessItems(
            userId = userId,
            isNormalUpgrade = isNormalUpgrade,
            status = status
        )
        return storeCommonService.generateStoreProcessInfo(
            userId = userId,
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType,
            creator = record.creator,
            processInfo = processInfo
        )
    }

    override fun cancelRelease(userId: String, storeId: String): Boolean {
        val status = StoreStatusEnum.GROUNDING_SUSPENSION
        checkStoreVersionOptRight(userId, storeId, status)
        storeBaseManageDao.updateStoreBaseInfo(
            dslContext = dslContext,
            updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                id = storeId,
                status = status,
                modifier = userId
            )
        )
        val storeBuildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, storeId)
        storeBuildInfoRecord?.let {
            // 组件打包流水线取消构建
            client.get(ServiceBuildResource::class).serviceShutdown(
                pipelineId = storeBuildInfoRecord.pipelineId,
                projectId = storeInnerPipelineConfig.innerPipelineProject,
                buildId = storeBuildInfoRecord.buildId,
                channelCode = ChannelCode.AM
            )
        }
        return true
    }

    override fun passTest(userId: String, storeId: String): Boolean {
        val record = storeBaseQueryDao.getComponentById(dslContext, storeId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        val storeCode = record.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(record.storeType.toInt())
        val recordStatus = StoreStatusEnum.valueOf(record.status)
        val isNormalUpgrade = storeCommonService.getNormalUpgradeFlag(
            storeCode = storeCode,
            storeType = storeType,
            status = recordStatus
        )
        val status = if (storeApproveSwitch == CLOSE || isNormalUpgrade) {
            StoreStatusEnum.RELEASED
        } else {
            StoreStatusEnum.AUDITING
        }
        checkStoreVersionOptRight(
            userId = userId,
            storeId = storeId,
            status = status,
            isNormalUpgrade = isNormalUpgrade
        )
        val storeReleaseRecord = storeVersionLogDao.getStoreVersion(dslContext, storeId)!!
        return handleStoreRelease(
            userId = userId,
            storeReleaseRequest = StoreReleaseRequest(
                storeId = storeId,
                storeCode = storeCode,
                storeType = storeType,
                version = record.version,
                status = status,
                releaseType = ReleaseTypeEnum.getReleaseTypeObj(storeReleaseRecord.releaseType.toInt())!!
            )
        )
    }

    override fun handleStoreRelease(userId: String, storeReleaseRequest: StoreReleaseRequest): Boolean {
        val storeId = storeReleaseRequest.storeId
        val storeCode = storeReleaseRequest.storeCode
        val storeType = storeReleaseRequest.storeType
        val status = storeReleaseRequest.status
        val newestReleaseFlag = getNewestReleaseFlag(storeCode, storeType, storeReleaseRequest)
        val releaseType = storeReleaseRequest.releaseType
        val latestFlag = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE && !newestReleaseFlag) {
            // 历史大版本下的小版本更新不把latestFlag置为true（当前发布的版本不是最新的已发布版本）
            null
        } else {
            true
        }
        if (status == StoreStatusEnum.RELEASED) {
            dslContext.transaction { t ->
                val context = DSL.using(t)
                // 记录发布信息
                val pubTime = LocalDateTime.now()
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = storeCode,
                        storeType = storeType,
                        latestUpgrader = userId,
                        latestUpgradeTime = pubTime
                    )
                )
                if (latestFlag == true) {
                    // 清空旧版本LATEST_FLAG
                    storeBaseManageDao.cleanLatestFlag(context, storeCode, storeType)
                }
                storeBaseManageDao.updateStoreBaseInfo(
                    dslContext = context,
                    updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                        id = storeId,
                        status = status,
                        latestFlag = latestFlag,
                        pubTime = pubTime,
                        modifier = userId
                    )
                )
                // 发送版本发布邮件
                storeNotifyService.sendStoreReleaseAuditNotifyMessage(storeId, AuditTypeEnum.AUDIT_SUCCESS)
            }
        } else {
            storeBaseManageDao.updateStoreBaseInfo(
                dslContext = dslContext,
                updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                    id = storeId,
                    status = status,
                    modifier = userId
                )
            )
        }
        return true
    }

    private fun getNewestReleaseFlag(
        storeCode: String,
        storeType: StoreTypeEnum,
        storeReleaseRequest: StoreReleaseRequest
    ): Boolean {
        // 查找插件最近一个已经发布的版本
        val releaseRecords = storeBaseQueryDao.getReleaseComponentsByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            num = 1
        )
        val newestReleaseRecord = if (releaseRecords.isNullOrEmpty()) {
            null
        } else {
            releaseRecords[0]
        }
        var newestReleaseFlag = false
        if (newestReleaseRecord != null) {
            // 比较当前版本是否比最近一个已经发布的版本新
            val requestVersion = storeReleaseRequest.version
            val newestReleaseVersion = newestReleaseRecord.version
            newestReleaseFlag = StoreUtils.isGreaterVersion(requestVersion, newestReleaseVersion)
        }
        return newestReleaseFlag
    }

    override fun offlineComponent(
        userId: String,
        storeOfflineRequest: StoreOfflineRequest,
        checkPermissionFlag: Boolean
    ): Boolean {
        val storeCode = storeOfflineRequest.storeCode
        val storeType = storeOfflineRequest.storeType
        // 判断用户是否有权限下线
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            )
        ) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        val version = storeOfflineRequest.version
        val reason = storeOfflineRequest.reason
        if (!version.isNullOrEmpty()) {
            // 按版本下架组件
            offlineComponentByVersion(
                storeType = storeType,
                storeCode = storeCode,
                version = version,
                userId = userId,
                reason = reason
            )
        } else {
            // 设置组件状态为下架
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeBaseManageDao.offlineComponent(
                    dslContext = context,
                    storeCode = storeCode,
                    storeType = storeType,
                    userId = userId,
                    msg = reason,
                    latestFlag = false
                )
                val newestUndercarriagedRecord = storeBaseQueryDao.getNewestComponentByCode(
                    dslContext = context,
                    storeCode = storeCode,
                    storeType = storeType,
                    status = StoreStatusEnum.UNDERCARRIAGED
                )
                if (null != newestUndercarriagedRecord) {
                    // 把发布时间最晚的下架版本latestFlag置为true
                    storeBaseManageDao.updateStoreBaseInfo(
                        dslContext = dslContext,
                        updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                            id = newestUndercarriagedRecord.id,
                            latestFlag = true,
                            modifier = userId
                        )
                    )
                }
            }
        }
        return true
    }

    override fun rebuild(userId: String, storeId: String): Boolean {
        val record = storeBaseQueryDao.getComponentById(dslContext, storeId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        val storeType = StoreTypeEnum.getStoreTypeObj(record.storeType.toInt())
        val storeReleaseSpecBusService = SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
        val status = storeReleaseSpecBusService.getStoreRunPipelineStatus(startFlag = false)
        status?.let {
            checkStoreVersionOptRight(userId, storeId, status)
        }
        val storeRunPipelineParam = StoreRunPipelineParam(
            userId = userId,
            storeId = storeId
        )
        storePipelineService.runPipeline(storeRunPipelineParam)
        return true
    }

    private fun offlineComponentByVersion(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        userId: String,
        reason: String?
    ) {
        val baseRecord = storeBaseQueryDao.getComponent(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version,
            storeType = storeType
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("$storeCode:$version")
        )
        if (StoreStatusEnum.RELEASED.name != baseRecord.status) {
            throw ErrorCodeException(errorCode = StoreMessageCode.STORE_COMPONENT_IS_NOT_ALLOW_OFFLINE)
        }
        val storeId = baseRecord.id
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 查找插件最近二个已经发布的版本
            val releaseRecords = storeBaseQueryDao.getReleaseComponentsByCode(context, storeCode, storeType, 2)
            if (releaseRecords.isNullOrEmpty()) {
                return@transaction
            }
            storeBaseManageDao.updateStoreBaseInfo(
                dslContext = dslContext,
                updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                    id = storeId,
                    status = StoreStatusEnum.UNDERCARRIAGED,
                    statusMsg = reason,
                    latestFlag = false,
                    modifier = userId
                )
            )
            if (releaseRecords[0].id == storeId) {
                var tmpStoreId: String? = null
                if (releaseRecords.size == 1) {
                    val newestUndercarriagedRecord = storeBaseQueryDao.getNewestComponentByCode(
                        dslContext = context,
                        storeCode = storeCode,
                        storeType = storeType,
                        status = StoreStatusEnum.UNDERCARRIAGED
                    )
                    if (null != newestUndercarriagedRecord) {
                        tmpStoreId = newestUndercarriagedRecord.id
                    }
                } else {
                    // 把前一个发布的版本的latestFlag置为true
                    val tmpStoreRecord = releaseRecords[1]
                    tmpStoreId = tmpStoreRecord.id
                }
                tmpStoreId?.let {
                    storeBaseManageDao.updateStoreBaseInfo(
                        dslContext = dslContext,
                        updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                            id = tmpStoreId,
                            latestFlag = true,
                            modifier = userId
                        )
                    )
                }
            }
        }
    }

    fun checkStoreVersionOptRight(
        userId: String,
        storeId: String,
        status: StoreStatusEnum,
        isNormalUpgrade: Boolean? = null
    ) {
        val record = storeBaseQueryDao.getComponentById(dslContext, storeId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        val storeCode = record.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(record.storeType.toInt())
        val creator = record.creator
        val recordStatus = StoreStatusEnum.valueOf(record.status)
        // 判断用户是否有权限(当前版本的创建者和管理员可以操作)
        if (!(storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte()
            ) || creator == userId)
        ) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.NO_COMPONENT_ADMIN_AND_CREATETOR_PERMISSION,
                params = arrayOf(record.name)
            )
        }
        checkStoreStatus(
            storeCode = storeCode,
            storeType = storeType,
            isNormalUpgrade = isNormalUpgrade,
            recordStatus = recordStatus,
            status = status
        )
    }

    private fun checkStoreStatus(
        storeCode: String,
        storeType: StoreTypeEnum,
        isNormalUpgrade: Boolean?,
        recordStatus: StoreStatusEnum,
        status: StoreStatusEnum
    ) {
        val repositoryHashId = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            fieldName = KEY_REPOSITORY_HASH_ID
        )?.fieldValue
        val testingValidPreviousStatuses = if (repositoryHashId.isNullOrBlank()) {
            // 传包方式可以进入测试中的状态是提交中
            listOf(StoreStatusEnum.COMMITTING)
        } else {
            listOf(StoreStatusEnum.BUILDING)
        }
        val releasedValidPreviousStatuses = if (isNormalUpgrade == true) {
            // 普通升级可以由测试中的状态直接发布
            listOf(StoreStatusEnum.TESTING)
        } else {
            listOf(StoreStatusEnum.TESTING, StoreStatusEnum.AUDITING)
        }
        val cancelValidPreviousStatuses = StoreStatusEnum.values().toMutableList()
        cancelValidPreviousStatuses.remove(StoreStatusEnum.RELEASED)
        val statusToValidPreviousStatuses = mapOf(
            StoreStatusEnum.COMMITTING to listOf(StoreStatusEnum.INIT),
            StoreStatusEnum.BUILDING to listOf(
                StoreStatusEnum.COMMITTING,
                StoreStatusEnum.BUILD_FAIL,
                StoreStatusEnum.TESTING
            ),
            StoreStatusEnum.BUILD_FAIL to listOf(StoreStatusEnum.BUILDING),
            StoreStatusEnum.TESTING to testingValidPreviousStatuses,
            StoreStatusEnum.CHECKING to listOf(
                StoreStatusEnum.COMMITTING,
                StoreStatusEnum.CHECK_FAIL,
                StoreStatusEnum.TESTING
            ),
            StoreStatusEnum.CHECK_FAIL to listOf(
                StoreStatusEnum.COMMITTING,
                StoreStatusEnum.CHECKING,
                StoreStatusEnum.CHECK_FAIL,
                StoreStatusEnum.TESTING
            ),
            StoreStatusEnum.AUDITING to listOf(StoreStatusEnum.TESTING),
            StoreStatusEnum.AUDIT_REJECT to listOf(StoreStatusEnum.AUDITING),
            StoreStatusEnum.RELEASED to releasedValidPreviousStatuses,
            StoreStatusEnum.GROUNDING_SUSPENSION to cancelValidPreviousStatuses,
            StoreStatusEnum.UNDERCARRIAGING to cancelValidPreviousStatuses,
            StoreStatusEnum.UNDERCARRIAGED to listOf(StoreStatusEnum.UNDERCARRIAGING, StoreStatusEnum.RELEASED)
        )
        // 检查状态扭转合法性
        val validateFlag = recordStatus in (statusToValidPreviousStatuses[status] ?: emptyList())
        if (!validateFlag) {
            throw ErrorCodeException(errorCode = StoreMessageCode.STORE_RELEASE_STEPS_ERROR)
        }
    }
}

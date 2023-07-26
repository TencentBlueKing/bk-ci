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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreApproveRecord
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.dao.common.StoreApproveDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.StoreApproveDetail
import com.tencent.devops.store.pojo.common.StoreApproveInfo
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.ApproveTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.AbstractStoreApproveSpecifyBusInfoService
import com.tencent.devops.store.service.common.StoreApproveService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store审批业务逻辑类
 * since: 2019-08-05
 */
@Suppress("ALL")
@Service
class StoreApproveServiceImpl : StoreApproveService {

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeApproveDao: StoreApproveDao

    @Autowired
    lateinit var storeMemberDao: StoreMemberDao

    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(StoreApproveServiceImpl::class.java)

    /**
     * 审批store组件
     */
    override fun approveStoreInfo(
        userId: String,
        approveId: String,
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean> {
        logger.info("approveStoreInfo params:[$userId|$approveId|$storeApproveRequest]")
        val storeApproveRecord = storeApproveDao.getStoreApproveInfo(dslContext, approveId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(approveId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        val storeCode = storeApproveRecord.storeCode
        val storeType = storeApproveRecord.storeType
        val token = storeApproveRecord.token
        if (!storeApproveRequest.token.isNullOrBlank() && token != storeApproveRequest.token) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PERMISSION_DENIED,
                language = I18nUtil.getLanguage(userId)
            )
        }
        // 判断是否是插件管理员在操作
        val flag = storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType)
        if (!flag) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(storeCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val busInfoService = getStoreApproveSpecifyBusInfoService(storeApproveRecord.type)
        val approveResult = busInfoService.approveStoreSpecifyBusInfo(
            userId = userId,
            storeType = StoreTypeEnum.getStoreTypeObj(storeType.toInt())!!,
            storeCode = storeCode,
            approveId = approveId,
            storeApproveRequest = storeApproveRequest
        )
        logger.info("approveStoreInfo approveResult is :$approveResult")
        if (approveResult.isNotOk()) {
            return approveResult
        }
        return Result(true)
    }

    override fun getStoreApproveInfos(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        applicant: String?,
        approveType: ApproveTypeEnum?,
        approveStatus: ApproveStatusEnum?,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreApproveInfo>?> {
        // 判断查看用户是否是当前插件的成员
        val flag = storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())
        if (!flag) {
            return I18nUtil.generateResponseDataObject(
                messageCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val storeApproveInfoList = storeApproveDao.getStoreApproveInfos(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            applicant = applicant,
            approveType = approveType,
            approveStatus = approveStatus,
            page = page,
            pageSize = pageSize)
            ?.map {
                generateStoreApproveInfo(it)
            }
        val storeApproveInfoCount = storeApproveDao.getStoreApproveInfoCount(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            applicant = applicant,
            approveType = approveType,
            approveStatus = approveStatus
        )
        val totalPages = PageUtil.calTotalPage(pageSize, storeApproveInfoCount)
        return Result(Page(
            count = storeApproveInfoCount,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            records = storeApproveInfoList ?: listOf()
        ))
    }

    override fun getUserStoreApproveInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveType: ApproveTypeEnum
    ): Result<StoreApproveInfo?> {
        logger.info("getUserStoreApproveInfo params:[$userId|$storeType|$storeCode]")
        val storeApproveInfoRecord = storeApproveDao.getUserStoreApproveInfo(
            dslContext = dslContext,
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            approveType = approveType
        )
        return if (null != storeApproveInfoRecord) {
            Result(generateStoreApproveInfo(storeApproveInfoRecord))
        } else {
            Result(data = null)
        }
    }

    override fun getStoreApproveDetail(userId: String, approveId: String): Result<StoreApproveDetail?> {
        logger.info("getUserStoreApproveInfo userId is :$userId, approveId is :$approveId")
        val storeApproveRecord = storeApproveDao.getStoreApproveInfo(dslContext, approveId)
        if (null != storeApproveRecord) {
            // 判断查看用户是否是当前插件的成员
            val flag = storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeApproveRecord.storeCode,
                storeType = storeApproveRecord.storeType
            )
            if (!flag) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = GET_INFO_NO_PERMISSION,
                    language = I18nUtil.getLanguage(userId),
                    params = arrayOf(storeApproveRecord.storeCode)
                )
            }
            val approveType = storeApproveRecord.type
            val busInfoService = getStoreApproveSpecifyBusInfoService(approveType)
            val additionalParams = busInfoService.getBusAdditionalParams(
                userId,
                StoreTypeEnum.getStoreTypeObj(storeApproveRecord.storeType.toInt())!!,
                storeApproveRecord.storeCode,
                approveId
            )
            val storeApproveDetail = StoreApproveDetail(
                approveId = storeApproveRecord.id,
                content = storeApproveRecord.content,
                applicant = storeApproveRecord.applicant,
                approveType = storeApproveRecord.type,
                approveStatus = storeApproveRecord.status,
                approveMsg = storeApproveRecord.approveMsg,
                storeCode = storeApproveRecord.storeCode,
                storeType = StoreTypeEnum.getStoreType(storeApproveRecord.storeType.toInt()),
                additionalParams = additionalParams,
                creator = storeApproveRecord.creator,
                modifier = storeApproveRecord.modifier,
                createTime = storeApproveRecord.createTime.timestampmilli(),
                updateTime = storeApproveRecord.updateTime.timestampmilli()
            )
            logger.info("getUserStoreApproveInfo storeApproveDetail is :$storeApproveDetail")
            return Result(data = storeApproveDetail)
        } else {
            return Result(data = null)
        }
    }

    private fun getStoreApproveSpecifyBusInfoService(approveType: String): AbstractStoreApproveSpecifyBusInfoService {
        return SpringContextUtil.getBean(
            clazz = AbstractStoreApproveSpecifyBusInfoService::class.java,
            beanName = "${approveType}_APPROVE_SERVICE"
        )
    }

    private fun generateStoreApproveInfo(it: TStoreApproveRecord): StoreApproveInfo {
        return StoreApproveInfo(
            approveId = it.id,
            content = it.content,
            applicant = it.applicant,
            approveType = it.type,
            approveStatus = it.status,
            approveMsg = it.approveMsg,
            storeCode = it.storeCode,
            storeType = StoreTypeEnum.getStoreType(it.storeType.toInt()),
            creator = it.creator,
            modifier = it.modifier,
            createTime = it.createTime.timestampmilli(),
            updateTime = it.updateTime.timestampmilli()
        )
    }
}

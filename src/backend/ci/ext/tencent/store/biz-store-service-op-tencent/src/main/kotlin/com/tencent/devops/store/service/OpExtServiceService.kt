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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.common.EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import org.jooq.impl.DSL
import org.jooq.impl.DefaultDSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OpExtServiceService @Autowired constructor(
    private val extServiceDao: ExtServiceDao,
    private val extServiceFeatureDao: ExtServiceFeatureDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val storeMemberService: TxExtServiceMemberImpl,
    private val dslContext: DefaultDSLContext,
    private val serviceNotifyService: ExtServiceNotifyService,
    private val extServiceBcsService: ExtServiceBcsService
) {

    fun queryServiceList(
        serviceName: String?,
        itemId: String?,
        lableId: String?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        isApprove: Boolean?,
        sortType: String?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<ExtServiceInfoResp?> {
        val serviceRecords = extServiceDao.queryServicesFromOp(
            dslContext = dslContext,
            serviceName = serviceName,
            isPublic = isPublic,
            isRecommend = isRecommend,
            itemId = itemId,
            lableId = lableId,
            isApprove = isApprove,
            sortType = sortType ?: ExtServiceSortTypeEnum.UPDATE_TIME.name,
            desc = desc ?: true,
            page = page,
            pageSize = pageSize
        )

        val count = extServiceDao.queryCountFromOp(
            dslContext = dslContext,
            serviceName = serviceName,
            isPublic = isPublic,
            isRecommend = isRecommend,
            itemId = itemId,
            isApprove = isApprove
        )

        val extensionServiceInfoList = mutableSetOf<ExtensionServiceVO>()
        serviceRecords.forEach {
            val serviceId = it["itemId"] as String

            extensionServiceInfoList.add(
                ExtensionServiceVO(
                    serviceId = serviceId,
                    serviceCode = it["serviceCode"] as String,
                    serviceName = it["serviceName"] as String,
                    serviceStatus = (it["serviceStatus"] as Byte).toInt(),
                    publisher = it["publisher"] as String,
                    projectCode = it["projectCode"] as String,
                    modifierTime = (it["updateTime"] as LocalDateTime).timestamp().toString(),
                    version = it["version"] as String
                )
            )
        }

        return Result(ExtServiceInfoResp(count, page, pageSize, extensionServiceInfoList))
    }

    fun listServiceVersionListByCode(
        serviceCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ExtensionServiceVO>?> {
        val serviceRecord = extServiceDao.getServiceByCode(dslContext, serviceCode, page, pageSize)
        val count = extServiceDao.countByCode(dslContext, serviceCode)
        val extensionServiceInfoList = mutableListOf<ExtensionServiceVO>()
        serviceRecord?.forEach {
            logger.info("listServiceByCode serviceCode[$serviceCode] record[$it]")
            extensionServiceInfoList.add(
                ExtensionServiceVO(
                    serviceId = it.id,
                    serviceCode = it.serviceCode,
                    serviceName = it.serviceName,
                    serviceStatus = it.serviceStatus.toInt(),
                    publisher = it.publisher,
                    modifierTime = it.updateTime.toString(),
                    version = it.version
                )
            )
        }
        val totalPages = PageUtil.calTotalPage(pageSize, count.toLong())
        return Result(Page(count = count.toLong(), page = page ?: 1, pageSize = pageSize ?: -1, totalPages = totalPages, records = extensionServiceInfoList))
    }

//    fun editExtInfo(userId: String, serviceId: String, serviceCode: String, infoResp: EditInfoDTO): Result<Boolean> {
//        val baseInfo = infoResp.baseInfo
//        val settingInfo = infoResp.settingInfo
//        if (baseInfo != null) {
//            extServiceDao.updateExtServiceBaseInfo(
//                dslContext = dslContext,
//                userId = userId,
//                serviceId = serviceId,
//                extServiceUpdateInfo = ExtServiceUpdateInfo(
//                    serviceName = baseInfo.serviceName,
//                    logoUrl = baseInfo.logoUrl,
//                    summary = baseInfo.summary,
//                    description = baseInfo.description,
//                    modifierUser = userId,
//                    status = null,
//                    latestFlag = null
//                )
//            )
//
//            // 更新标签信息
//            val labelIdList = baseInfo.labels
//            if (null != labelIdList) {
//                extServiceLabelDao.deleteByServiceId(dslContext, serviceId)
//                if (labelIdList.isNotEmpty())
//                    extServiceLabelDao.batchAdd(dslContext, userId, serviceId, labelIdList)
//            }
//            val itemIds = baseInfo.itemIds
//            if (itemIds != null) {
//                val existenceItems = extServiceItemDao.getItemByServiceId(dslContext, serviceId)
//                val existenceItemIds = mutableListOf<String>()
//                existenceItems?.forEach {
//                    existenceItemIds.add(it.itemId)
//                }
//                itemIds.forEach { itemId ->
//                    if (!existenceItemIds.contains(itemId)) {
//                        extServiceItemDao.create(
//                            userId = userId,
//                            dslContext = dslContext,
//                            extServiceItemRelCreateInfo = ExtServiceItemRelCreateInfo(
//                                serviceId = serviceId,
//                                modifierUser = userId,
//                                creatorUser = userId,
//                                itemId = itemId,
//                                bkServiceId = extServiceBaseService.getItemBkServiceId(itemId)
//                            )
//                        )
//                    }
//                }
//                // 添加扩展点使用记录
//                client.get(ServiceItemResource::class).addServiceNum(itemIds)
//            }
//        }
//
//        storeMediaService.deleteByStoreCode(userId, serviceCode, StoreTypeEnum.SERVICE)
//        infoResp.mediaInfo?.forEach {
//            storeMediaInfoDao.add(
//                dslContext = dslContext,
//                userId = userId,
//                type = StoreTypeEnum.SERVICE.type.toByte(),
//                id = UUIDUtil.generate(),
//                storeMediaInfoReq = StoreMediaInfoRequest(
//                    storeCode = serviceCode,
//                    mediaUrl = it.mediaUrl,
//                    mediaType = it.mediaType.name,
//                    modifier = userId
//                )
//            )
//        }
//
//        if (settingInfo != null) {
//            extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
//                dslContext = dslContext,
//                userId = userId,
//                serviceCode = serviceCode,
//                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
//                    publicFlag = settingInfo.publicFlag,
//                    recommentFlag = settingInfo.recommendFlag,
//                    certificationFlag = settingInfo.certificationFlag,
//                    modifierUser = userId,
//                    serviceTypeEnum = settingInfo.type
//                )
//            )
//        }
//
//        return Result(true)
//    }

    fun approveService(userId: String, serviceId: String, approveReq: ServiceApproveReq): Result<Boolean> {
        // 判断扩展服务是否存在
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId)
            )

        val oldStatus = serviceRecord.serviceStatus
        if (oldStatus != ExtServiceStatusEnum.AUDITING.status.toByte()) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId)
            )
        }

        if (approveReq.result != PASS && approveReq.result != REJECT) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(approveReq.result)
            )
        }
        val serviceCode = serviceRecord.serviceCode
        val auditFlag = approveReq.result == PASS
        if (auditFlag) {
            // 正式发布最新的扩展服务版本
            val deployExtServiceResult = extServiceBcsService.deployExtService(
                userId = userId,
                grayFlag = false,
                serviceCode = serviceCode,
                version = serviceRecord.version,
                checkPermissionFlag = false
            )
            logger.info("deployExtServiceResult is:$deployExtServiceResult")
            if (deployExtServiceResult.isNotOk()) {
                return deployExtServiceResult
            }
        }
        val serviceStatus =
            if (auditFlag) {
                ExtServiceStatusEnum.RELEASE_DEPLOYING.status.toByte()
            } else {
                ExtServiceStatusEnum.AUDIT_REJECT.status.toByte()
            }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val pubTime = LocalDateTime.now()
            extServiceDao.approveServiceFromOp(
                dslContext = context,
                userId = userId,
                serviceId = serviceId,
                serviceStatus = serviceStatus,
                approveReq = approveReq,
                pubTime = pubTime
            )
            extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                dslContext = dslContext,
                serviceCode = serviceCode,
                userId = userId,
                extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                    weight = approveReq.weight,
                    recommentFlag = approveReq.recommendFlag,
                    publicFlag = approveReq.publicFlag,
                    certificationFlag = approveReq.certificationFlag,
                    serviceTypeEnum = approveReq.serviceType
                )
            )
        }
        // 审核失败通知发布者
        if (!auditFlag) {
            serviceNotifyService.sendServiceReleaseNotifyMessage(serviceId, false, EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE)
        }
        return Result(true)
    }

    fun deleteService(
        userId: String,
        serviceId: String,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean> {
        logger.info("deleteService userId: $userId , serviceId: $serviceId , checkPermissionFlag: $checkPermissionFlag")
        val serviceRecord =
            extServiceDao.getServiceById(dslContext, serviceId) ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId)
            )
        val serviceCode = serviceRecord.serviceCode
        val type = StoreTypeEnum.SERVICE.type.toByte()
        if (checkPermissionFlag && !storeMemberService.isStoreAdmin(
                userId,
                serviceCode,
                StoreTypeEnum.SERVICE.type.toByte()
            )
        ) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val releasedCount = extServiceDao.countReleaseServiceByCode(dslContext, serviceCode)
        logger.info("releasedCount: $releasedCount")
        if (releasedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SERVICE_RELEASED_IS_NOT_ALLOW_DELETE,
                arrayOf(serviceCode)
            )
        }
        // 如果已经被安装到其他项目下使用，不能删除
        val installedCount = storeProjectRelDao.countInstalledProject(dslContext, serviceCode, type)
        logger.info("installedCount: $releasedCount")
        if (installedCount > 0) {
            return MessageCodeUtil.generateResponseDataObject(
                StoreMessageCode.USER_SERVICE_USED_IS_NOT_ALLOW_DELETE,
                arrayOf(serviceCode)
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            extServiceDao.deleteExtService(context, userId, serviceId)
            extServiceFeatureDao.deleteExtFeatureService(context, userId, serviceCode)
        }

        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpExtServiceService::class.java)
    }
}

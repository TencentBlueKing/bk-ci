/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.OpStoreComponentService
import com.tencent.devops.store.common.service.StoreNotifyService
import com.tencent.devops.store.common.service.StoreReleaseService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreApproveReleaseRequest
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import com.tencent.devops.store.pojo.common.publication.StoreReleaseRequest
import com.tencent.devops.store.pojo.common.publication.UpdateStoreBaseDataPO
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@Suppress("LongParameterList")
class OpStoreComponentServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseFeatureManageDao: StoreBaseFeatureManageDao,
    private val storeVersionLogDao: StoreVersionLogDao,
    private val storeNotifyService: StoreNotifyService,
    private val storeReleaseService: StoreReleaseService,
    private val redisOperation: RedisOperation
) : OpStoreComponentService {

    override fun approveComponentRelease(
        userId: String,
        storeId: String,
        storeApproveReleaseRequest: StoreApproveReleaseRequest
    ): Boolean {
        val record = storeBaseQueryDao.getComponentById(dslContext, storeId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        val storeCode = record.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(record.storeType.toInt())
        if (record.status != StoreStatusEnum.AUDITING.name) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(storeId))
        }
        val approveResult = storeApproveReleaseRequest.result
        if (approveResult != PASS && approveResult != REJECT) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(approveResult)
            )
        }
        val passFlag = approveResult == PASS
        val storeStatus = if (passFlag) {
            StoreStatusEnum.RELEASED
        } else {
            StoreStatusEnum.AUDIT_REJECT
        }
        val releaseRecord = storeVersionLogDao.getStoreVersion(dslContext, storeId) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeId)
        )
        val releaseType = ReleaseTypeEnum.getReleaseTypeObj(releaseRecord.releaseType.toInt())!!
        val version = record.version
        val firstVersion = storeBaseQueryDao.getFirstComponent(
            dslContext = dslContext, storeCode = storeCode, storeType = storeType
        )?.version
        val latestFlag = when {
            releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE || version == firstVersion -> {
                // 历史大版本下的小版本更新或者首个版本上架审核时，不更新latestFlag
                null
            }
            else -> passFlag
        }
        if (passFlag) {
            // 审核通过则发布插件
            storeReleaseService.handleStoreRelease(
                userId = userId,
                storeReleaseRequest = StoreReleaseRequest(
                    storeId = storeId,
                    storeCode = storeCode,
                    storeType = storeType,
                    version = version,
                    status = storeStatus,
                    releaseType = releaseType,
                    publisher = record.modifier
                )
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeBaseManageDao.updateStoreBaseInfo(
                dslContext = context,
                updateStoreBaseDataPO = UpdateStoreBaseDataPO(
                    id = storeId,
                    status = storeStatus,
                    statusMsg = storeApproveReleaseRequest.message,
                    latestFlag = latestFlag,
                    pubTime = LocalDateTime.now(),
                    modifier = userId
                )
            )
            storeBaseFeatureManageDao.saveStoreBaseFeatureData(
                dslContext = dslContext,
                storeBaseFeatureDataPO = StoreBaseFeatureDataPO(
                    storeCode = storeCode,
                    storeType = storeType,
                    publicFlag = storeApproveReleaseRequest.publicFlag,
                    recommendFlag = storeApproveReleaseRequest.recommendFlag,
                    certificationFlag = storeApproveReleaseRequest.certificationFlag,
                    rdType = storeApproveReleaseRequest.rdType?.name,
                    weight = storeApproveReleaseRequest.weight,
                    creator = record.creator,
                    modifier = userId
                )
            )
        }
        // 更新公共组件缓存
        if (storeApproveReleaseRequest.publicFlag) {
            redisOperation.addSetValue(StoreUtils.getStorePublicFlagKey(storeType.name), storeCode)
        } else {
            redisOperation.removeSetMember(StoreUtils.getStorePublicFlagKey(storeType.name), storeCode)
        }
        if (!passFlag) {
            // 审核不通过则发消息告知用户
            storeNotifyService.sendStoreReleaseAuditNotifyMessage(storeId, AuditTypeEnum.AUDIT_REJECT)
        }
        return true
    }
}

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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("SERVICE_HANDLE_BUILD_RESULT")
class ExtServiceHandleBuildResultService @Autowired constructor(
    private val dslContext: DSLContext,
    private val extServiceDao: ExtServiceDao,
    private val extServiceFeatureDao: ExtServiceFeatureDao,
    private val extServiceBcsService: ExtServiceBcsService
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(ExtServiceHandleBuildResultService::class.java)

    override fun handleStoreBuildResult(storeBuildResultRequest: StoreBuildResultRequest): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val serviceId = storeBuildResultRequest.storeId
        val serviceRecord = extServiceDao.getServiceById(dslContext, serviceId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(serviceId)
            )
        // 防止重复的mq消息造成的状态异常
        if (serviceRecord.serviceStatus != ExtServiceStatusEnum.BUILDING.status.toByte()) {
            // 如果在构建中就取消发布，异步启动的构建流水线可能会成功部署应用至灰度环境，需停掉灰度空间的应用
            if (serviceRecord.serviceStatus == ExtServiceStatusEnum.GROUNDING_SUSPENSION.status.toByte()) {
                // 停止bcs灰度命名空间的应用
                val serviceCode = serviceRecord.serviceCode
                val bcsStopAppResult = extServiceBcsService.stopExtService(
                    userId = serviceRecord.modifier,
                    serviceCode = serviceCode,
                    deploymentName = serviceCode,
                    serviceName = "$serviceCode-service",
                    checkPermissionFlag = false,
                    grayFlag = true
                )
                logger.info("service[$serviceCode] bcsStopAppResult is :$bcsStopAppResult")
                if (bcsStopAppResult.isNotOk()) {
                    return bcsStopAppResult
                }
                extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                    dslContext = dslContext,
                    serviceCode = serviceRecord.serviceCode,
                    userId = serviceRecord.modifier,
                    extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                        killGrayAppFlag = null,
                        killGrayAppMarkTime = null
                    )
                )
            }
            return Result(true)
        }
        var serviceStatus = ExtServiceStatusEnum.TESTING // 构建成功将扩展服务状态置为测试状态
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            serviceStatus = ExtServiceStatusEnum.BUILD_FAIL // 构建失败
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            extServiceDao.setServiceStatusById(
                dslContext = context,
                serviceId = serviceId,
                serviceStatus = serviceStatus.status.toByte(),
                userId = storeBuildResultRequest.userId,
                msg = null
            )
            if (serviceStatus == ExtServiceStatusEnum.TESTING) {
                // 如果扩展服务处于测试中，需要对灰度环境部署的扩展应用不标记停止部署
                extServiceFeatureDao.updateExtServiceFeatureBaseInfo(
                    dslContext = context,
                    serviceCode = serviceRecord.serviceCode,
                    userId = serviceRecord.modifier,
                    extServiceFeatureUpdateInfo = ExtServiceFeatureUpdateInfo(
                        killGrayAppFlag = false,
                        killGrayAppMarkTime = null
                    )
                )
            }
        }
        return Result(true)
    }
}

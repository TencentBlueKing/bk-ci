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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.constants.KEY_EXT_SERVICE_INIT_TEST_PROJECT_PREFIX
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("SERVICE_HANDLE_BUILD_RESULT")
class ExtServiceHandleBuildResultService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val extServiceDao: ExtServiceDao,
    private val storeProjectRelDao: StoreProjectRelDao
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(ExtServiceHandleBuildResultService::class.java)

    override fun handleStoreBuildResult(storeBuildResultRequest: StoreBuildResultRequest): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val serviceId = storeBuildResultRequest.storeId
        val serviceRecord =
            extServiceDao.getServiceById(dslContext, serviceId) ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(serviceId)
            )
        var serviceStatus = ExtServiceStatusEnum.TESTING // 构建成功将扩展服务状态置位测试状态
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            serviceStatus = ExtServiceStatusEnum.BUILD_FAIL // 构建失败
        }
        if (serviceStatus == ExtServiceStatusEnum.TESTING) {
            val serviceCode = serviceRecord.serviceCode
            // 查出扩展服务对应的初始化项目放入redis中
            val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                dslContext,
                serviceCode,
                StoreTypeEnum.SERVICE.type.toByte()
            )
            if (initProjectCode != null)
                redisOperation.addSetValue(KEY_EXT_SERVICE_INIT_TEST_PROJECT_PREFIX + serviceCode, initProjectCode)
        }
        extServiceDao.setServiceStatusById(
            dslContext = dslContext,
            serviceId = serviceId,
            serviceStatus = serviceStatus.status.toByte(),
            userId = storeBuildResultRequest.userId,
            msg = null
        )
        return Result(true)
    }
}

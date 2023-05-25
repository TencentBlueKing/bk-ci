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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import com.tencent.devops.store.service.common.StoreBuildService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class StoreBuildServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storePipelineRelDao: StorePipelineRelDao,
    private val storePipelineBuildRelDao: StorePipelineBuildRelDao
) : StoreBuildService {

    private val logger = LoggerFactory.getLogger(StoreBuildServiceImpl::class.java)

    override fun handleStoreBuildResult(
        pipelineId: String,
        buildId: String,
        storeBuildResultRequest: StoreBuildResultRequest
    ): Result<Boolean> {
        logger.info("handleStoreBuildResult params:[$pipelineId|$buildId|$storeBuildResultRequest]")
        // 查看该次构建流水线属于研发商店哪个组件类型
        val storePipelineRelRecord = storePipelineRelDao.getStorePipelineRelByPipelineId(dslContext, pipelineId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(pipelineId),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        val storeType = storePipelineRelRecord.storeType
        val storeHandleBuildResultService =
            getStoreHandleBuildResultService(StoreTypeEnum.getStoreType(storeType.toInt()))
        val result = storeHandleBuildResultService.handleStoreBuildResult(storeBuildResultRequest)
        logger.info("handleStoreBuildResult result is:$result")
        if (result.isNotOk() || result.data != true) {
            return result
        }
        return Result(true)
    }

    override fun handleStoreBuildStatus(
        userId: String,
        buildId: String,
        pipelineId: String,
        status: BuildStatus
    ): Result<Boolean> {
        val buildInfo = storePipelineBuildRelDao.getStorePipelineBuildRelByBuildId(dslContext, buildId)
        if (buildInfo == null) {
            logger.warn("[$pipelineId] build ($buildId) is not exist")
            return Result(true)
        }
        logger.info("[$pipelineId]| store- the build[$buildId] event ($status)")
        // 渠道为研发商店才回调研发商店处理构建结果的接口
        val result = handleStoreBuildResult(
            pipelineId = pipelineId,
            buildId = buildId,
            storeBuildResultRequest = StoreBuildResultRequest(
                userId = userId,
                buildStatus = status,
                storeId = buildInfo.storeId
            )
        )
        logger.info("handleStoreBuildResult result is:$result")
        return Result(true)
    }

    private fun getStoreHandleBuildResultService(storeType: String): AbstractStoreHandleBuildResultService {
        return SpringContextUtil.getBean(
            clazz = AbstractStoreHandleBuildResultService::class.java,
            beanName = "${storeType}_HANDLE_BUILD_RESULT"
        )
    }
}

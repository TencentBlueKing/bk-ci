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
package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service("IMAGE_HANDLE_BUILD_RESULT")
class ImageHandleBuildResultService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val marketImageService: MarketImageService
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(ImageHandleBuildResultService::class.java)

    override fun handleStoreBuildResult(storeBuildResultRequest: StoreBuildResultRequest): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val imageId = storeBuildResultRequest.storeId
        val imageRecord = imageDao.getImage(dslContext, imageId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(imageId),
                language = I18nUtil.getLanguage(storeBuildResultRequest.userId)
            )
        // 防止重复的mq消息造成的状态异常
        if (imageRecord.imageStatus != ImageStatusEnum.CHECKING.status.toByte()) {
            return Result(true)
        }
        var imageStatus = ImageStatusEnum.TESTING // 验证成功将镜像状态置位测试状态
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            imageStatus = ImageStatusEnum.CHECK_FAIL // 验证失败
        }
        marketImageService.setImageBuildStatusByImageId(
            imageId = imageId,
            userId = storeBuildResultRequest.userId,
            imageStatus = imageStatus,
            msg = null
        )
        return Result(true)
    }
}

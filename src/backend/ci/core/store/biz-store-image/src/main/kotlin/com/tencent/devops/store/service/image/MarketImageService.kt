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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MarketImageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val marketImageDao: MarketImageDao
) {
    private val logger = LoggerFactory.getLogger(MarketImageService::class.java)

    /**
     * 根据镜像ID和镜像代码判断镜像是否存在
     */
    fun judgeImageExistByIdAndCode(imageId: String, imageCode: String): Result<Boolean> {
        logger.info("judgeImageExistByIdAndCode params:[$imageId|$imageCode]")
        val count = marketImageDao.countByIdAndCode(
            dslContext = dslContext,
            imageId = imageId,
            imageCode = imageCode
        )
        if (count < 1) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("imageId:$imageId,imageCode:$imageCode"),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        return Result(true)
    }

    fun setImageBuildStatusByImageId(
        imageId: String,
        userId: String,
        imageStatus: ImageStatusEnum,
        msg: String?
    ): Result<Boolean> {
        logger.info("setImageBuildStatusByImageId params :[$userId|$imageId|$imageStatus|$msg]")
        val imageRecord = imageDao.getImage(dslContext, imageId)
        if (null == imageRecord) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(imageId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            if (imageStatus == ImageStatusEnum.TESTING) {
                // 只有处于验证中的镜像才允许改构建结束后的状态
                // 若验证成功，只有处于验证中的镜像才更改状态
                if (ImageStatusEnum.CHECKING.status.toByte() == imageRecord.imageStatus) {
                    marketImageDao.updateImageStatusById(
                        dslContext = dslContext,
                        imageId = imageRecord.id,
                        imageStatus = imageStatus.status.toByte(),
                        userId = userId,
                        msg = msg
                    )
                }
            } else {
                // 验证失败始终置于失败状态
                marketImageDao.updateImageStatusById(
                    dslContext = dslContext,
                    imageId = imageRecord.id,
                    imageStatus = imageStatus.status.toByte(),
                    userId = userId,
                    msg = msg
                )
            }
        }
        return Result(true)
    }

    fun updateImageBaseInfo(
        userId: String,
        projectCode: String,
        imageCode: String,
        version: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ): Result<Boolean> {
        val imageRecord = imageDao.getImage(dslContext, imageCode, version)
        return if (null != imageRecord) {
            marketImageDao.updateImageBaseInfo(
                dslContext = dslContext,
                userId = userId,
                imageIdList = listOf(imageRecord.id),
                imageBaseInfoUpdateRequest = imageBaseInfoUpdateRequest
            )
            Result(true)
        } else {
            I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$imageCode+$version"),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
    }
}

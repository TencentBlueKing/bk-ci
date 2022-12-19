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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageCommonService @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val marketImageDao: MarketImageDao
) {
    private val logger = LoggerFactory.getLogger(ImageCommonService::class.java)

    fun generateImageStatusList(
        imageCode: String,
        projectCode: String
    ): MutableList<Byte> {
        val flag = storeProjectRelDao.isTestProjectCode(dslContext, imageCode, StoreTypeEnum.IMAGE, projectCode)
        logger.info("generateImageStatusList imageCode=$imageCode|projectCode=$projectCode|flag=$flag")
        // 普通项目的查已发布、下架中、已下架的镜像
        var imageStatusList =
            mutableListOf(
                ImageStatusEnum.RELEASED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGED.status.toByte()
            )
        if (flag) {
            // 原生初始化项目有和申请镜像协作者指定的调试项目权查处于测试中、审核中、已发布、下架中、已下架的镜像
            imageStatusList = mutableListOf(
                ImageStatusEnum.TESTING.status.toByte(),
                ImageStatusEnum.AUDITING.status.toByte(),
                ImageStatusEnum.RELEASED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGED.status.toByte()
            )
        }
        return imageStatusList
    }

    fun checkEditCondition(imageCode: String): Boolean {
        // 查询镜像的最新记录
        val newestImageRecord = marketImageDao.getNewestImageByCode(dslContext, imageCode)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(imageCode))
        val imageFinalStatusList = listOf(
            ImageStatusEnum.AUDIT_REJECT.status.toByte(),
            ImageStatusEnum.RELEASED.status.toByte(),
            ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            ImageStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        // 判断最近一个镜像版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息
        return imageFinalStatusList.contains(newestImageRecord.imageStatus)
    }
}

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

import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.service.common.AbstractClassifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("IMAGE_CLASSIFY_SERVICE")
class MarketImageClassifyService : AbstractClassifyService() {

    private val logger = LoggerFactory.getLogger(MarketImageClassifyService::class.java)

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var imageDao: ImageDao

    override fun getDeleteClassifyFlag(classifyId: String): Boolean {
        // 允许删除分类是条件：1、该分类下的镜像都不处于上架状态 2、该分类下的镜像如果处于已下架状态但已经没人在用
        var flag = false
        val releaseImageNum = imageDao.countReleaseImageNumByClassifyId(dslContext, classifyId)
        logger.info("$classifyId releaseImageNum is :$releaseImageNum")
        if (releaseImageNum == 0) {
            val undercarriageImageNum = imageDao.countUndercarriageImageNumByClassifyId(dslContext, classifyId)
            logger.info("$classifyId undercarriageImageNum is :$undercarriageImageNum")
            if (undercarriageImageNum == 0) {
                flag = true
            }
        }
        return flag
    }
}

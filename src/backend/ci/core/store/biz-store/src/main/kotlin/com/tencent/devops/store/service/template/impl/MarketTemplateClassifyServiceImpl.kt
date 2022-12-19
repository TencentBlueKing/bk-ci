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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.store.dao.template.StoreTemplateDao
import com.tencent.devops.store.service.common.AbstractClassifyService
import com.tencent.devops.store.service.template.MarketTemplateClassifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("TEMPLATE_CLASSIFY_SERVICE")
class MarketTemplateClassifyServiceImpl : MarketTemplateClassifyService, AbstractClassifyService() {

    private val logger = LoggerFactory.getLogger(MarketTemplateClassifyServiceImpl::class.java)

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var templateDao: StoreTemplateDao

    override fun getDeleteClassifyFlag(classifyId: String): Boolean {
        // 允许删除分类是条件：1、该分类下的模板都不处于上架状态 2、该分类下的模板如果处于已下架状态但已经没人在用
        var flag = false
        val releaseTemplateNum = templateDao.countReleaseTemplateNumByClassifyId(dslContext, classifyId)
        logger.info("$classifyId releaseTemplateNum is :$releaseTemplateNum")
        if (releaseTemplateNum == 0) {
            val undercarriageTemplateNum = templateDao.countUndercarriageTemplateNumByClassifyId(dslContext, classifyId)
            logger.info("$classifyId undercarriageTemplateNum is :$undercarriageTemplateNum")
            if (undercarriageTemplateNum == 0) {
                flag = true
            }
        }
        return flag
    }
}

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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.store.dao.template.TemplateCategoryRelDao
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.template.TemplateCategoryService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TemplateCategoryServiceImpl @Autowired constructor() : TemplateCategoryService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var templateCategoryRelDao: TemplateCategoryRelDao

    private val logger = LoggerFactory.getLogger(TemplateCategoryServiceImpl::class.java)

    /**
     * 查找模板范畴
     */
    override fun getCategorysByTemplateId(templateId: String): Result<List<Category>?> {
        logger.info("the templateId is :$templateId")
        val templateCategoryList = mutableListOf<Category>()
        val templateCategoryRecords =
            templateCategoryRelDao.getCategorysByTemplateId(dslContext, templateId) // 查询模板范畴信息
        templateCategoryRecords?.forEach {
            templateCategoryList.add(
                Category(
                    id = it["id"] as String,
                    categoryCode = it["categoryCode"] as String,
                    categoryName = it["categoryName"] as String,
                    iconUrl = it["iconUrl"] as? String,
                    categoryType = StoreTypeEnum.getStoreType((it["categoryType"] as Byte).toInt()),
                    createTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    updateTime = (it["updateTime"] as LocalDateTime).timestampmilli()
                )
            )
        }
        return Result(templateCategoryList)
    }
}
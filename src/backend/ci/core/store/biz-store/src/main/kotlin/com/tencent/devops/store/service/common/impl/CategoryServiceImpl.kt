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
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.CategoryRequest
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ICON_URL
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_ID
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_TYPE
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.BusinessEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.CategoryService
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 范畴业务逻辑类
 *
 * since: 2019-03-22
 */
@Service
class CategoryServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val categoryDao: CategoryDao,
    private val businessConfigDao: BusinessConfigDao
) : CategoryService {

    private val logger = LoggerFactory.getLogger(CategoryServiceImpl::class.java)

    /**
     * 获取所有范畴信息
     * @param type
     */
    override fun getAllCategory(type: Byte): Result<List<Category>?> {
        val atomCategoryList = categoryDao.getAllCategory(dslContext, type)?.map {
            val category = categoryDao.convert(it)
            val businessConfig = businessConfigDao.listFeatureConfig(
                dslContext = dslContext,
                business = BusinessEnum.CATEGORY.name,
                businessValue = category.categoryCode
            )
            businessConfig?.forEach { config ->
                category.settings[config.feature] = config.configValue ?: ""
            }
            category
        }
        return Result(atomCategoryList)
    }

    /**
     * 根据id获取范畴信息
     */
    override fun getCategory(id: String): Result<Category?> {
        val categoryRecord = categoryDao.getCategory(dslContext, id)
        return Result(
            if (categoryRecord == null) {
                null
            } else {
                categoryDao.convert(categoryRecord)
            }
        )
    }

    /**
     * 保存范畴信息
     */
    override fun saveCategory(categoryRequest: CategoryRequest, type: Byte): Result<Boolean> {
        logger.info("saveCategory params[$categoryRequest|$type]")
        val categoryCode = categoryRequest.categoryCode
        // 判断范畴代码是否存在
        val codeCount = categoryDao.countByCode(dslContext, categoryCode, type)
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(categoryCode),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        val categoryName = categoryRequest.categoryName
        // 判断范畴名称是否存在
        val nameCount = categoryDao.countByName(dslContext, categoryName, type)
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(categoryName),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        val id = UUIDUtil.generate()
        categoryDao.add(dslContext, id, categoryRequest, type)
        return Result(true)
    }

    /**
     * 更新范畴信息
     */
    override fun updateCategory(id: String, categoryRequest: CategoryRequest, type: Byte): Result<Boolean> {
        logger.info("updateCategory params[$id|$categoryRequest|$type]")
        val categoryCode = categoryRequest.categoryCode
        // 判断范畴是否存在
        val codeCount = categoryDao.countByCode(dslContext, categoryCode, type)
        if (codeCount > 0) {
            // 判断更新范畴名称是否属于自已
            val category = categoryDao.getCategory(dslContext, id)
            if (null != category && categoryCode != category.categoryCode) {
                // 抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(categoryCode),
                    data = false
                )
            }
        }
        val categoryName = categoryRequest.categoryName
        // 判断类型范畴是否存在
        val count = categoryDao.countByName(dslContext, categoryName, type)
        if (count > 0) {
            // 判断更新的范畴名称是否属于自已
            val category = categoryDao.getCategory(dslContext, id)
            if (null != category && categoryName != category.categoryName) {
                // 抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(categoryName),
                    data = false
                )
            }
        }
        categoryDao.update(dslContext, id, categoryRequest)
        return Result(true)
    }

    /**
     * 根据id删除范畴信息
     */
    override fun deleteCategory(id: String): Result<Boolean> {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            categoryDao.delete(context, id)
        }
        return Result(true)
    }

    /**
     * 为范畴集合添加范畴
     */
    override fun addCategoryToCategoryList(it: Record, categoryList: MutableList<Category>) {
        val categoryCode = it[KEY_CATEGORY_CODE] as String
        val categoryName = it[KEY_CATEGORY_NAME] as String
        val type = (it[KEY_CATEGORY_TYPE] as Byte).toInt()
        val categoryLanName = I18nUtil.getCodeLanMessage(
            messageCode = "${StoreTypeEnum.getStoreType(type)}.category.$categoryCode",
            defaultMessage = categoryName
        )
        categoryList.add(
            Category(
                id = it[KEY_CATEGORY_ID] as String,
                categoryCode = categoryCode,
                categoryName = categoryLanName,
                iconUrl = it[KEY_CATEGORY_ICON_URL] as? String,
                categoryType = StoreTypeEnum.getStoreType((it[KEY_CATEGORY_TYPE] as Byte).toInt()),
                createTime = (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                updateTime = (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli()
            )
        )
    }
}

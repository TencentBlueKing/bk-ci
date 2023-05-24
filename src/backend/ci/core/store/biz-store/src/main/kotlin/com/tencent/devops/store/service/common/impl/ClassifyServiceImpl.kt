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
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.AbstractClassifyService
import com.tencent.devops.store.service.common.ClassifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 分类业务逻辑类
 *
 * since: 2018-12-20
 */
@Suppress("ALL")
@Service
class ClassifyServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val classifyDao: ClassifyDao
) : ClassifyService {

    private val logger = LoggerFactory.getLogger(ClassifyServiceImpl::class.java)

    /**
     * 获取所有分类信息
     * @param type
     */
    override fun getAllClassify(type: Byte): Result<List<Classify>> {
        val classifyList = classifyDao.getAllClassify(dslContext, type).map { classifyDao.convert(it) }
        return Result(classifyList)
    }

    /**
     * 根据id获取分类信息
     */
    override fun getClassify(id: String): Result<Classify?> {
        val classifyRecord = classifyDao.getClassify(dslContext, id)
        return Result(
            if (classifyRecord == null) {
                null
            } else {
                classifyDao.convert(classifyRecord)
            }
        )
    }

    /**
     * 保存分类信息
     */
    override fun saveClassify(classifyRequest: ClassifyRequest, type: Byte): Result<Boolean> {
        logger.info("saveClassify params:[$classifyRequest|$type]")
        val classifyCode = classifyRequest.classifyCode
        // 判断分类代码是否存在
        val codeCount = classifyDao.countByCode(dslContext, classifyCode, type)
        if (codeCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(classifyCode),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        val classifyName = classifyRequest.classifyName
        // 判断分类名称是否存在
        val nameCount = classifyDao.countByName(dslContext, classifyName, type)
        if (nameCount > 0) {
            // 抛出错误提示
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(classifyName),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        val id = UUIDUtil.generate()
        classifyDao.add(dslContext, id, classifyRequest, type)
        return Result(true)
    }

    /**
     * 更新分类信息
     */
    override fun updateClassify(id: String, classifyRequest: ClassifyRequest, type: Byte): Result<Boolean> {
        logger.info("updateClassify params:[$id|$classifyRequest|$type]")
        val classifyCode = classifyRequest.classifyCode
        // 判断分类是否存在
        val codeCount = classifyDao.countByCode(dslContext, classifyCode, type)
        if (codeCount > 0) {
            // 判断更新分类名称是否属于自已
            val classify = classifyDao.getClassify(dslContext, id)
            if (null != classify && classifyCode != classify.classifyCode) {
                // 抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(classifyCode),
                    data = false
                )
            }
        }
        val classifyName = classifyRequest.classifyName
        // 判断类型分类是否存在
        val count = classifyDao.countByName(dslContext, classifyName, type)
        if (count > 0) {
            // 判断更新的分类名称是否属于自已
            val classify = classifyDao.getClassify(dslContext, id)
            if (null != classify && classifyName != classify.classifyName) {
                // 抛出错误提示
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(classifyName),
                    data = false
                )
            }
        }
        classifyDao.update(dslContext, id, classifyRequest)
        return Result(true)
    }

    /**
     * 根据id删除分类信息
     */
    override fun deleteClassify(id: String): Result<Boolean> {
        val classifyRecord = classifyDao.getClassify(dslContext, id)
        var flag = false
        if (null != classifyRecord) {
            val classifyType = classifyRecord.type
            val classifyService = getStoreClassifyService(StoreTypeEnum.getStoreType(classifyType.toInt()))
            flag = classifyService.getDeleteClassifyFlag(id)
        }
        if (flag) {
            classifyDao.delete(dslContext, id)
        } else {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CLASSIFY_IS_NOT_ALLOW_DELETE,
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        return Result(true)
    }

    private fun getStoreClassifyService(storeType: String): AbstractClassifyService {
        return SpringContextUtil.getBean(AbstractClassifyService::class.java, "${storeType}_CLASSIFY_SERVICE")
    }
}

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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.LabelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.LabelRequest
import com.tencent.devops.store.service.common.LabelService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 标签业务逻辑类
 *
 * since: 2019-03-22
 */
@Service
class LabelServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val labelDao: LabelDao
) : LabelService {

    private val logger = LoggerFactory.getLogger(LabelServiceImpl::class.java)

    /**
     * 获取所有标签信息
     * @param type 0:插件 1：模板
     */
    override fun getAllLabel(type: Byte): Result<List<Label>?> {
        val atomLabelList = labelDao.getAllLabel(dslContext, type)?.map { labelDao.convert(it) }
        return Result(atomLabelList)
    }

    /**
     * 根据id获取标签信息
     */
    override fun getLabel(id: String): Result<Label?> {
        val labelRecord = labelDao.getLabel(dslContext, id)
        logger.info("the pipelineContainerRecord is :{}", labelRecord)
        return Result(
            if (labelRecord == null) {
                null
            } else {
                labelDao.convert(labelRecord)
            }
        )
    }

    /**
     * 保存标签信息
     */
    override fun saveLabel(labelRequest: LabelRequest, type: Byte): Result<Boolean> {
        logger.info("the save LabelRequest is:$labelRequest,type is:$type")
        val labelCode = labelRequest.labelCode
        // 判断标签代码是否存在
        val codeCount = labelDao.countByCode(dslContext, labelCode, type)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(labelCode),
                false
            )
        }
        val labelName = labelRequest.labelName
        // 判断标签名称是否存在
        val nameCount = labelDao.countByName(dslContext, labelName, type)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_EXIST,
                arrayOf(labelName),
                false
            )
        }
        val id = UUIDUtil.generate()
        labelDao.add(dslContext, id, labelRequest, type)
        return Result(true)
    }

    /**
     * 更新标签信息
     */
    override fun updateLabel(id: String, labelRequest: LabelRequest, type: Byte): Result<Boolean> {
        logger.info("the update id is :$id,the update labelRequest is:$labelRequest,type is:$type")
        val labelCode = labelRequest.labelCode
        // 判断标签是否存在
        val codeCount = labelDao.countByCode(dslContext, labelCode, type)
        if (codeCount > 0) {
            // 判断更新标签名称是否属于自已
            val label = labelDao.getLabel(dslContext, id)
            if (null != label && labelCode != label.labelCode) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(labelCode),
                    false
                )
            }
        }
        val labelName = labelRequest.labelName
        // 判断类型标签是否存在
        val count = labelDao.countByName(dslContext, labelName, type)
        if (count > 0) {
            // 判断更新的标签名称是否属于自已
            val label = labelDao.getLabel(dslContext, id)
            if (null != label && labelName != label.labelName) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_EXIST,
                    arrayOf(labelName),
                    false
                )
            }
        }
        labelDao.update(dslContext, id, labelRequest)
        return Result(true)
    }

    /**
     * 根据id删除标签信息
     */
    override fun deleteLabel(id: String): Result<Boolean> {
        logger.info("the delete id is :$id")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            labelDao.delete(context, id)
        }
        return Result(true)
    }
}

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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ReasonDao
import com.tencent.devops.store.dao.common.ReasonRelDao
import com.tencent.devops.store.pojo.common.Reason
import com.tencent.devops.store.pojo.common.ReasonReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.service.common.ReasonService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 原因定义类
 * since: 2019-03-22
 */
@Service
class ReasonServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val reasonDao: ReasonDao,
    private val reasonRelDao: ReasonRelDao
) : ReasonService {

    private val logger = LoggerFactory.getLogger(ReasonServiceImpl::class.java)

    override fun list(
        type: ReasonTypeEnum,
        enable: Boolean?
    ): Result<List<Reason>> {
        val reasons = reasonDao.list(dslContext, type.type, enable).map { reasonDao.convert(it) }
        return Result(reasons)
    }

    override fun add(
        userId: String,
        type: ReasonTypeEnum,
        reasonReq: ReasonReq
    ): Result<Boolean> {
        logger.info("add reason, $userId | $reasonReq")
        val id = UUIDUtil.generate()
        reasonDao.add(dslContext, id, userId, type.type, reasonReq)
        return Result(true)
    }

    override fun update(
        userId: String,
        id: String,
        reasonReq: ReasonReq
    ): Result<Boolean> {
        logger.info("update reason, $userId | $reasonReq | $id")
        reasonDao.update(dslContext, id, userId, reasonReq)
        return Result(true)
    }

    override fun enable(
        userId: String,
        id: String,
        enable: Boolean
    ): Result<Boolean> {
        logger.info("enable reason, $userId | $enable | $id")
        reasonDao.enable(dslContext, id, userId, enable)
        return Result(true)
    }

    override fun delete(
        userId: String,
        id: String
    ): Result<Boolean> {
        logger.info("delete reason, $userId | $id")
        val reason = reasonDao.get(dslContext, id)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(id),
                language = I18nUtil.getLanguage(userId)
            )
        // 若已被使用，不允许删除
        val reasonContent = reason.content
        if (reasonRelDao.isUsed(dslContext, id)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_UNINSTALL_REASON_USED,
                params = arrayOf(reasonContent),
                language = I18nUtil.getLanguage(userId)
            )
        }
        reasonDao.delete(dslContext, id)
        return Result(true)
    }
}

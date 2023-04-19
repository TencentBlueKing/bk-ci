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

package com.tencent.devops.support.resources.op

import com.tencent.devops.common.api.pojo.MessageCodeDetail
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.op.OpMessageCodeResource
import com.tencent.devops.support.model.code.AddMessageCodeRequest
import com.tencent.devops.support.model.code.MessageCodeResp
import com.tencent.devops.support.model.code.UpdateMessageCodeRequest
import com.tencent.devops.support.services.MessageCodeDetailService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpMessageCodeResourceImpl @Autowired constructor(private val messageCodeDetailService: MessageCodeDetailService) :
        OpMessageCodeResource {

    override fun addMessageCodeDetail(addMessageCodeRequest: AddMessageCodeRequest): Result<Boolean> {
        return messageCodeDetailService.addMessageCodeDetail(addMessageCodeRequest)
    }

    override fun refreshMessageCodeCache(messageCode: String): Result<Boolean> {
        return messageCodeDetailService.refreshMessageCodeCache(messageCode)
    }

    override fun getMessageCodeDetails(
        messageCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MessageCodeResp> {
        return messageCodeDetailService.getMessageCodeDetails(messageCode, page, pageSize)
    }

    override fun updateMessageCodeDetail(messageCode: String, updateMessageCodeRequest: UpdateMessageCodeRequest): Result<Boolean> {
        return messageCodeDetailService.updateMessageCodeDetail(messageCode, updateMessageCodeRequest)
    }

    override fun getMessageCodeDetail(messageCode: String): Result<MessageCodeDetail?> {
        return messageCodeDetailService.getMessageCodeDetail(messageCode)
    }
}

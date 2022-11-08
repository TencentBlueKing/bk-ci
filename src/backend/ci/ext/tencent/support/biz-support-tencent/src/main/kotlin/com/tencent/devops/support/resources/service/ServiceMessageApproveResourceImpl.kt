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

package com.tencent.devops.support.resources.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.service.ServiceMessageApproveResource
import com.tencent.devops.support.model.approval.CompleteMoaWorkItemRequest
import com.tencent.devops.support.model.approval.CreateMoaApproveRequest
import com.tencent.devops.support.model.approval.MoaWorkItemElement
import com.tencent.devops.support.services.MessageApproveService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMessageApproveResourceImpl @Autowired constructor(
    private val messageApproveService: MessageApproveService
) :
    ServiceMessageApproveResource {

    override fun moaComplete(taskId: String): Result<Boolean> {
        return messageApproveService.moaComplete(taskId)
    }

    override fun createMoaMessageApproval(
        userId: String,
        createMoaApproveRequest: CreateMoaApproveRequest
    ): Result<Boolean> {
        return messageApproveService.createMoaMessageApproval(userId, createMoaApproveRequest)
    }

    override fun createMoaWorkItemMessageApproval(
        moaWorkItemElementList: List<MoaWorkItemElement>
    ): Result<Boolean> {
        return messageApproveService.createMoaWorkItem(moaWorkItemElementList)
    }

    override fun createMoaWorkItemMessageComplete(
        completeMoaWorkItemRequest: CompleteMoaWorkItemRequest
    ): Result<Boolean> {
        return messageApproveService.completeMoaWorkItem(completeMoaWorkItemRequest)
    }
}

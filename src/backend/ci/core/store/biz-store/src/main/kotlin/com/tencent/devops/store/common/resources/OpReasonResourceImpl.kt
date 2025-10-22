/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.OpReasonResource
import com.tencent.devops.store.pojo.common.reason.Reason
import com.tencent.devops.store.pojo.common.reason.ReasonReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import com.tencent.devops.store.common.service.ReasonService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpReasonResourceImpl @Autowired constructor(
    private val reasonService: ReasonService
) : OpReasonResource {

    override fun add(userId: String, type: ReasonTypeEnum, reasonReq: ReasonReq): Result<Boolean> {
        return reasonService.add(userId, type, reasonReq)
    }

    override fun update(userId: String, id: String, type: ReasonTypeEnum, reasonReq: ReasonReq): Result<Boolean> {
        return reasonService.update(userId, id, reasonReq)
    }

    override fun enableReason(userId: String, id: String, type: ReasonTypeEnum, enable: Boolean): Result<Boolean> {
        return reasonService.enable(userId, id, enable)
    }

    override fun list(type: ReasonTypeEnum, enable: Boolean?): Result<List<Reason>> {
        return reasonService.list(type, enable)
    }

    override fun delete(userId: String, id: String): Result<Boolean> {
        return reasonService.delete(userId, id)
    }
}

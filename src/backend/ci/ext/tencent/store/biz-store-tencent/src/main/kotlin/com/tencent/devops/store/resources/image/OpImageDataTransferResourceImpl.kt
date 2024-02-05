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

package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.OpImageDataTransferResource
import com.tencent.devops.store.service.image.OpImageDataTransferService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpImageDataTransferResourceImpl @Autowired constructor(
    private val opImageDataTransferService: OpImageDataTransferService
) : OpImageDataTransferResource {
    override fun batchRecheckByProject(userId: String, projectCode: String): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.batchRecheckByProject(
                userId = userId,
                projectCode = projectCode,
                interfaceName = "/op/datatransfer/image/batchRecheckByProject,put"
            )
        )
    }

    override fun batchRecheckAll(userId: String): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.batchRecheckAll(
                userId = userId,
                interfaceName = "/op/datatransfer/image/batchRecheckAll,put"
            )
        )
    }

    override fun initClassifyAndCategory(
        userId: String,
        classifyCode: String?,
        classifyName: String?,
        categoryCode: String?,
        categoryName: String?
    ): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.initClassifyAndCategory(
                userId = userId,
                classifyCode = classifyCode,
                classifyName = classifyName,
                categoryCode = categoryCode,
                categoryName = categoryName,
                interfaceName = "/op/datatransfer/image/initClassifyAndCategory,put"
            )
        )
    }

    override fun transferImage(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        categoryCode: String?
    ): Result<Int> {
        return Result(
            0,
            "ok",
            opImageDataTransferService.transferImage(
                userId = userId,
                projectCode = projectCode,
                classifyCode = classifyCode,
                categoryCode = categoryCode,
                interfaceName = "/op/datatransfer/image/transferImage,put"
            )
        )
    }
}

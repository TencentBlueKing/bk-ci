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

package com.tencent.devops.store.atom.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomArchiveResource
import com.tencent.devops.store.pojo.atom.AtomPkgInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.atom.service.MarketAtomArchiveService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMarketAtomArchiveResourceImpl @Autowired constructor(
    private val marketAtomArchiveService: MarketAtomArchiveService
) : ServiceMarketAtomArchiveResource {

    override fun verifyAtomPackageByUserId(
        userId: String,
        atomCode: String,
        version: String,
        projectCode: String,
        releaseType: ReleaseTypeEnum?,
        os: String?
    ): Result<Boolean> {
        return marketAtomArchiveService.verifyAtomPackageByUserId(
            userId = userId,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            releaseType = releaseType,
            os = os
        )
    }

    override fun verifyAtomTaskJson(
        userId: String,
        atomCode: String,
        version: String,
        projectCode: String
    ): Result<GetAtomConfigResult?> {
        return marketAtomArchiveService.verifyAtomTaskJson(
            userId = userId,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version
        )
    }

    override fun validateReleaseType(
        userId: String,
        atomCode: String,
        version: String,
        projectCode: String,
        fieldCheckConfirmFlag: Boolean?
    ): Result<Boolean> {
        return marketAtomArchiveService.validateReleaseType(
            userId = userId,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            fieldCheckConfirmFlag = fieldCheckConfirmFlag
        )
    }

    override fun updateAtomPkgInfo(
        userId: String,
        atomId: String,
        projectCode: String,
        atomPkgInfoUpdateRequest: AtomPkgInfoUpdateRequest
    ): Result<Boolean> {
        return marketAtomArchiveService.updateAtomPkgInfo(userId, atomId, projectCode, atomPkgInfoUpdateRequest)
    }
}

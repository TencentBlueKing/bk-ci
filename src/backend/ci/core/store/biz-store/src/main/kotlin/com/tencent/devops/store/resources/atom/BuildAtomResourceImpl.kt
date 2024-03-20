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
package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.store.api.atom.BuildAtomResource
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.service.atom.AtomReleaseService
import com.tencent.devops.store.service.atom.AtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService,
    private val atomReleaseService: AtomReleaseService
) : BuildAtomResource {

    override fun getAtomDefaultValidVersion(projectCode: String, atomCode: String): Result<VersionInfo?> {
        return atomService.getAtomDefaultValidVersion(projectCode, atomCode)
    }

    @SensitiveApiPermission("branch_test_version_management")
    override fun createAtomBranchTestVersion(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String> {
        return atomReleaseService.creatAtomBranchTestVersion(userId, marketAtomUpdateRequest)
    }

    @SensitiveApiPermission("branch_test_version_management")
    override fun endBranchVersionTest(userId: String, atomCode: String, branch: String): Result<Boolean> {
        return atomReleaseService.endBranchVersionTest(userId, atomCode, branch)
    }

    override fun getProcessInfo(userId: String, atomId: String): Result<StoreProcessInfo> {
        return atomReleaseService.getProcessInfo(userId, atomId)
    }
}
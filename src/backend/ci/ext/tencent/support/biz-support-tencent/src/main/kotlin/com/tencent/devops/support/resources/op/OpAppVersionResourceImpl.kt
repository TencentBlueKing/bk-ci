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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.op.OpAppVersionResource
import com.tencent.devops.support.model.app.AppVersionRequest
import com.tencent.devops.support.model.app.pojo.AppVersion
import com.tencent.devops.support.services.AppVersionService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAppVersionResourceImpl @Autowired constructor(private val appVersionService: AppVersionService) : OpAppVersionResource {
    override fun updateAppVersion(appVersionId: Long, appVersionRequest: AppVersionRequest): Result<Int> {
        return Result(data = appVersionService.setAppVersion(appVersionId, appVersionRequest))
    }

    override fun addAppVersion(appVersionRequest: AppVersionRequest): Result<Int> {
        return Result(data = appVersionService.setAppVersion(null, appVersionRequest))
    }

    override fun getAppVersion(appVersionId: Long): Result<AppVersion?> {
        return Result(data = appVersionService.getAppVersion(appVersionId))
    }

    override fun getAllAppVersion(): Result<List<AppVersion>> {
        return Result(data = appVersionService.getAllAppVersion())
    }

    override fun deleteAppVersion(appVersionId: Long): Result<Int> {
        return Result(data = appVersionService.deleteAppVersion(appVersionId))
    }

    override fun addAppVersions(appVersionRequests: List<AppVersionRequest>): Result<Int> {
        var result = 1
        appVersionRequests.forEach {
            var insertResult = appVersionService.setAppVersion(null, it)
            if (insertResult != 1) {
                result = insertResult
            }
        }
        return Result(data = result)
    }
}

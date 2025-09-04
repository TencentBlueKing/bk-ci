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

package com.tencent.devops.stream.v1.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.stream.api.service.v1.GitCIStarterWebResource
import com.tencent.devops.stream.v1.pojo.V1GitStarterWebList
import com.tencent.devops.stream.v1.pojo.V1GitYamlContent
import com.tencent.devops.stream.v1.pojo.V1GitYamlProperty
import com.tencent.devops.stream.v1.service.V1GitCIStarterWebService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class GitCIStarterWebResourceImpl @Autowired constructor(
    private val gitCIStarterWebService: V1GitCIStarterWebService
) : GitCIStarterWebResource {
    override fun getYamlList(userId: String): Result<List<V1GitYamlContent>> {
        checkParam(userId)
        return Result(gitCIStarterWebService.getYamlList())
    }

    override fun getPropertyList(userId: String, category: String?): Result<List<V1GitYamlProperty>> {
        checkParam(userId)
        return Result(gitCIStarterWebService.getPropertyList(category))
    }

    override fun getWebList(userId: String): Result<V1GitStarterWebList> {
        checkParam(userId)
        return Result(gitCIStarterWebService.getStarterWebList())
    }

    override fun update(userId: String, properties: List<V1GitYamlContent>): Result<Int> {
        checkParam(userId)
        return Result(gitCIStarterWebService.updateStarterYamls(properties))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}

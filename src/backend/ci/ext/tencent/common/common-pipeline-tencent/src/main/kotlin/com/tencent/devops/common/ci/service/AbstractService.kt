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

package com.tencent.devops.common.ci.service

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.ci.SERVICE_TYPE
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import javax.ws.rs.core.Response

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = SERVICE_TYPE)
@JsonSubTypes(
    JsonSubTypes.Type(value = MysqlService::class, name = MysqlService.type)
)

abstract class AbstractService(
    open val image: String,
    open val user: String?,
    open val password: String?,
    open val paramNamespace: String?
) {
    abstract fun getType(): String

    abstract fun getServiceParamNameSpace(): String

    abstract fun getServiceInput(repoUrl: String, repoUsername: String, repoPwd: String, env: String): ServiceJobDevCloudInput

    fun parseImage(): Pair<String, String> {
        val list = image.split(":")
        if (list.size != 2) {
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "GITCI Service镜像格式非法")
        }
        return Pair(list[0], list[1])
    }
}

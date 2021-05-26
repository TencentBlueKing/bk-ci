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

package com.tencent.devops.common.ci.v2.service

import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.common.ci.v2.ServiceWith

data class V2MysqlService(
    override val serviceId: String,
    override val image: String,
    override val with: ServiceWith
) : V2AbstractService(serviceId, image, with) {

    override fun getType(): String {
        return type
    }

    override fun getServiceInput(
        repoUrl: String,
        repoUsername: String,
        repoPwd: String,
        env: String
    ): ServiceJobDevCloudInput {
        val params = if (with.password.isNullOrBlank()) {
            "{\"env\":{\"MYSQL_ALLOW_EMPTY_PASSWORD\":\"yes\"}}"
        } else {
            "{\"env\":{\"MYSQL_ROOT_PASSWORD\":\"${with.password}\"}}"
        }

        return ServiceJobDevCloudInput(
            image,
            repoUrl,
            repoUsername,
            repoPwd,
            params,
            env
        )
    }

    override fun getServiceParamNameSpace(): String {
        return serviceId
    }

    companion object {
        const val type = "mysql"
    }
}

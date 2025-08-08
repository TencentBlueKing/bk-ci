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

package com.tencent.devops.repository.pojo.oauth

import com.tencent.devops.common.api.enums.ScmType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户代码库Oauth授权信息")
data class OauthTokenVo(
    @get:Schema(title = "授权账号")
    val username: String,
    @get:Schema(title = "授权代码库数量")
    val repoCount: Long,
    @get:Schema(title = "创建时间")
    val createTime: Long? = null,
    @get:Schema(title = "授权类型")
    val scmCode: String,
    @get:Schema(title = "是否过期")
    val expired: Boolean = false,
    @get:Schema(title = "是否已授权")
    val authorized: Boolean = true,
    @get:Schema(title = "操作人")
    val operator: String,
    @get:Schema(title = "代码库类型")
    val scmType: ScmType,
    @get:Schema(title = "代码库名称")
    val name: String
)

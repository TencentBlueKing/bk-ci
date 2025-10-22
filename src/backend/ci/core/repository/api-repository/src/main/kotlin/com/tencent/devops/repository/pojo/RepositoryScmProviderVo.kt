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

package com.tencent.devops.repository.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.scm.api.enums.ScmProviderType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "源代码提供者展示")
data class RepositoryScmProviderVo(
    @get:Schema(title = "标识", required = true)
    val providerCode: String,
    @get:Schema(title = "提供者类型", required = true)
    val providerType: ScmProviderType,
    @get:Schema(title = "名称", required = true)
    val name: String,
    @get:Schema(title = "描述", required = true)
    val desc: String?,
    @get:Schema(title = "代码库类型", required = true)
    val scmType: ScmType,
    @get:Schema(title = "logo链接", required = true)
    val logoUrl: String?,
    @get:Schema(title = "文档链接", required = true)
    val docUrl: String?,
    @get:Schema(title = "支持的授权类型", required = true)
    val credentialTypeList: List<RepoCredentialTypeVo>,
    @get:Schema(title = "是否支持api", required = true)
    val api: Boolean,
    @get:Schema(title = "是否支持合并请求", required = true)
    val merge: Boolean,
    @get:Schema(title = "是否支持webhook", required = true)
    val webhook: Boolean,
    @get:Schema(title = "webhook鉴权类型", required = true)
    val webhookSecretType: String?,
    @get:Schema(title = "是否支持pac", required = true)
    val pac: Boolean
)

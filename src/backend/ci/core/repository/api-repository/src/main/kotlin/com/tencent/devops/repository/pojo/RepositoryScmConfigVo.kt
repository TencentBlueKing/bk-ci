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
import com.tencent.devops.repository.pojo.enums.ScmConfigOauthType
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "源代码管理配置")
data class RepositoryScmConfigVo(
    @get:Schema(title = "代码库标识", required = true)
    val scmCode: String,
    @get:Schema(title = "代码库名称", required = true)
    val name: String,
    @get:Schema(title = "源代码提供者,如github,gitlab", required = true)
    val providerCode: String,
    @get:Schema(title = "代码库类型,如git,svn", required = true)
    val scmType: ScmType,
    @get:Schema(title = "代码库域名,多个用;分割", required = true)
    val hosts: String?,
    @get:Schema(title = "logo链接", required = true)
    val logoUrl: String?,
    @get:Schema(title = "文档链接", required = true)
    val docUrl: String?,
    @get:Schema(title = "支持的授权类型", required = true)
    val credentialTypeList: List<RepoCredentialTypeVo>,
    @get:Schema(title = "授权类型支持的凭证类型", required = true)
    val oauthType: ScmConfigOauthType,
    @get:Schema(title = "oauth关联的代码库标识, oauthType为reuse时使用", required = true)
    val oauthScmCode: String? = null,
    @get:Schema(title = "状态", required = true)
    val status: ScmConfigStatus,
    @get:Schema(title = "支持merge", required = true)
    val mergeEnabled: Boolean,
    @get:Schema(title = "支持pac", required = true)
    val pacEnabled: Boolean,
    @get:Schema(title = "支持webhook", required = true)
    val webhookEnabled: Boolean,
    @get:Schema(title = "提供者属性配置", required = true)
    val props: ScmConfigProps,
    @get:Schema(title = "是否能够删除", required = true)
    val canDelete: Boolean = false,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "更新人", required = true)
    val updater: String,
    @get:Schema(title = "创建时间", required = true)
    val createTime: Long?,
    @get:Schema(title = "更新时间", required = true)
    val updateTime: Long?
)

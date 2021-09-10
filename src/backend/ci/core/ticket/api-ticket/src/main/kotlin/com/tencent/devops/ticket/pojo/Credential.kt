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

package com.tencent.devops.ticket.pojo

import com.tencent.devops.ticket.pojo.enums.CredentialType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("凭据-凭据内容")
data class Credential(
    @ApiModelProperty("凭据ID", required = true)
    val credentialId: String,
    @ApiModelProperty("凭据名称", required = true)
    val credentialName: String? = null,
    @ApiModelProperty("凭据类型", required = true)
    val credentialType: CredentialType,
    @ApiModelProperty("凭据描述", required = false)
    val credentialRemark: String? = null,
    @ApiModelProperty("最后更新时间", required = true)
    val updatedTime: Long,
    @ApiModelProperty("凭据内容", required = true)
    val v1: String,
    @ApiModelProperty("凭据内容", required = true)
    val v2: String? = null,
    @ApiModelProperty("凭据内容", required = true)
    val v3: String? = null,
    @ApiModelProperty("凭据内容", required = true)
    val v4: String? = null,
    @ApiModelProperty("最后更新者", required = true)
    val updateUser: String? = null,
    @ApiModelProperty("创建人", required = true)
    val createUser: String? = null
)
